package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.AuditEntry;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.DeadLetterRequest;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.EventView;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.ReplayRequest;
import dev.agentflow.inbox.api.ReplayResponse;
import dev.agentflow.inbox.api.TransitionView;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * F10: replay, audit and self-service visibility. An operator can put events back in the queue one
 * by one, by inbox and time range, or by source; a producer can see what became of its own events.
 */
class F10ReplayAuditIT extends IntegrationTestBase {

    private static final Instant T0 = MutableClock.T0;

    /** Writes an event and drives it to {@code failed}. */
    private String failedEvent(String inbox, String key) {
        String eventId = write(inbox, webhook(key, "{}"));
        ClaimedRun run = claim("worker-1", inbox).require();
        runtime.ack(run.runId(), AckRequest.failed(run.epoch(), run.lastRunSeq(), "downstream rejected it")).require();
        return eventId;
    }

    private String doneEvent(String inbox, String key) {
        String eventId = write(inbox, webhook(key, "{}"));
        runtime.ackDone(claim("worker-1", inbox).require()).require();
        return eventId;
    }

    // ---- visibility -----------------------------------------------------------------------------

    @Test
    void oneCallShowsAnEventsWholePathFromAcceptanceToItsFinalState() {
        String eventId = write("u42", webhook("order-1", "{}"));
        clock.advance(Duration.ofSeconds(1));
        ClaimedRun first = claim("worker-1", "u42").require();
        clock.advance(Duration.ofSeconds(3));
        runtime.nack(first.runId(), new NackRequest(first.epoch(), "payment service timed out", null)).require();
        clock.advance(Duration.ofSeconds(2));
        ClaimedRun second = claim("worker-2", "u42").require();
        clock.advance(Duration.ofSeconds(4));
        runtime.ackDone(second).require();

        // list, then open the event: two requests show everything
        EventView[] listed = acme.listEvents("u42", "status=done").require();
        assertThat(listed).extracting(EventView::eventId).containsExactly(eventId);
        EventDetail detail = acme.getEvent("u42", eventId).require();

        List<TransitionView> history = detail.transitions();
        assertThat(history).extracting(TransitionView::reason)
                .containsExactly("accepted", "claim", "nack", "redelivery", "ack");
        assertThat(history).extracting(TransitionView::toStatus)
                .containsExactly("pending", "claimed", "pending", "claimed", "done");
        assertThat(history).extracting(TransitionView::actor).containsExactly(
                "client:acme-bot", "worker:worker-1", "worker:worker-1", "worker:worker-2", "worker:worker-2");
        assertThat(history).extracting(TransitionView::at).containsExactly(
                T0, T0.plusSeconds(1), T0.plusSeconds(4), T0.plusSeconds(6), T0.plusSeconds(10));
        assertThat(history).extracting(TransitionView::attempt).containsExactly(null, 1, 1, 2, 2);
        assertThat(detail.event().attempt()).isEqualTo(2);
    }

    @Test
    void aProducerSeesTheStateAndTheFailureReasonOfItsOwnEvents() {
        String failed = failedEvent("u42", "order-1");
        String waiting = write("u42", webhook("order-2", "{}"));

        EventDetail detail = acme.getEvent("u42", failed).require();
        assertThat(detail.event().status()).isEqualTo("failed");
        assertThat(detail.event().lastError()).isEqualTo("downstream rejected it");
        assertThat(acme.listEvents("u42", "status=failed").require()).extracting(EventView::eventId).containsExactly(failed);
        assertThat(acme.listEvents("u42", "status=pending").require()).extracting(EventView::eventId).containsExactly(waiting);
        assertThat(acme.listEvents("u42", "").require()).hasSize(2);
        // a colleague producer sees only what it wrote itself; the operator sees the tenant
        assertThat(acme2.listEvents("u42", "").require()).isEmpty();
        assertThat(ops.listEvents("u42", "tenant_id=acme&source=biz_webhook").require()).hasSize(2);
    }

