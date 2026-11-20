package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.SteerRequest;
import dev.agentflow.inbox.api.SteerResponse;
import dev.agentflow.inbox.api.SuspendRequest;
import dev.agentflow.inbox.api.TransitionView;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * F9: events that arrive while a run is in progress. The inbox makes the mechanical call (can this
 * join the current run, is something asking it to yield); the agent makes the semantic one. Whatever
 * the agent decides, no event is lost: the worst case is that it is handled after the run.
 */
class F9DeliveryModesIT extends IntegrationTestBase {

    private static final Duration WINDOW = Duration.ofMillis(1500);

    /** The conversation of user u42 has a run in progress on its first message. */
    private ClaimedRun runningConversation(boolean acceptsSteer) {
        write("u42", message("msg-1", "u42", "summarise the report"));
        clock.advance(WINDOW);
        return runtime.claim(ClaimRequest.inbox("worker-1", ACME, "u42").withSteer(acceptsSteer)).require();
    }

    private HeartbeatResponse heartbeat(ClaimedRun run) {
        return runtime.heartbeat(run.runId(), new HeartbeatRequest(run.epoch(), null)).require();
    }

    // ---- steer ----------------------------------------------------------------------------------

    @Test
    void steer_aLaterMessageOfTheSameConversationJoinsTheRunningRun() {
        ClaimedRun run = runningConversation(true);
        String second = write("u42", message("msg-2", "u42", "only the risks section please"));

        assertThat(heartbeat(run).steerPending()).isEqualTo(1);
        assertThat(eventStatus(second)).isEqualTo("pending");

        // between two tool calls the runtime pulls what arrived
        SteerResponse steered = runtime.steer(run.runId(), new SteerRequest(run.epoch(), run.lastRunSeq())).require();
        assertThat(steered.events()).singleElement().satisfies(event -> {
            assertThat(event.eventId()).isEqualTo(second);
            assertThat(event.runSeq()).isEqualTo(2);
            assertThat(event.deliveryMode()).isEqualTo("steer");
        });
        assertThat(heartbeat(run).steerPending()).isZero();

        runtime.ack(run.runId(), AckRequest.done(run.epoch(), 2)).require();
        assertThat(count("SELECT count(*) FROM run")).as("no second run was needed").isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(2);
        assertThat(acme.getEvent("u42", second).require().transitions()).extracting(TransitionView::reason)
                .containsExactly("accepted", "steer", "ack");
    }

    @Test
    void steer_aLostResponseCanBeFetchedAgain() {
        ClaimedRun run = runningConversation(true);
        write("u42", message("msg-2", "u42", "and the costs"));

        SteerResponse first = runtime.steer(run.runId(), new SteerRequest(run.epoch(), 1)).require();
        SteerResponse again = runtime.steer(run.runId(), new SteerRequest(run.epoch(), 1)).require();

        assertThat(again.events()).extracting(ClaimedEvent::eventId)
                .containsExactlyElementsOf(first.events().stream().map(ClaimedEvent::eventId).toList());
        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'steer'")).isEqualTo(1);
        runtime.ack(run.runId(), AckRequest.done(run.epoch(), 2)).require();
    }

    @Test
    void steer_onlyTheRunsOwnKeyCanJoin() {
        ClaimedRun run = runningConversation(true);
        String other = write("u42", webhook("order-1", "{}").mergeKey("order:1"));
        clock.advance(WINDOW);

        assertThat(heartbeat(run).steerPending()).isZero();
        assertThat(runtime.steer(run.runId(), new SteerRequest(run.epoch(), 1)).require().events()).isEmpty();
        assertThat(eventStatus(other)).isEqualTo("pending");

        runtime.ackDone(run).require();
        ClaimedRun next = claim("worker-1", "u42").require(); // handled afterwards: a followup
        assertThat(next.events()).extracting(ClaimedEvent::eventId).containsExactly(other);
        runtime.ackDone(next).require();
    }

