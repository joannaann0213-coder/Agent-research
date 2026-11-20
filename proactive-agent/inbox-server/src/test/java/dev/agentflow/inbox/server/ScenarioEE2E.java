package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.MemoryOutbox;
import dev.agentflow.inbox.client.OutboxMessage;
import dev.agentflow.inbox.client.ScriptedWorker;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.timer.TimerScheduler;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Scenario E: a timed follow-up. Inside a run the agent decides to look again in two hours. It does
 * not sleep: it writes itself an event with a future {@code ready_at}, so a restart cannot lose the
 * follow-up and no run is held open while waiting. When the time comes it may well decide to say nothing.
 */
class ScenarioEE2E extends IntegrationTestBase {

    @Autowired
    TimerScheduler timerScheduler;

    private final MemoryOutbox outbox = new MemoryOutbox();
    /** Stand-in for what the agent would read from memory and thread state when it wakes up. */
    private final AtomicBoolean supplierReplied = new AtomicBoolean();

    private ScriptedWorker.Decision agent(ScriptedWorker.RunContext context) {
        var event = context.events().getFirst();
        if (event.type().equals("conversation.message.v1")) {
            // "remind me if the supplier has not replied in two hours"
            context.client().writeEvent(ACME, context.run().inboxId(),
                    WriteEventRequest.builder(EventSource.AGENT_SELF, "agent.followup.v1")
                            .sourceEventId(context.run().runId() + ":followup-1")
                            .readyAt(clock.now().plus(Duration.ofHours(2)))
                            .payloadJson("{\"check\":\"supplier reply\"}").build()).require();
            context.send("im", "I will check in two hours and let you know if there is still no reply.");
            return ScriptedWorker.Decision.done();
        }
        // the follow-up: look first, speak only if it is worth it
        if (!supplierReplied.get()) {
            context.send("im", "Two hours have passed and the supplier has not replied yet.");
        }
        return ScriptedWorker.Decision.done();
    }

    private ScriptedWorker worker() {
        return new ScriptedWorker(agent, outbox, "worker-1", this::agent);
    }

    private void userAsksForAFollowUp() {
        write("u42", message("msg-1", "u42", "remind me if the supplier has not replied in two hours"));
        clock.advance(Duration.ofMillis(1500));
        assertThat(worker().runOnce(ClaimRequest.inbox("worker-1", ACME, "u42"))).isTrue();

        // the follow-up exists only as a timer: nothing is queued and no run is being held open
        assertThat(count("SELECT count(*) FROM timer WHERE state = 'scheduled'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE status <> 'done'")).isZero();
        assertThat(count("SELECT count(*) FROM run WHERE status <> 'done'")).isZero();
        assertThat(worker().runOnce()).isFalse();
        assertThat(outbox.delivered()).hasSize(1);
    }

    @Test
    void twoHoursLater_noReplyYet_theAgentSendsTheReminder() {
        userAsksForAFollowUp();

        clock.advance(Duration.ofMinutes(119));
        assertThat(timerScheduler.runOnce()).isZero();
        clock.advance(Duration.ofMinutes(1));
        assertThat(timerScheduler.runOnce()).isEqualTo(1);

        assertThat(worker().runOnce()).isTrue();
        assertThat(outbox.delivered()).extracting(OutboxMessage::text)
                .contains("Two hours have passed and the supplier has not replied yet.");
        assertThat(count("SELECT count(*) FROM event WHERE source = 'agent_self' AND status = 'done' AND lane = 'background'"))
                .isEqualTo(1);
    }

    @Test
    void twoHoursLater_theSupplierHasReplied_theAgentStaysSilent() {
        userAsksForAFollowUp();
        supplierReplied.set(true);

        clock.advance(Duration.ofHours(2));
        assertThat(timerScheduler.runOnce()).isEqualTo(1);
        assertThat(worker().runOnce()).isTrue();

        // the run happened and ended, and it said nothing: silence is a normal ending
        assertThat(outbox.delivered()).as("only the first acknowledgement").hasSize(1);
        assertThat(count("SELECT count(*) FROM run WHERE status = 'done'")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(2);
    }
}
