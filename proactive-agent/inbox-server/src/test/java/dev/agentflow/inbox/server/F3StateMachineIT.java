package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.api.TransitionView;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.api.RunController;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * F3: events are persisted, their state is queryable and every move is recorded. After a crash the
 * unit of replay is the event (through its run), not the request.
 */
class F3StateMachineIT extends IntegrationTestBase {

    @Autowired
    LeaseSweeper leaseSweeper;

    @Test
    void normalPath_everyTransitionSaysWhoWhenFromToAndWhy() {
        String eventId = write("u42", webhook("order-1", "{\"amount\":299}"));

        clock.advance(Duration.ofSeconds(1));
        ClaimedRun run = claim("worker-1", "u42").require();
        assertThat(run.kind()).isEqualTo(ClaimedRun.KIND_START);
        assertThat(run.epoch()).isEqualTo(1);
        assertThat(run.attempt()).isEqualTo(1);
        assertThat(run.leaseExpiresAt()).isEqualTo(clock.now().plusSeconds(60));
        assertThat(run.leaseDeadlineAt()).isEqualTo(clock.now().plus(Duration.ofMinutes(30)));
        assertThat(run.events()).singleElement().satisfies(event -> {
            assertThat(event.eventId()).isEqualTo(eventId);
            assertThat(event.runSeq()).isEqualTo(1);
            assertThat(event.deliveryMode()).isEqualTo("batch");
            assertThat(event.payload().get("amount").intValue()).isEqualTo(299);
        });
        assertThat(eventStatus(eventId)).isEqualTo("claimed");

        clock.advance(Duration.ofSeconds(5));
        RunOutcome outcome = runtime.ackDone(run).require();
        assertThat(outcome.status()).isEqualTo("done");

        EventDetail detail = acme.getEvent("u42", eventId).require();
        assertThat(detail.event().status()).isEqualTo("done");
        assertThat(detail.event().runId()).isEqualTo(run.runId());
        assertThat(detail.event().finishedAt()).isEqualTo(clock.now());
        List<TransitionView> history = detail.transitions();
        assertThat(history).extracting(TransitionView::fromStatus).containsExactly(null, "pending", "claimed");
        assertThat(history).extracting(TransitionView::toStatus).containsExactly("pending", "claimed", "done");
        assertThat(history).extracting(TransitionView::reason).containsExactly("accepted", "claim", "ack");
        assertThat(history).extracting(TransitionView::actor)
                .containsExactly("client:acme-bot", "worker:worker-1", "worker:worker-1");
        assertThat(history.get(1).runId()).isEqualTo(run.runId());
        assertThat(history.get(1).at()).isEqualTo(clock.now().minusSeconds(5));
        assertThat(history.get(2).at()).isEqualTo(clock.now());

        // the inbox is free again and there is nothing left to claim
        ApiResult<ClaimedRun> next = claim("worker-1", "u42");
        assertThat(next.status()).isEqualTo(204);
        assertThat(next.header(RunController.HEADER_REASON)).contains("empty");
    }

