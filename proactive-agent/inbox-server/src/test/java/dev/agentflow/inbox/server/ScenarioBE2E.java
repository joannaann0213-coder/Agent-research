package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.client.MemoryOutbox;
import dev.agentflow.inbox.client.OutboxMessage;
import dev.agentflow.inbox.client.ScriptedWorker;
import dev.agentflow.inbox.client.ScriptedWorkerMain;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Scenario B (and the F5 acceptance check): the user sends five messages in a row. They become one
 * run, and the user gets one complete answer that reflects all five, not five fragments.
 */
class ScenarioBE2E extends IntegrationTestBase {

    @Test
    void fiveMessagesInARow_oneRun_oneReplyThatReflectsAllFive() {
        String[] parts = {"can you check my order", "the one from this morning", "number 1234", "it has not arrived",
                "and the courier does not answer"};
        for (int i = 0; i < parts.length; i++) {
            write("u42", message("msg-" + i, "u42", parts[i]));
            clock.advance(Duration.ofMillis(300));
        }

        MemoryOutbox outbox = new MemoryOutbox();
        ScriptedWorker worker = new ScriptedWorker(runtime, outbox, "worker-1", context -> {
            context.send("im", ScriptedWorkerMain.summary(context));
            return ScriptedWorker.Decision.done();
        });
        ClaimRequest inbox = ClaimRequest.inbox("worker-1", ACME, "u42");

        assertThat(worker.runOnce(inbox)).as("the user may still be typing").isFalse();
        clock.advance(Duration.ofMillis(1200)); // 1.5 s of silence after the last message
        assertThat(worker.runOnce(inbox)).isTrue();
        assertThat(worker.runOnce(inbox)).isFalse();

        assertThat(count("SELECT count(*) FROM run")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(5);
        assertThat(count("SELECT count(DISTINCT run_id) FROM event")).isEqualTo(1);
        assertThat(outbox.delivered()).extracting(OutboxMessage::text).singleElement().satisfies(reply -> {
            assertThat(reply).startsWith("handled 5 event(s)");
            assertThat(reply).containsSubsequence(parts);
        });
    }
}