    // ---- replay ---------------------------------------------------------------------------------

    @Test
    void aFailedEventIsReplayedWithoutForce_andWhoAndWhyAreRecorded() {
        String eventId = failedEvent("u42", "order-1");
        String oldRun = acme.getEvent("u42", eventId).require().event().runId();
        clock.advance(Duration.ofMinutes(30));

        ReplayResponse response = ops.replay(ReplayRequest.event(ACME, eventId, false,
                "payment service is back", "alice")).require();
        assertThat(response.replayed()).containsExactly(eventId);
        assertThat(response.skipped()).isEmpty();

        EventDetail detail = acme.getEvent("u42", eventId).require();
        assertThat(detail.event().status()).isEqualTo("pending");
        assertThat(detail.event().runId()).isNull();
        assertThat(detail.event().attempt()).isZero();
        assertThat(detail.event().lastError()).isNull();
        TransitionView replay = detail.transitions().getLast();
        assertThat(replay.fromStatus()).isEqualTo("failed");
        assertThat(replay.toStatus()).isEqualTo("pending");
        assertThat(replay.reason()).isEqualTo("replay");
        assertThat(replay.actor()).isEqualTo("operator:alice");
        assertThat(replay.detail().get("reason").stringValue()).isEqualTo("payment service is back");
        assertThat(replay.detail().get("force").booleanValue()).isFalse();

        AuditEntry entry = ops.audit(ACME, 10).require()[0];
        assertThat(entry.action()).isEqualTo("replay");
        assertThat(entry.operator()).isEqualTo("alice");
        assertThat(entry.actorClientId()).isEqualTo("ops");
        assertThat(entry.target().get("event_id").stringValue()).isEqualTo(eventId);
        assertThat(entry.params().get("force").booleanValue()).isFalse();
        assertThat(entry.result().get("replayed").intValue()).isEqualTo(1);
        assertThat(entry.at()).isEqualTo(clock.now());

        // it is processed again, in a new run
        ClaimedRun again = claim("worker-1", "u42").require();
        assertThat(again.runId()).isNotEqualTo(oldRun);
        assertThat(again.attempt()).isEqualTo(1);
        runtime.ackDone(again).require();
        assertThat(eventStatus(eventId)).isEqualTo("done");
    }

    @Test
    void doneAndDeadEventsNeedForce() {
        String done = doneEvent("u42", "order-done");
        String dead = write("u42", webhook("order-dead", "{}"));
        ops.deadLetter(dead, new DeadLetterRequest(ACME, "known bad order", "bob")).require();

        ReplayResponse cautious = ops.replay(ReplayRequest.inbox(ACME, "u42", T0, T0.plusSeconds(1), false,
                "look again", "alice")).require();
        assertThat(cautious.replayed()).isEmpty();
        assertThat(cautious.skipped()).extracting(ReplayResponse.Skipped::eventId).containsExactly(done, dead);
        assertThat(cautious.skipped()).extracting(ReplayResponse.Skipped::why).containsOnly(ReplayResponse.NEEDS_FORCE);
        assertThat(eventStatus(done)).isEqualTo("done");

        ReplayResponse forced = ops.replay(ReplayRequest.inbox(ACME, "u42", T0, T0.plusSeconds(1), true,
                "reprocess after the pricing fix", "alice")).require();
        assertThat(forced.replayed()).containsExactly(done, dead);
        assertThat(eventStatus(done)).isEqualTo("pending");
        assertThat(eventStatus(dead)).isEqualTo("pending");
        assertThat(acme.getEvent("u42", done).require().transitions().getLast().detail().get("force").booleanValue()).isTrue();

        ClaimedRun first = claim("worker-1", "u42").require();
        runtime.ackDone(first).require();
        runtime.ackDone(claim("worker-1", "u42").require()).require();
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(2);
    }