    @Test
    void steer_whatWasPulledButNotConsumedGoesBackToTheQueue() {
        ClaimedRun run = runningConversation(true);
        String second = write("u42", message("msg-2", "u42", "one more thing"));

        // the event joins the run, but the worker finishes without ever having read it
        runtime.steer(run.runId(), new SteerRequest(run.epoch(), 1)).require();
        assertThat(eventStatus(second)).isEqualTo("claimed");
        runtime.ack(run.runId(), AckRequest.done(run.epoch(), 1)).require();

        EventDetail released = acme.getEvent("u42", second).require();
        assertThat(released.event().status()).isEqualTo("pending");
        assertThat(released.event().runId()).isNull();
        assertThat(released.transitions()).extracting(TransitionView::reason).containsExactly("accepted", "steer", "released");

        // nothing is lost: it becomes a run of its own, once its quiet window has passed
        clock.advance(WINDOW);
        ClaimedRun next = claim("worker-1", "u42").require();
        assertThat(next.runId()).isNotEqualTo(run.runId());
        assertThat(next.events()).extracting(ClaimedEvent::eventId).containsExactly(second);
        runtime.ackDone(next).require();
    }

    @Test
    void steer_needsTheLease() {
        ClaimedRun run = runningConversation(true);
        assertThat(runtime.steer(run.runId(), new SteerRequest(run.epoch() + 1, 1)).status()).isEqualTo(409);
        runtime.ackDone(run).require();
        assertThat(runtime.steer(run.runId(), new SteerRequest(run.epoch(), 1)).status()).isEqualTo(409);
    }

    // ---- followup and collect -------------------------------------------------------------------

    @Test
    void followup_aRunThatDoesNotAcceptSteeringIsNotDisturbed() {
        ClaimedRun run = runningConversation(false);
        String second = write("u42", message("msg-2", "u42", "also check the appendix"));

        assertThat(heartbeat(run).steerPending()).as("reported, but not forced on the run").isEqualTo(1);
        assertThat(runtime.steer(run.runId(), new SteerRequest(run.epoch(), 1)).require().events()).isEmpty();
        assertThat(claim("worker-2", "u42").status()).as("one run per conversation at a time").isEqualTo(204);

        clock.advance(WINDOW);
        runtime.ackDone(run).require();
        ClaimedRun next = claim("worker-2", "u42").require(); // runs right after the first one
        assertThat(next.events()).extracting(ClaimedEvent::eventId).containsExactly(second);
        runtime.ackDone(next).require();
        assertThat(count("SELECT count(*) FROM run")).isEqualTo(2);
    }

    @Test
    void collect_aBurstThatArrivesMidRunBecomesOneFollowOnRun() {
        ClaimedRun run = runningConversation(false);
        write("u42", message("msg-2", "u42", "wait"));
        write("u42", message("msg-3", "u42", "actually"));
        write("u42", message("msg-4", "u42", "use last quarter's numbers"));
        clock.advance(WINDOW);
        runtime.ackDone(run).require();

        ClaimedRun next = claim("worker-1", "u42").require();
        assertThat(next.events()).hasSize(3);
        assertThat(next.events()).extracting(e -> e.payload().get("text").stringValue())
                .containsExactly("wait", "actually", "use last quarter's numbers");
        runtime.ackDone(next).require();
    }

    // ---- interrupt ------------------------------------------------------------------------------