    @Test
    void nackRetriesWithExponentialBackoff_thenTheEventIsDead() {
        String eventId = write("u42", webhook("order-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();
        String runId = run.runId();

        long[] expectedBackoffSeconds = {2, 4, 8, 16};
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThat(run.attempt()).isEqualTo(attempt);
            assertThat(run.epoch()).isEqualTo(attempt);
            RunOutcome nacked = runtime.nack(runId, new NackRequest(run.epoch(), "downstream timeout", null)).require();
            assertThat(nacked.status()).isEqualTo("queued");
            assertThat(nacked.nextAttemptAt()).isEqualTo(clock.now().plusSeconds(expectedBackoffSeconds[attempt - 1]));
            assertThat(eventStatus(eventId)).isEqualTo("pending");

            // not before the backoff has passed
            ApiResult<ClaimedRun> early = claim("worker-1", "u42");
            assertThat(early.status()).isEqualTo(204);
            assertThat(early.header(RunController.HEADER_REASON)).contains("backoff");
            assertThat(early.header(RunController.HEADER_RETRY_AFTER_MS))
                    .contains(Long.toString(expectedBackoffSeconds[attempt - 1] * 1000));

            clock.advance(Duration.ofSeconds(expectedBackoffSeconds[attempt - 1]));
            run = claim("worker-1", "u42").require();
            assertThat(run.runId()).as("the same run is issued again").isEqualTo(runId);
            assertThat(run.kind()).isEqualTo(ClaimedRun.KIND_REDELIVERY);
            assertThat(run.events()).extracting(e -> e.eventId()).containsExactly(eventId);
        }

        // the fifth attempt fails too: the attempt limit is reached
        RunOutcome dead = runtime.nack(runId, new NackRequest(run.epoch(), "still failing", null)).require();
        assertThat(dead.status()).isEqualTo("dead");
        assertThat(eventStatus(eventId)).isEqualTo("dead");
        assertThat(claim("worker-1", "u42").status()).isEqualTo(204);

        EventDetail detail = acme.getEvent("u42", eventId).require();
        assertThat(detail.event().attempt()).isEqualTo(5);
        assertThat(detail.event().lastError()).isEqualTo("still failing");
        assertThat(detail.transitions().getLast().reason()).isEqualTo("attempts_exhausted");
        assertThat(detail.transitions()).filteredOn(t -> t.reason().equals("nack")).hasSize(4);
        assertThat(detail.transitions()).filteredOn(t -> t.reason().equals("redelivery")).hasSize(4);
    }

    @Test
    void aClaimWithoutAnInboxIsToldWhenTheBackoffEnds() {
        write("u42", webhook("order-1", "{}"));
        ClaimedRun run = runtime.claim(ClaimRequest.any("worker-1")).require();
        runtime.nack(run.runId(), new NackRequest(run.epoch(), "downstream timeout", null)).require();

        // Nothing is claimable anywhere, but time alone will change that, so the claimer is told when.
        ApiResult<ClaimedRun> early = runtime.claim(ClaimRequest.any("worker-2"));
        assertThat(early.status()).isEqualTo(204);
        assertThat(early.header(RunController.HEADER_REASON)).contains("backoff");
        assertThat(early.header(RunController.HEADER_RETRY_AFTER_MS)).contains("2000");

        // A claimer that does not serve the run's lane has nothing to wait for.
        ApiResult<ClaimedRun> otherLane = runtime.claim(ClaimRequest.any("worker-2").withLanes(Lane.BATCH));
        assertThat(otherLane.header(RunController.HEADER_REASON)).contains("empty");
        assertThat(otherLane.header(RunController.HEADER_RETRY_AFTER_MS)).isEmpty();

        clock.advance(Duration.ofSeconds(2));
        ClaimedRun again = runtime.claim(ClaimRequest.any("worker-2")).require();
        assertThat(again.runId()).isEqualTo(run.runId());
        assertThat(again.kind()).isEqualTo(ClaimedRun.KIND_REDELIVERY);
    }

    @Test
    void nackMayAskForItsOwnRetryDelay() {
        write("u42", webhook("order-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();

        RunOutcome nacked = runtime.nack(run.runId(), new NackRequest(run.epoch(), "rate limited", 30_000L)).require();
        assertThat(nacked.nextAttemptAt()).isEqualTo(clock.now().plusSeconds(30));
        // an absurd delay is bounded by the backoff cap (5 minutes)
        clock.advance(Duration.ofSeconds(30));
        run = claim("worker-1", "u42").require();
        nacked = runtime.nack(run.runId(), new NackRequest(run.epoch(), "rate limited", 86_400_000L)).require();
        assertThat(nacked.nextAttemptAt()).isEqualTo(clock.now().plus(Duration.ofMinutes(5)));
    }

    @Test
    void ackFailedIsFinal_itIsNotRetried() {
        String eventId = write("u42", webhook("order-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();

        RunOutcome outcome = runtime.ack(run.runId(), AckRequest.failed(run.epoch(), run.lastRunSeq(),
                "order does not exist")).require();
        assertThat(outcome.status()).isEqualTo("failed");

        clock.advance(Duration.ofHours(1));
        assertThat(claim("worker-1", "u42").status()).isEqualTo(204);
        EventDetail detail = acme.getEvent("u42", eventId).require();
        assertThat(detail.event().status()).isEqualTo("failed");
        assertThat(detail.event().lastError()).isEqualTo("order does not exist");
        assertThat(detail.transitions().getLast().reason()).isEqualTo("fail");
    }

    @Test
    void abandonedRunIsReclaimedAfterItsLease_andTheSameRunComesBack() {
        String eventId = write("u42", webhook("order-1", "{}"));
        ClaimedRun first = claim("worker-1", "u42").require();

        // worker-1 dies here. Its events stay claimed until the lease runs out.
        clock.advance(Duration.ofSeconds(59));
        assertThat(leaseSweeper.runOnce()).isZero();
        assertThat(eventStatus(eventId)).isEqualTo("claimed");
        assertThat(claim("worker-2", "u42").status()).as("the inbox is still held").isEqualTo(204);

        clock.advance(Duration.ofSeconds(2));
        assertThat(leaseSweeper.runOnce()).isEqualTo(1);
        assertThat(eventStatus(eventId)).isEqualTo("pending");

        ClaimedRun second = claim("worker-2", "u42").require();
        assertThat(second.runId()).as("replay yields the same run").isEqualTo(first.runId());
        assertThat(second.kind()).isEqualTo(ClaimedRun.KIND_REDELIVERY);
        assertThat(second.epoch()).isEqualTo(2);
        assertThat(second.attempt()).isEqualTo(2);
        assertThat(second.events()).extracting(e -> e.eventId()).containsExactly(eventId);

        // worker-1 was only slow, not dead: its late ack is refused, it must stop
        ApiResult<RunOutcome> stale = runtime.ackDone(first);
        assertThat(stale.status()).isEqualTo(409);
        assertThat(stale.code()).isEqualTo(ProblemCodes.LEASE_LOST);
        assertThat(runtime.heartbeat(first.runId(), new HeartbeatRequest(first.epoch(), null)).status()).isEqualTo(409);

        runtime.ackDone(second).require();

        // the final state is the same as on the normal path
        EventDetail detail = acme.getEvent("u42", eventId).require();
        assertThat(detail.event().status()).isEqualTo("done");
        assertThat(detail.transitions()).extracting(TransitionView::reason)
                .containsExactly("accepted", "claim", "lease_expired", "redelivery", "ack");
        assertThat(detail.transitions().get(2).actor()).isEqualTo("system:lease-sweeper");
        RunView view = runtime.getRun(first.runId()).require();
        assertThat(view.status()).isEqualTo("done");
        assertThat(view.queuedReason()).isEqualTo("lease_expired");
    }

    @Test
    void aSameKeyEventThatArrivesBeforeTheReclaimDoesNotChangeTheRun() {
        String first = write("u42", webhook("order-1-v1", "{\"v\":1}").mergeKey("order:1"));
        clock.advance(Duration.ofSeconds(2)); // past the quiet window
        ClaimedRun run = claim("worker-1", "u42").require();
        assertThat(run.events()).hasSize(1);

        // the worker crashes; meanwhile a newer event with the same key arrives
        String second = write("u42", webhook("order-1-v2", "{\"v\":2}").mergeKey("order:1"));
        clock.advance(Duration.ofSeconds(61));
        assertThat(leaseSweeper.runOnce()).isEqualTo(1);

        ClaimedRun again = claim("worker-2", "u42").require();
        assertThat(again.runId()).isEqualTo(run.runId());
        assertThat(again.events()).extracting(e -> e.eventId())
                .as("the re-issued run has exactly its original events").containsExactly(first);
        runtime.ackDone(again).require();

        // the newer event is a run of its own afterwards
        ClaimedRun next = claim("worker-2", "u42").require();
        assertThat(next.runId()).isNotEqualTo(run.runId());
        assertThat(next.events()).extracting(e -> e.eventId()).containsExactly(second);
        runtime.ackDone(next).require();
    }

    @Test
    void repeatedLeaseExpiryEndsInDeadToo() {
        String eventId = write("u42", webhook("poison", "{}"));
        for (int attempt = 1; attempt <= 5; attempt++) {
            ClaimedRun run = claim("worker-" + attempt, "u42").require();
            assertThat(run.attempt()).isEqualTo(attempt);
            clock.advance(Duration.ofSeconds(61)); // every worker crashes on it
            assertThat(leaseSweeper.runOnce()).isEqualTo(1);
        }
        assertThat(eventStatus(eventId)).isEqualTo("dead");
        assertThat(claim("worker-6", "u42").status()).isEqualTo(204);
    }

    @Test
    void heartbeatKeepsARunAlive() {
        write("u42", webhook("order-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();

        for (int i = 0; i < 5; i++) {
            clock.advance(Duration.ofSeconds(50));
            HeartbeatResponse beat = runtime.heartbeat(run.runId(), new HeartbeatRequest(run.epoch(), null)).require();
            assertThat(beat.extended()).isTrue();
            assertThat(beat.leaseExpiresAt()).isEqualTo(clock.now().plusSeconds(60));
            assertThat(leaseSweeper.runOnce()).isZero();
        }
        runtime.ackDone(run).require();
    }

    @Test
    void ackIsIdempotent_andGuardedByTheEpoch() {
        write("u42", webhook("order-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();

        assertThat(runtime.ack(run.runId(), AckRequest.done(run.epoch() + 1, 1)).status()).isEqualTo(409);
        assertThat(runtime.ackDone(run).require().status()).isEqualTo("done");
        assertThat(runtime.ackDone(run).require().status()).as("the same ack again is a no-op").isEqualTo("done");
        assertThat(runtime.ack(run.runId(), AckRequest.failed(run.epoch(), 1, "changed my mind")).status())
                .as("a different outcome after the fact is refused").isEqualTo(409);
        assertThat(runtime.ack("01NOSUCHRUN000000000000000", AckRequest.done(1, 1)).status()).isEqualTo(404);
        assertThat(runtime.ack(run.runId(), new AckRequest(run.epoch(), "maybe", 1, null)).status()).isEqualTo(400);

        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'ack'")).isEqualTo(1);
    }

    @Test
    void anAckThatArrivesAfterTheLeaseRanOutButBeforeTheReclaimStillCounts() {
        String eventId = write("u42", webhook("order-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();

        clock.advance(Duration.ofSeconds(90)); // lease is over, the sweeper has not run yet
        assertThat(runtime.ackDone(run).require().status()).isEqualTo("done");
        assertThat(leaseSweeper.runOnce()).isZero();
        assertThat(eventStatus(eventId)).isEqualTo("done");
    }
}