    @Test
    void eventsThatAreStillInFlightAreNeverReplayed() {
        String claimed = write("u42", webhook("order-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();
        String pending = write("u42", webhook("order-2", "{}"));

        ReplayResponse response = ops.replay(ReplayRequest.inbox(ACME, "u42", T0, T0.plusSeconds(1), true,
                "curious", "alice")).require();
        assertThat(response.replayed()).isEmpty();
        assertThat(response.skipped()).extracting(ReplayResponse.Skipped::eventId).containsExactly(claimed, pending);
        assertThat(response.skipped()).extracting(ReplayResponse.Skipped::why).containsOnly(ReplayResponse.IN_FLIGHT);
        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'replay'")).isZero();
        runtime.ackDone(run).require();
    }

    @Test
    void replayByInboxAndTimeRange_isHalfOpenOnTheTimeReceived() {
        String early = failedEvent("u42", "at-0");
        clock.advance(Duration.ofMinutes(10));
        String inRange = failedEvent("u42", "at-10");
        String otherInbox = failedEvent("u43", "at-10");
        clock.advance(Duration.ofMinutes(10));
        String late = failedEvent("u42", "at-20");

        ReplayResponse response = ops.replay(ReplayRequest.inbox(ACME, "u42", T0.plus(Duration.ofMinutes(10)),
                T0.plus(Duration.ofMinutes(20)), false, "outage between 10:10 and 10:20", "alice")).require();

        assertThat(response.replayed()).containsExactly(inRange);
        assertThat(eventStatus(early)).isEqualTo("failed");
        assertThat(eventStatus(late)).as("the end of the range is exclusive").isEqualTo("failed");
        assertThat(eventStatus(otherInbox)).isEqualTo("failed");
    }

    @Test
    void replayBySource_coversEveryInboxOfTheTenant_andNoOtherTenant() {
        WriteEventRequest.Builder mail = WriteEventRequest.builder(EventSource.CONNECTOR, "mail.received.v1").payloadJson("{}");
        String a = fail("u1", mail.sourceEventId("mail-1"));
        String b = fail("u2", mail.sourceEventId("mail-2"));
        String webhook = failedEvent("u1", "order-1");
        globex.writeEvent("u1", mail.sourceEventId("mail-9").build()).require();

        ReplayResponse response = ops.replay(ReplayRequest.source(ACME, EventSource.CONNECTOR, T0, T0.plusSeconds(1),
                false, "mail connector was misconfigured", "alice")).require();

        assertThat(response.replayed()).containsExactlyInAnyOrder(a, b);
        assertThat(eventStatus(webhook)).isEqualTo("failed");
        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'replay' AND tenant_id = 'globex'")).isZero();
    }

    private String fail(String inbox, WriteEventRequest.Builder event) {
        String eventId = write(inbox, event);
        ClaimedRun run = claim("worker-1", inbox).require();
        runtime.ack(run.runId(), AckRequest.failed(run.epoch(), run.lastRunSeq(), "bad")).require();
        return eventId;
    }

    @Test
    void aDryRunReportsWithoutChangingAnything() {
        String failed = failedEvent("u42", "order-1");
        String done = doneEvent("u42", "order-2");

        ReplayResponse preview = ops.replay(ReplayRequest.inbox(ACME, "u42", T0, T0.plusSeconds(1), false,
                "what would happen", "alice").asDryRun()).require();
        assertThat(preview.dryRun()).isTrue();
        assertThat(preview.replayed()).containsExactly(failed);
        assertThat(preview.skipped()).extracting(ReplayResponse.Skipped::eventId).containsExactly(done);

        assertThat(eventStatus(failed)).isEqualTo("failed");
        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'replay'")).isZero();
        assertThat(ops.audit(ACME, 10).require()).isEmpty();
    }

    @Test
    void aReplayMustSayWhy_andNameExactlyOneSelector() {
        String eventId = failedEvent("u42", "order-1");

        assertThat(ops.replay(ReplayRequest.event(ACME, eventId, false, " ", "alice")).status()).isEqualTo(400);
        assertThat(ops.replay(new ReplayRequest(ACME, null, null, null, null, null, false, "why", "alice", null))
                .status()).as("no selector").isEqualTo(400);
        assertThat(ops.replay(new ReplayRequest(ACME, eventId, "u42", null, T0, T0.plusSeconds(1), false, "why", "alice", null))
                .status()).as("two selectors").isEqualTo(400);
        assertThat(ops.replay(new ReplayRequest(ACME, null, "u42", null, T0, null, false, "why", "alice", null))
                .status()).as("open range").isEqualTo(400);
        assertThat(ops.replay(ReplayRequest.event(null, eventId, false, "why", "alice")).status())
                .as("an operator must name the tenant").isEqualTo(400);
        assertThat(ops.replay(ReplayRequest.event(GLOBEX, eventId, false, "why", "alice")).status())
                .as("the event belongs to another tenant").isEqualTo(404);
        assertThat(eventStatus(eventId)).isEqualTo("failed");
    }

    // ---- manual dead-letter ---------------------------------------------------------------------

    @Test
    void aWaitingEventCanBeDeadLetteredByHand_andOnlyAWaitingOne() {
        String poison = write("u42", webhook("order-poison", "{}"));
        String busy = write("u42", webhook("order-busy", "{}").lane(dev.agentflow.inbox.api.Lane.BACKGROUND));

        ClaimedRun run = runtime.claim(dev.agentflow.inbox.api.ClaimRequest.inbox("worker-1", ACME, "u42")
                .withLanes(dev.agentflow.inbox.api.Lane.BACKGROUND)).require();
        assertThat(ops.deadLetter(busy, new DeadLetterRequest(ACME, "too late", "bob")).status())
                .as("a claimed event belongs to its run").isEqualTo(409);

        assertThat(ops.deadLetter(poison, new DeadLetterRequest(ACME, "crashes every worker", "bob")).status()).isEqualTo(204);
        EventDetail detail = acme.getEvent("u42", poison).require();
        assertThat(detail.event().status()).isEqualTo("dead");
        assertThat(detail.event().lastError()).isEqualTo("crashes every worker");
        assertThat(detail.transitions().getLast().reason()).isEqualTo("manual_dead");
        assertThat(detail.transitions().getLast().actor()).isEqualTo("operator:bob");
        assertThat(ops.audit(ACME, 10).require()[0].action()).isEqualTo("manual_dead");

        assertThat(claim("worker-2", "u42").status()).as("it is out of the queue").isEqualTo(204);
        assertThat(ops.deadLetter(poison, new DeadLetterRequest(ACME, "again", "bob")).code()).isEqualTo(ProblemCodes.CONFLICT);
        assertThat(ops.deadLetter(poison, new DeadLetterRequest(ACME, "", "bob")).status()).isEqualTo(400);
        assertThat(ops.deadLetter("01NOSUCHEVENT0000000000000", new DeadLetterRequest(ACME, "x", "bob")).status()).isEqualTo(404);
        runtime.ackDone(run).require();
    }

    @Test
    void theAuditLogIsNewestFirstAndCanBeReadPerTenant() {
        String a = failedEvent("u42", "order-1");
        ops.replay(ReplayRequest.event(ACME, a, false, "first", "alice")).require();
        clock.advance(Duration.ofSeconds(5));
        String g = globex.writeEvent("u1", webhook("order-1", "{}").build()).require().eventId();
        ops.deadLetter(g, new DeadLetterRequest(GLOBEX, "second", "bob")).require();

        assertThat(ops.audit(null, 10).require()).extracting(AuditEntry::action).containsExactly("manual_dead", "replay");
        assertThat(ops.audit(ACME, 10).require()).extracting(AuditEntry::operator).containsExactly("alice");
        assertThat(ops.audit(GLOBEX, 10).require()).extracting(AuditEntry::operator).containsExactly("bob");
        assertThat(ops.audit(null, 1).require()).hasSize(1);
    }
}
