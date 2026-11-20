package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.ReplayRequest;
import dev.agentflow.inbox.server.metrics.DepthSampler;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The meters of section "Metrics" in the design: every one carries tenant and lane so that a problem
 * can be traced to who and where. Meters are cumulative for the JVM, so the test compares before and after.
 */
class MetricsIT extends IntegrationTestBase {

    @Autowired
    MeterRegistry registry;

    @Autowired
    DepthSampler depthSampler;

    @Autowired
    LeaseSweeper leaseSweeper;

    private Search globexMeter(String name) {
        return registry.find(name).tag("tenant", GLOBEX);
    }

    private long writes(String result) {
        var timer = globexMeter("inbox.write").tag("result", result).tag("lane", "interactive").timer();
        return timer == null ? 0 : timer.count();
    }

    private double counter(String name, String tagKey, String tagValue) {
        var counter = globexMeter(name).tag(tagKey, tagValue).counter();
        return counter == null ? 0 : counter.count();
    }

    private double gauge(String name, String lane, String stat) {
        var gauge = globexMeter(name).tag("lane", lane).tag("stat", stat).gauge();
        return gauge == null ? -1 : gauge.value();
    }

    @Test
    void writesClaimsReclaimsAndReplaysAreMeasured() throws Exception {
        long accepted = writes("accepted");
        long deduped = writes("deduped");
        long conflicts = writes("conflict");
        var latencyBefore = globexMeter("inbox.claim.latency").tag("lane", "interactive").timer();
        long claimsBefore = latencyBefore == null ? 0 : latencyBefore.count();
        double reclaimsBefore = counter("inbox.lease.reclaim", "lane", "interactive");
        double forcedBefore = counter("inbox.replay", "force", "true");

        // two messages of one conversation, one duplicate, one reused key
        globex.writeEvent("u1", message("m1", "c1", "hello").build()).require();
        globex.writeEvent("u1", message("m2", "c1", "there").build()).require();
        globex.writeEvent("u1", message("m2", "c1", "there").build()).require();
        assertThat(globex.writeEvent("u1", message("m2", "c1", "something else").build()).status()).isEqualTo(409);
        assertThat(writes("accepted") - accepted).isEqualTo(2);
        assertThat(writes("deduped") - deduped).isEqualTo(1);
        assertThat(writes("conflict") - conflicts).isEqualTo(1);

        // the batch became eligible when its 1.5 s window closed; it is claimed 2.5 s after that
        clock.advance(Duration.ofSeconds(4));
        ClaimedRun run = runtime.claim(new dev.agentflow.inbox.api.ClaimRequest("worker-1", GLOBEX, "u1", null, null, null)).require();
        var latency = globexMeter("inbox.claim.latency").tag("lane", "interactive").timer();
        assertThat(latency.count() - claimsBefore).isEqualTo(1);
        assertThat(latency.max(TimeUnit.MILLISECONDS)).as("measured from the end of the quiet window").isEqualTo(2500.0);
        assertThat(globexMeter("inbox.ready.to.claim").tag("lane", "interactive").timer().max(TimeUnit.MILLISECONDS))
                .as("measured from the first event").isEqualTo(4000.0);
        assertThat(globexMeter("inbox.batch.size").tag("lane", "interactive").summary().max()).isEqualTo(2.0);

        // the worker dies: one lease reclaim
        clock.advance(Duration.ofSeconds(61));
        assertThat(leaseSweeper.runOnce()).isEqualTo(1);
        assertThat(counter("inbox.lease.reclaim", "lane", "interactive") - reclaimsBefore).isEqualTo(1.0);

        // finish it, then force a replay of both events
        ClaimedRun again = runtime.claim(new dev.agentflow.inbox.api.ClaimRequest("worker-2", GLOBEX, "u1", null, null, null)).require();
        runtime.ack(again.runId(), AckRequest.done(again.epoch(), again.lastRunSeq())).require();
        ops.replay(ReplayRequest.inbox(GLOBEX, "u1", clock.now().minus(Duration.ofHours(1)), clock.now().plusSeconds(1),
                true, "metrics test", "alice")).require();
        assertThat(counter("inbox.replay", "force", "true") - forcedBefore)
                .as("a forced replay is counted separately: it bypassed the idempotency check").isEqualTo(2.0);
        assertThat(run.runId()).isEqualTo(again.runId());

        // everything is visible to a scraper
        String scrape = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(baseUri() + "/actuator/prometheus")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        assertThat(scrape).contains("inbox_write_seconds_count", "inbox_claim_latency_seconds", "inbox_ready_to_claim_seconds",
                "inbox_batch_size", "inbox_lease_reclaim_total", "inbox_replay_total");
        assertThat(scrape).contains("tenant=\"globex\"").contains("lane=\"interactive\"").contains("force=\"true\"");
    }

    @Test
    void backlogIsReportedAsMedianP99AndMaxAcrossInboxes_notAsAMean() {
        // one inbox is drowning, nine are fine
        for (int i = 0; i < 12; i++) {
            globex.writeEvent("hot", webhook("h" + i, "{}").build()).require();
        }
        for (int user = 0; user < 9; user++) {
            globex.writeEvent("calm-" + user, webhook("c", "{}").build()).require();
        }
        String poison = globex.writeEvent("calm-0", webhook("poison", "{}")
                .lane(dev.agentflow.inbox.api.Lane.BATCH).build()).require().eventId();
        ops.deadLetter(poison, new dev.agentflow.inbox.api.DeadLetterRequest(GLOBEX, "bad", "bob")).require();

        depthSampler.sample();
        assertThat(gauge("inbox.pending.depth", "interactive", "max")).isEqualTo(12.0);
        assertThat(gauge("inbox.pending.depth", "interactive", "p50")).isEqualTo(1.0);
        assertThat(gauge("inbox.pending.depth", "interactive", "p99")).isEqualTo(12.0);
        assertThat(gauge("inbox.pending.depth", "interactive", "total")).isEqualTo(21.0);
        assertThat(gauge("inbox.dead.letter.depth", "batch", "total")).isEqualTo(1.0);

        // once the backlog is gone the gauges fall back to zero instead of keeping their last value
        jdbc.sql("TRUNCATE TABLE event_transition, admin_audit, timer, event, run, inbox RESTART IDENTITY CASCADE").update();
        depthSampler.sample();
        assertThat(gauge("inbox.pending.depth", "interactive", "max")).isZero();
        assertThat(gauge("inbox.dead.letter.depth", "batch", "total")).isZero();
    }
}
