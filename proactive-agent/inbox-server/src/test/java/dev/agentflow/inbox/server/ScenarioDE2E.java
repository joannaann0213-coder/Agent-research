package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.api.TransitionView;
import dev.agentflow.inbox.client.FileOutbox;
import dev.agentflow.inbox.client.OutboxMessage;
import dev.agentflow.inbox.client.ScriptedWorker;
import dev.agentflow.inbox.client.ScriptedWorkerMain;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Scenario D (and the F3 acceptance check): a worker is killed in the middle of a run, after it has
 * already sent its reply. Nothing is lost and nothing is sent twice: the lease expires, the same run
 * is issued to another worker, and the outbox recognises the reply by its run id and sequence.
 *
 * <p>The doomed worker is a real, separate JVM that this test kills without warning.
 */
class ScenarioDE2E extends IntegrationTestBase {

    @Autowired
    LeaseSweeper leaseSweeper;

    @Test
    void workerKilledMidRun_noEventLost_sameRunReplayed_replySentOnce(@TempDir Path outboxDirectory) throws Exception {
        // The other JVM signs its requests with the real time, so the test clock starts there.
        clock.set(Instant.now().truncatedTo(ChronoUnit.MICROS));

        String eventId = write("crash", webhook("order-1234-v7", "{\"order_id\":\"1234\"}"));

        Process doomed = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                ScriptedWorkerMain.class.getName(),
                "--base-url", baseUri().toString(),
                "--client-id", "runtime-1",
                "--secret", "runtime-secret",
                "--worker-id", "worker-doomed",
                "--outbox-dir", outboxDirectory.toString(),
                "--behaviour", "reply-then-hang",
                "--wait-ms", "5000")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        String runId;
        try {
            // It claims the run, sends its reply and reports that it did; then it hangs before acking.
            String sent = CompletableFuture.supplyAsync(() -> readUntil(doomed, "SENT ")).get(60, TimeUnit.SECONDS);
            runId = sent.split(" ")[1];
            assertThat(sent.split(" ")[2]).as("epoch").isEqualTo("1");
        } finally {
            doomed.destroyForcibly(); // kill -9
        }
        assertThat(doomed.waitFor(10, TimeUnit.SECONDS)).isTrue();

        FileOutbox outbox = new FileOutbox(outboxDirectory);
        assertThat(outbox.delivered()).as("the reply went out before the crash").hasSize(1);
        assertThat(eventStatus(eventId)).as("the event is stuck with the dead worker").isEqualTo("claimed");
        assertThat(claim("worker-2", "crash").status()).as("until its lease runs out").isEqualTo(204);

        // 60 s later the lease is over and the sweeper takes the run back
        clock.advance(Duration.ofSeconds(61));
        assertThat(leaseSweeper.runOnce()).isEqualTo(1);
        assertThat(eventStatus(eventId)).isEqualTo("pending");

        // another worker replays the same run from the top, with the same outbox
        ScriptedWorker successor = new ScriptedWorker(runtime, outbox, "worker-2", context -> {
            assertThat(context.run().runId()).as("replay yields the same run").isEqualTo(runId);
            assertThat(context.run().epoch()).isEqualTo(2);
            assertThat(context.send("im", ScriptedWorkerMain.summary(context)))
                    .as("the outbox has seen this reply already").isFalse();
            return ScriptedWorker.Decision.done();
        });
        assertThat(successor.runOnce(ClaimRequest.inbox("worker-2", ACME, "crash"))).isTrue();

        // the user heard exactly once
        List<OutboxMessage> delivered = outbox.delivered();
        assertThat(delivered).singleElement().satisfies(message -> {
            assertThat(message.dedupeKey()).isEqualTo(runId + ":1");
            assertThat(message.text()).isEqualTo("handled 1 event(s): order.paid.v1 order-1234-v7");
        });

        // and the final state is the one a run without a crash reaches
        String controlId = write("control", webhook("order-1234-v7", "{\"order_id\":\"1234\"}"));
        ScriptedWorker control = new ScriptedWorker(runtime, outbox, "worker-3", context -> {
            context.send("im", ScriptedWorkerMain.summary(context));
            return ScriptedWorker.Decision.done();
        });
        assertThat(control.runOnce(ClaimRequest.inbox("worker-3", ACME, "control"))).isTrue();

        EventDetail crashed = acme.getEvent("crash", eventId).require();
        EventDetail normal = acme.getEvent("control", controlId).require();
        assertThat(crashed.event().status()).isEqualTo(normal.event().status()).isEqualTo("done");
        assertThat(normal.transitions()).extracting(TransitionView::reason).containsExactly("accepted", "claim", "ack");
        assertThat(crashed.transitions()).extracting(TransitionView::reason)
                .containsExactly("accepted", "claim", "lease_expired", "redelivery", "ack");
        assertThat(crashed.transitions()).extracting(TransitionView::actor).containsExactly(
                "client:acme-bot", "worker:worker-doomed", "system:lease-sweeper", "worker:worker-2", "worker:worker-2");
        RunView run = runtime.getRun(runId).require();
        assertThat(run.status()).isEqualTo("done");
        assertThat(run.attempt()).isEqualTo(2);
        assertThat(outbox.delivered()).as("one reply per run, crash or no crash").hasSize(2);
    }

    /** Reads the process's standard output until a line starts with {@code prefix}. */
    private static String readUntil(Process process, String prefix) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(prefix)) {
                    return line;
                }
            }
            throw new IllegalStateException("the worker exited without printing " + prefix);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
