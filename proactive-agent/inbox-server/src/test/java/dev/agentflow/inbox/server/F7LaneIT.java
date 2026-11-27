package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.api.RunController;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * F7: concurrency and lane isolation. Different inboxes run in parallel. Inside one inbox each lane
 * has its own slots, so background work does not block a reply, while one merge key is never handled
 * by two runs at once.
 */
class F7LaneIT extends IntegrationTestBase {

    private static WriteEventRequest.Builder on(Lane lane, String key) {
        return WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "job.requested.v1")
                .sourceEventId(key).lane(lane).payloadJson("{}");
    }

    @Test
    void oneUsersLongTaskDoesNotDelayAnotherUser() {
        write("alice", on(Lane.INTERACTIVE, "a1"));
        ClaimedRun aliceRun = claim("worker-1", "alice").require(); // still running

        write("bob", on(Lane.INTERACTIVE, "b1"));
        ClaimedRun bobRun = claim("worker-2", "bob").require();
        assertThat(bobRun.inboxId()).isEqualTo("bob");

        runtime.ackDone(bobRun).require();
        runtime.ackDone(aliceRun).require();
    }

    @Test
    void aUsersBackgroundTaskDoesNotBlockTheirOwnNewMessage() {
        write("u42", on(Lane.BACKGROUND, "daily-report"));
        ClaimedRun report = claim("worker-1", "u42").require();
        assertThat(report.lane()).isEqualTo(Lane.BACKGROUND);

        // the report is still being compiled when the user writes
        write("u42", message("msg-1", "u42", "what is on my calendar?"));
        clock.advance(Duration.ofMillis(1500));
        ClaimedRun reply = claim("worker-2", "u42").require();
        assertThat(reply.lane()).isEqualTo(Lane.INTERACTIVE);
        assertThat(count("SELECT count(*) FROM run WHERE status = 'active' AND inbox_id = 'u42'")).isEqualTo(2);

        runtime.ackDone(reply).require();
        runtime.ackDone(report).require();
    }

    @Test
    void eachLaneHasItsOwnCap() {
        // interactive: one at a time
        write("u42", on(Lane.INTERACTIVE, "i1"));
        write("u42", on(Lane.INTERACTIVE, "i2"));
        ClaimedRun i1 = claim("worker-1", "u42").require();
        ApiResult<ClaimedRun> blocked = claim("worker-2", "u42");
        assertThat(blocked.status()).isEqualTo(204);
        assertThat(blocked.header(RunController.HEADER_REASON)).contains("busy");

        // background: two at a time
        write("u42", on(Lane.BACKGROUND, "b1"));
        write("u42", on(Lane.BACKGROUND, "b2"));
        write("u42", on(Lane.BACKGROUND, "b3"));
        ClaimedRun b1 = claim("worker-2", "u42").require();
        ClaimedRun b2 = claim("worker-3", "u42").require();
        assertThat(b1.lane()).isEqualTo(Lane.BACKGROUND);
        assertThat(b2.lane()).isEqualTo(Lane.BACKGROUND);
        assertThat(claim("worker-4", "u42").status()).as("both background slots and the interactive slot are taken")
                .isEqualTo(204);
        assertThat(jdbc.sql("SELECT slot_no FROM run WHERE lane = 'background' AND status = 'active' ORDER BY slot_no")
                .query(Integer.class).list()).containsExactly(1, 2);

        // a slot frees up: the third background job starts, and reuses the freed slot
        runtime.ackDone(b1).require();
        ClaimedRun b3 = claim("worker-4", "u42").require();
        assertThat(b3.events().getFirst().sourceEventId()).isEqualTo("b3");

        runtime.ackDone(i1).require();
        ClaimedRun i2 = claim("worker-1", "u42").require();
        assertThat(i2.events().getFirst().sourceEventId()).isEqualTo("i2");
        runtime.ackDone(i2).require();
        runtime.ackDone(b2).require();
        runtime.ackDone(b3).require();
    }

    @Test
    void theSameMergeKeyNeverRunsTwiceAtOnce_evenWithAFreeSlotInAnotherLane() {
        write("u42", on(Lane.INTERACTIVE, "order-1-v1").mergeKey("order:1"));
        clock.advance(Duration.ofSeconds(2));
        ClaimedRun first = claim("worker-1", "u42").require();

        // the same order shows up again on the background lane, which has free slots
        write("u42", on(Lane.BACKGROUND, "order-1-v2").mergeKey("order:1"));
        clock.advance(Duration.ofSeconds(2));
        ApiResult<ClaimedRun> blocked = claim("worker-2", "u42");
        assertThat(blocked.status()).isEqualTo(204);
        assertThat(blocked.header(RunController.HEADER_REASON)).contains("busy");

        runtime.ackDone(first).require();
        ClaimedRun second = claim("worker-2", "u42").require();
        assertThat(second.mergeKey()).isEqualTo("order:1");
        assertThat(second.lane()).isEqualTo(Lane.BACKGROUND);
        runtime.ackDone(second).require();
    }

    @Test
    void batchWorkOfOneInboxRunsFourAtATime_andTheFifthWaitsForAFreeSlot() {
        for (int job = 1; job <= 5; job++) {
            write("u42", on(Lane.BATCH, "job-" + job));
        }
        List<ClaimedRun> atWork = new ArrayList<>();
        for (int worker = 1; worker <= 4; worker++) {
            atWork.add(claim("worker-" + worker, "u42").require());
        }
        assertThat(atWork).extracting(ClaimedRun::lane).containsOnly(Lane.BATCH);
        assertThat(count("SELECT count(*) FROM run WHERE status = 'active' AND inbox_id = 'u42'")).isEqualTo(4);

        ApiResult<ClaimedRun> fifth = claim("worker-5", "u42");
        assertThat(fifth.status()).isEqualTo(204);
        assertThat(fifth.header(RunController.HEADER_REASON)).contains("busy");

        // And what a person waits for is not held up by any of it.
        write("u42", on(Lane.INTERACTIVE, "question"));
        ClaimedRun question = claim("worker-5", "u42").require();
        assertThat(question.lane()).isEqualTo(Lane.INTERACTIVE);
        runtime.ackDone(question).require();

        runtime.ackDone(atWork.getFirst()).require();
        ClaimedRun last = claim("worker-5", "u42").require();
        assertThat(last.events().getFirst().sourceEventId()).isEqualTo("job-5");
        runtime.ackDone(last).require();
        atWork.subList(1, 4).forEach(run -> runtime.ackDone(run).require());
    }

    @Test
    void lanesAreServedInPriorityOrder_whateverTheArrivalOrder() {
        write("u42", on(Lane.BATCH, "backfill"));
        write("u42", on(Lane.BACKGROUND, "report"));
        write("u42", on(Lane.INTERACTIVE, "question"));

        ClaimedRun first = claim("worker-1", "u42").require();
        assertThat(first.lane()).isEqualTo(Lane.INTERACTIVE);
        runtime.ackDone(first).require();
        ClaimedRun second = claim("worker-1", "u42").require();
        assertThat(second.lane()).isEqualTo(Lane.BACKGROUND);
        runtime.ackDone(second).require();
        ClaimedRun third = claim("worker-1", "u42").require();
        assertThat(third.lane()).isEqualTo(Lane.BATCH);
        runtime.ackDone(third).require();
    }

    @Test
    void aWorkerPoolCanServeOnlySomeLanes() {
        write("u42", on(Lane.INTERACTIVE, "question"));
        write("u42", on(Lane.BATCH, "backfill"));

        ClaimedRun batch = runtime.claim(ClaimRequest.inbox("batch-worker", ACME, "u42").withLanes(Lane.BATCH)).require();
        assertThat(batch.lane()).isEqualTo(Lane.BATCH);
        assertThat(runtime.claim(ClaimRequest.inbox("batch-worker", ACME, "u42").withLanes(Lane.BATCH)).status())
                .isEqualTo(204);
        ClaimedRun interactive = claim("worker-1", "u42").require();
        runtime.ackDone(batch).require();
        runtime.ackDone(interactive).require();
    }

    @Test
    void capsCanBeChangedPerInbox() {
        write("u42", on(Lane.BACKGROUND, "b1"));
        write("u42", on(Lane.BACKGROUND, "b2"));
        jdbc.sql("UPDATE inbox SET cap_background = 1 WHERE inbox_id = 'u42'").update();

        ClaimedRun first = claim("worker-1", "u42").require();
        assertThat(claim("worker-2", "u42").status()).isEqualTo(204);
        runtime.ackDone(first).require();
        runtime.ackDone(claim("worker-2", "u42").require()).require();
    }

    @Test
    void claimWithoutAnInbox_findsWorkAcrossInboxesAndTenants_oldestFirst() {
        write("alice", on(Lane.INTERACTIVE, "a1"));
        clock.advance(Duration.ofSeconds(1));
        globex.writeEvent("carol", on(Lane.INTERACTIVE, "c1").build()).require();
        clock.advance(Duration.ofSeconds(1));
        write("bob", on(Lane.INTERACTIVE, "b1"));

        ClaimedRun first = runtime.claim(ClaimRequest.any("worker-1")).require();
        assertThat(first.tenantId()).isEqualTo(ACME);
        assertThat(first.inboxId()).isEqualTo("alice");
        ClaimedRun second = runtime.claim(ClaimRequest.any("worker-1")).require();
        assertThat(second.tenantId()).isEqualTo(GLOBEX);
        assertThat(second.inboxId()).isEqualTo("carol");
        ClaimedRun third = runtime.claim(ClaimRequest.any("worker-1")).require();
        assertThat(third.inboxId()).isEqualTo("bob");
        ApiResult<ClaimedRun> nothing = runtime.claim(ClaimRequest.any("worker-1"));
        assertThat(nothing.status()).isEqualTo(204);
        assertThat(nothing.header(RunController.HEADER_REASON)).contains("empty");

        runtime.ackDone(first).require();
        runtime.ackDone(second).require();
        runtime.ackDone(third).require();
    }

    @Test
    void aClaimCanBeLimitedToOneTenant() {
        write("alice", on(Lane.INTERACTIVE, "a1"));
        globex.writeEvent("carol", on(Lane.INTERACTIVE, "c1").build()).require();

        ClaimedRun run = runtime.claim(new ClaimRequest("worker-1", GLOBEX, null, null, null, null)).require();
        assertThat(run.tenantId()).isEqualTo(GLOBEX);
        assertThat(runtime.claim(new ClaimRequest("worker-1", GLOBEX, null, null, null, null)).status()).isEqualTo(204);
        // naming an inbox without its tenant is ambiguous at platform scope
        assertThat(runtime.claim(new ClaimRequest("worker-1", null, "alice", null, null, null)).status()).isEqualTo(400);
        assertThat(runtime.claim(new ClaimRequest(null, null, null, null, null, null)).status()).isEqualTo(400);
        runtime.ackDone(run).require();
        runtime.ackDone(claim("worker-1", "alice").require()).require();
    }
}