    @Test
    void interrupt_theRunYieldsTheNewMessageGoesFirstAndTheOldRunResumesLater() {
        ClaimedRun run = runningConversation(false);
        String queuedEarlier = write("u42", webhook("order-9", "{}")); // waits behind the run
        assertThat(heartbeat(run).interruptRequested()).isFalse();

        String stop = write("u42", message("msg-2", "u42", "stop"));
        HeartbeatResponse beat = heartbeat(run);
        assertThat(beat.interruptRequested()).isTrue();
        assertThat(beat.interruptEventId()).isEqualTo(stop);

        // the agent agrees to yield: it keeps its state and suspends; it is not killed
        runtime.suspend(run.runId(), new SuspendRequest(run.epoch(), "resume:" + run.runId(), 3600L, null, 1)).require();

        // the interrupting message is handled first, ahead of what was queued before it, without a quiet window
        ClaimedRun interrupting = claim("worker-1", "u42").require();
        assertThat(interrupting.events()).extracting(ClaimedEvent::eventId).containsExactly(stop);
        assertThat(interrupting.events().getFirst().interrupt()).isTrue();
        runtime.ackDone(interrupting).require();

        ClaimedRun earlier = claim("worker-1", "u42").require();
        assertThat(earlier.events()).extracting(ClaimedEvent::eventId).containsExactly(queuedEarlier);
        runtime.ackDone(earlier).require();

        // later the user says "carry on": that event carries the resume key and the old run continues
        String carryOn = write("u42", WriteEventRequest.builder(EventSource.CONVERSATION, "conversation.message.v1")
                .sourceEventId("msg-3").mergeKey("conversation:u42").resumeKey("resume:" + run.runId())
                .payloadJson("{\"text\":\"carry on\"}"));
        ClaimedRun resumed = claim("worker-1", "u42").require();
        assertThat(resumed.runId()).isEqualTo(run.runId());
        assertThat(resumed.kind()).isEqualTo(ClaimedRun.KIND_RESUME);
        assertThat(resumed.events()).extracting(ClaimedEvent::eventId).containsExactly(carryOn);
        runtime.ackDone(resumed).require();

        assertThat(count("SELECT count(*) FROM event WHERE status <> 'done'")).isZero();
    }

    @Test
    void interrupt_ifTheAgentDeclinesTheMessageIsSimplyHandledAfterTheRun() {
        ClaimedRun run = runningConversation(false);
        String stop = write("u42", message("msg-2", "u42", "cancel"));
        assertThat(heartbeat(run).interruptRequested()).isTrue();

        // the agent judges that it is nearly done and finishes normally
        runtime.ackDone(run).require();

        ClaimedRun next = claim("worker-1", "u42").require();
        assertThat(next.events()).extracting(ClaimedEvent::eventId).containsExactly(stop);
        runtime.ackDone(next).require();
        assertThat(acme.getEvent("u42", stop).require().event().status()).isEqualTo("done");
    }

    @Test
    void interrupt_forAnIdleKeyAsksTheRunThatFillsItsLane_andNoOtherLane() {
        // an order alert is being handled on the interactive lane, a report on the background lane
        write("u42", webhook("order-1", "{}").mergeKey("order:1"));
        write("u42", WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "report.requested.v1")
                .sourceEventId("report-1").lane(Lane.BACKGROUND).payloadJson("{}"));
        clock.advance(WINDOW);
        ClaimedRun alert = claim("worker-1", "u42").require();
        ClaimedRun report = claim("worker-2", "u42").require();
        assertThat(alert.lane()).isEqualTo(Lane.INTERACTIVE);
        assertThat(report.lane()).isEqualTo(Lane.BACKGROUND);

        // the user says stop in a conversation nobody is working on; the interactive lane is full
        String stop = write("u42", message("msg-1", "u42", "停止"));
        assertThat(heartbeat(alert).interruptRequested()).isTrue();
        assertThat(heartbeat(alert).interruptEventId()).isEqualTo(stop);
        assertThat(heartbeat(report).interruptRequested()).as("another lane is not asked to yield").isFalse();

        runtime.ackDone(alert).require();
        runtime.ackDone(report).require();
        runtime.ackDone(claim("worker-1", "u42").require()).require();
    }

    @Test
    void interrupt_isSetExplicitlyOrByAnExactStopWord() {
        write("u1", message("m1", "c", "please stop the music"));
        write("u1", message("m2", "c", "  STOP "));
        write("u1", message("m3", "c", "取消"));
        write("u1", webhook("w1", "{\"text\":\"stop\"}")); // not a conversation message
        write("u1", webhook("w2", "{}").interrupt(true)); // the producer asked for it
        write("u1", message("m4", "c", "stop").interrupt(false)); // the producer overrides the detector

        assertThat(jdbc.sql("SELECT source_event_id FROM event WHERE interrupt ORDER BY seq").query(String.class).list())
                .containsExactly("m2", "m3", "w2");
    }
}
