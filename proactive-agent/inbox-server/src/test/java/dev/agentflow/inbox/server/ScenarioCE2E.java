package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.MemoryOutbox;
import dev.agentflow.inbox.client.OutboxMessage;
import dev.agentflow.inbox.client.ScriptedWorker;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import dev.agentflow.inbox.server.sweep.SuspendSweeper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Scenario C: a high-risk action needs a human. The run sends an approval card and suspends; the
 * user's answer comes back through the inbox as an event and the same run continues from where it
 * stopped. The scripted worker stands in for the agent and for its saved session state.
 */
class ScenarioCE2E extends IntegrationTestBase {

    private static final String RESUME_KEY = "approval:7788";

    @Autowired
    SuspendSweeper suspendSweeper;

    @Autowired
    LeaseSweeper leaseSweeper;

    private final MemoryOutbox outbox = new MemoryOutbox();
    private final AtomicBoolean crashAfterApplying = new AtomicBoolean();

    /**
     * Phase 1: ask. Phase 2 (the answer is among the events): act on it. Message 1 is always the card
     * and message 2 always the result, however the run got to that point.
     */
    private ScriptedWorker.Decision agent(ScriptedWorker.RunContext context) {
        var answer = context.events().stream().filter(e -> e.type().equals("approval.reply.v1")).findFirst();
        if (answer.isEmpty()) {
            if (ClaimedRun.KIND_RESUME.equals(context.run().kind()) && "timeout".equals(context.run().resumeReason())) {
                return ScriptedWorker.Decision.done(); // nobody answered: end quietly, nothing is applied
            }
            context.send(1, "approval", "Approve bulk configuration change #7788?");
            return new ScriptedWorker.Decision.Suspend(RESUME_KEY, 86_400, null);
        }
        String decision = answer.get().payload().get("decision").stringValue();
        context.send(2, "im", "approved".equals(decision) ? "Change #7788 was applied." : "Change #7788 was not applied.");
        if (crashAfterApplying.getAndSet(false)) {
            throw new ScriptedWorker.LeaseLostException(context.run().runId()); // the worker dies before acking
        }
        return ScriptedWorker.Decision.done();
    }

    private ScriptedWorker worker(String id) {
        return new ScriptedWorker(runtime, outbox, id, this::agent);
    }

    private void requestChange() {
        write("u42", webhook("change-7788", "{\"action\":\"bulk config change\"}").mergeKey("change:7788"));
        clock.advance(Duration.ofSeconds(2));
    }

    private void userAnswers(String decision) {
        write("u42", WriteEventRequest.builder(EventSource.CONVERSATION, "approval.reply.v1")
                .sourceEventId("approval-7788").resumeKey(RESUME_KEY).payloadJson("{\"decision\":\"" + decision + "\"}"));
    }

    @Test
    void theRunAsksSuspendsAndContinuesWhenTheUserApproves() {
        requestChange();
        ClaimRequest inbox = ClaimRequest.inbox("worker", ACME, "u42");

        assertThat(worker("worker-1").runOnce(inbox)).isTrue();
        assertThat(outbox.delivered()).extracting(OutboxMessage::channel).containsExactly("approval");
        assertThat(count("SELECT count(*) FROM run WHERE status = 'suspended'")).isEqualTo(1);
        assertThat(ops.inboxStatus(ACME, "u42").require().activeRuns()).as("the inbox is free while it waits").isZero();
        assertThat(worker("worker-1").runOnce(inbox)).as("nothing to do until the user answers").isFalse();

        clock.advance(Duration.ofHours(3));
        userAnswers("approved");
        userAnswers("approved"); // a double click is one event

        assertThat(worker("worker-2").runOnce(inbox)).isTrue();
        assertThat(outbox.delivered()).extracting(OutboxMessage::text).containsExactlyInAnyOrder(
                "Approve bulk configuration change #7788?", "Change #7788 was applied.");
        // the approval card and the result travel separately, and both belong to the same run
        assertThat(outbox.delivered()).extracting(OutboxMessage::channel).containsExactlyInAnyOrder("approval", "im");
        assertThat(outbox.delivered()).extracting(OutboxMessage::runId).containsOnly(outbox.delivered().getFirst().runId());

        assertThat(count("SELECT count(*) FROM run")).as("one run did all of it").isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(2);
        assertThat(worker("worker-2").runOnce(inbox)).isFalse();
    }

    @Test
    void aRejectionIsAnAnswerToo() {
        requestChange();
        ClaimRequest inbox = ClaimRequest.inbox("worker", ACME, "u42");
        worker("worker-1").runOnce(inbox);
        userAnswers("rejected");
        worker("worker-1").runOnce(inbox);

        assertThat(outbox.delivered()).extracting(OutboxMessage::text).contains("Change #7788 was not applied.");
    }

    @Test
    void ifNobodyAnswersTheRunEndsSilently() {
        requestChange();
        ClaimRequest inbox = ClaimRequest.inbox("worker", ACME, "u42");
        worker("worker-1").runOnce(inbox);

        clock.advance(Duration.ofHours(24));
        assertThat(suspendSweeper.runOnce()).isEqualTo(1);

        assertThat(worker("worker-1").runOnce(inbox)).as("the runtime is not even woken").isFalse();
        assertThat(outbox.delivered()).as("only the card was ever sent").hasSize(1);
        assertThat(count("SELECT count(*) FROM run WHERE status = 'done' AND end_reason = 'suspend_timeout'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(1);
    }

    @Test
    void aCrashAfterApplyingTheChangeDoesNotApplyOrAnnounceItTwice() {
        requestChange();
        ClaimRequest inbox = ClaimRequest.inbox("worker", ACME, "u42");
        worker("worker-1").runOnce(inbox);
        userAnswers("approved");

        crashAfterApplying.set(true);
        assertThat(worker("worker-1").runOnce(inbox)).isTrue(); // sends the result, then dies without acking
        assertThat(outbox.delivered()).hasSize(2);
        assertThat(count("SELECT count(*) FROM run WHERE status = 'active'")).isEqualTo(1);

        clock.advance(Duration.ofSeconds(61));
        assertThat(leaseSweeper.runOnce()).isEqualTo(1);

        // The successor gets the same run with everything it holds, replays the last step, and the
        // outbox recognises both messages: the user still has exactly one card and one result.
        assertThat(worker("worker-2").runOnce(inbox)).isTrue();
        assertThat(outbox.delivered()).hasSize(2);
        assertThat(count("SELECT count(*) FROM run")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM run WHERE status = 'done'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(2);
    }
}
