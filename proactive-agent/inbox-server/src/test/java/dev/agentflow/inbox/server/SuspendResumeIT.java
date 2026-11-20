package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.api.SuspendRequest;
import dev.agentflow.inbox.api.TransitionView;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import dev.agentflow.inbox.server.sweep.SuspendSweeper;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Suspend and resume (scenario C). A run that needs an approval suspends with a resume key and a
 * mandatory timeout. The answer is not a callback: it is a new event in the same inbox, so the resume
 * path gets dedupe, queueing, audit and replay from the same mechanism as everything else.
 */
class SuspendResumeIT extends IntegrationTestBase {

    private static final String KEY = "approval:7788";

    @Autowired
    SuspendSweeper suspendSweeper;

    @Autowired
    LeaseSweeper leaseSweeper;

    /** A run that asked for approval and is waiting for it. */
    private ClaimedRun suspendedRun(long timeoutSeconds, String onTimeout) {
        write("u42", webhook("change-request-1", "{\"action\":\"bulk config change\"}").mergeKey("change:1"));
        clock.advance(Duration.ofSeconds(2));
        ClaimedRun run = claim("worker-1", "u42").require();
        RunOutcome outcome = runtime.suspend(run.runId(),
                new SuspendRequest(run.epoch(), KEY, timeoutSeconds, onTimeout, run.lastRunSeq())).require();
        assertThat(outcome.status()).isEqualTo("suspended");
        return run;
    }

    private WriteEventRequest.Builder approvalReply(String id) {
        return WriteEventRequest.builder(EventSource.CONVERSATION, "approval.reply.v1")
                .sourceEventId(id).resumeKey(KEY).payloadJson("{\"decision\":\"approved\"}");
    }

    @Test
    void suspendFreesTheInbox_andTheAnswerResumesTheSameRun() {
        ClaimedRun run = suspendedRun(86_400, null);
        String triggerId = run.events().getFirst().eventId();

        RunView waiting = runtime.getRun(run.runId()).require();
        assertThat(waiting.status()).isEqualTo("suspended");
        assertThat(waiting.slotNo()).isNull();
        assertThat(waiting.leaseExpiresAt()).isNull();
        assertThat(waiting.suspendDeadlineAt()).isEqualTo(clock.now().plus(Duration.ofHours(24)));
        assertThat(eventStatus(triggerId)).as("consumed, but not finished until the run is").isEqualTo("claimed");

        // the inbox is free: other work proceeds, even for the same key
        String unrelated = write("u42", webhook("change-request-2", "{}").mergeKey("change:1"));
        clock.advance(Duration.ofSeconds(2));
        ClaimedRun other = claim("worker-2", "u42").require();
        assertThat(other.runId()).isNotEqualTo(run.runId());
        assertThat(other.events()).extracting(ClaimedEvent::eventId).containsExactly(unrelated);
        runtime.ackDone(other).require();

        // the user approves: an ordinary event, deduped like any other
        clock.advance(Duration.ofMinutes(10));
        String reply = write("u42", approvalReply("approval-7788"));
        assertThat(acme.writeEvent("u42", approvalReply("approval-7788").build()).require().deduped()).isTrue();

        ClaimedRun resumed = claim("worker-3", "u42").require();
        assertThat(resumed.runId()).as("no new run: the original one continues").isEqualTo(run.runId());
        assertThat(resumed.kind()).isEqualTo(ClaimedRun.KIND_RESUME);
        assertThat(resumed.resumeReason()).isEqualTo("event");
        assertThat(resumed.epoch()).isEqualTo(2);
        assertThat(resumed.attempt()).as("waiting for an answer is not a failure").isEqualTo(1);
        assertThat(resumed.events()).singleElement().satisfies(event -> {
            assertThat(event.eventId()).isEqualTo(reply);
            assertThat(event.runSeq()).isEqualTo(2);
            assertThat(event.deliveryMode()).isEqualTo("resume");
            assertThat(event.payload().get("decision").stringValue()).isEqualTo("approved");
        });

        runtime.ack(resumed.runId(), AckRequest.done(resumed.epoch(), 2)).require();
        assertThat(eventStatus(triggerId)).isEqualTo("done");
        assertThat(eventStatus(reply)).isEqualTo("done");
        EventDetail trigger = acme.getEvent("u42", triggerId).require();
        assertThat(trigger.transitions()).extracting(TransitionView::reason)
                .containsExactly("accepted", "claim", "suspended", "ack");
        assertThat(trigger.transitions().get(2).detail().get("resume_key").stringValue()).isEqualTo(KEY);
        assertThat(runtime.getRun(run.runId()).require().status()).isEqualTo("done");
        assertThat(suspendSweeper.runOnce()).isZero();
    }

    @Test
    void suspendRequiresAResumeKeyAndATimeout() {
        write("u42", webhook("change-request-1", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();

        ApiResult<RunOutcome> noTimeout = runtime.suspend(run.runId(), new SuspendRequest(run.epoch(), KEY, null, null, 1));
        assertThat(noTimeout.status()).isEqualTo(400);
        assertThat(noTimeout.problem().detail()).contains("timeout_seconds");
        assertThat(runtime.suspend(run.runId(), new SuspendRequest(run.epoch(), KEY, 0L, null, 1)).status()).isEqualTo(400);
        assertThat(runtime.suspend(run.runId(), new SuspendRequest(run.epoch(), " ", 60L, null, 1)).status()).isEqualTo(400);
        assertThat(runtime.suspend(run.runId(), new SuspendRequest(run.epoch(), KEY, 60L, "explode", 1)).status()).isEqualTo(400);
        assertThat(runtime.suspend(run.runId(), new SuspendRequest(run.epoch() + 1, KEY, 60L, null, 1)).status()).isEqualTo(409);
        assertThat(runtime.getRun(run.runId()).require().status()).as("still running").isEqualTo("active");

        // a valid suspend, and the same call again, are one suspension
        assertThat(runtime.suspend(run.runId(), new SuspendRequest(run.epoch(), KEY, 60L, null, 1)).require().status())
                .isEqualTo("suspended");
        assertThat(runtime.suspend(run.runId(), new SuspendRequest(run.epoch(), KEY, 60L, null, 1)).require().status())
                .isEqualTo("suspended");
        // an absurd timeout is bounded (30 days)
        assertThat(runtime.getRun(run.runId()).require().suspendDeadlineAt()).isEqualTo(clock.now().plusSeconds(60));
    }

    @Test
    void aResumeKeyIsUniqueAmongTheInboxesSuspendedRuns() {
        suspendedRun(3600, null);
        write("u42", webhook("change-request-2", "{}"));
        ClaimedRun second = claim("worker-2", "u42").require();

        ApiResult<RunOutcome> clash = runtime.suspend(second.runId(), new SuspendRequest(second.epoch(), KEY, 3600L, null, 1));
        assertThat(clash.status()).isEqualTo(409);
        assertThat(clash.code()).isEqualTo(ProblemCodes.RESUME_KEY_IN_USE);
        runtime.ackDone(second).require();
    }

    @Test
    void timeout_byDefaultTheRunEndsSilently_withTheReasonOnRecord() {
        ClaimedRun run = suspendedRun(3600, null);
        String triggerId = run.events().getFirst().eventId();

        clock.advance(Duration.ofSeconds(3599));
        assertThat(suspendSweeper.runOnce()).isZero();
        clock.advance(Duration.ofSeconds(1));
        assertThat(suspendSweeper.runOnce()).isEqualTo(1);
        assertThat(suspendSweeper.runOnce()).isZero();

        RunView ended = runtime.getRun(run.runId()).require();
        assertThat(ended.status()).isEqualTo("done");
        assertThat(ended.endReason()).isEqualTo("suspend_timeout");
        EventDetail trigger = acme.getEvent("u42", triggerId).require();
        assertThat(trigger.event().status()).isEqualTo("done");
        assertThat(trigger.transitions().getLast().reason()).isEqualTo("suspend_timeout");
        assertThat(trigger.transitions().getLast().actor()).isEqualTo("system:suspend-sweeper");
        assertThat(claim("worker-1", "u42").status()).as("nothing was sent to the runtime").isEqualTo(204);

        // an approval that comes too late is not lost either: it is an ordinary event now
        String late = write("u42", approvalReply("approval-7788"));
        ClaimedRun lateRun = claim("worker-1", "u42").require();
        assertThat(lateRun.kind()).isEqualTo(ClaimedRun.KIND_START);
        assertThat(lateRun.runId()).isNotEqualTo(run.runId());
        assertThat(lateRun.events()).singleElement().satisfies(e -> {
            assertThat(e.eventId()).isEqualTo(late);
            assertThat(e.resumeKey()).isEqualTo(KEY);
        });
        runtime.ackDone(lateRun).require();
    }

    @Test
    void timeout_aRunThatAskedToBeToldIsHandedBack() {
        ClaimedRun run = suspendedRun(3600, SuspendRequest.RESUME);
        clock.advance(Duration.ofSeconds(3600));
        assertThat(suspendSweeper.runOnce()).isEqualTo(1);

        ClaimedRun resumed = claim("worker-2", "u42").require();
        assertThat(resumed.runId()).isEqualTo(run.runId());
        assertThat(resumed.kind()).isEqualTo(ClaimedRun.KIND_RESUME);
        assertThat(resumed.resumeReason()).isEqualTo("timeout");
        assertThat(resumed.epoch()).isEqualTo(2);
        assertThat(resumed.attempt()).isEqualTo(1);
        assertThat(resumed.events()).extracting(ClaimedEvent::eventId)
                .containsExactly(run.events().getFirst().eventId());

        // the runtime records "not approved" and ends the run
        runtime.ackDone(resumed).require();
        assertThat(runtime.getRun(run.runId()).require().status()).isEqualTo("done");
    }

    @Test
    void anAnswerThatArrivedInTimeWinsOverTheTimeout_evenIfItWasNotClaimedYet() {
        ClaimedRun run = suspendedRun(3600, null);
        clock.advance(Duration.ofSeconds(3599));
        write("u42", approvalReply("approval-7788")); // one second before the deadline

        clock.advance(Duration.ofSeconds(120)); // nobody claimed it for two minutes
        assertThat(suspendSweeper.runOnce()).as("the deadline is about when the answer arrived").isZero();

        ClaimedRun resumed = claim("worker-1", "u42").require();
        assertThat(resumed.runId()).isEqualTo(run.runId());
        assertThat(resumed.kind()).isEqualTo(ClaimedRun.KIND_RESUME);
        runtime.ack(resumed.runId(), AckRequest.done(resumed.epoch(), 2)).require();
    }

    @Test
    void aResumedRunThatCrashesIsRedeliveredWithEverythingItHolds() {
        ClaimedRun run = suspendedRun(3600, null);
        String reply = write("u42", approvalReply("approval-7788"));
        ClaimedRun resumed = claim("worker-1", "u42").require();
        assertThat(resumed.epoch()).isEqualTo(2);

        clock.advance(Duration.ofSeconds(61)); // worker-1 dies while handling the approval
        assertThat(leaseSweeper.runOnce()).isEqualTo(1);

        ClaimedRun redelivered = claim("worker-2", "u42").require();
        assertThat(redelivered.runId()).isEqualTo(run.runId());
        assertThat(redelivered.kind()).isEqualTo(ClaimedRun.KIND_REDELIVERY);
        assertThat(redelivered.epoch()).isEqualTo(3);
        assertThat(redelivered.attempt()).isEqualTo(2);
        assertThat(redelivered.events()).extracting(ClaimedEvent::eventId)
                .containsExactly(run.events().getFirst().eventId(), reply);
        runtime.ackDone(redelivered).require();
    }

    @Test
    void aSecondAnswerIsAnOrdinaryEvent() {
        ClaimedRun run = suspendedRun(3600, null);
        write("u42", approvalReply("approval-7788"));
        ClaimedRun resumed = claim("worker-1", "u42").require();
        runtime.ack(resumed.runId(), AckRequest.done(resumed.epoch(), 2)).require();

        String again = write("u42", approvalReply("approval-7788-clicked-twice"));
        ClaimedRun ordinary = claim("worker-1", "u42").require();
        assertThat(ordinary.kind()).isEqualTo(ClaimedRun.KIND_START);
        assertThat(ordinary.runId()).isNotEqualTo(run.runId());
        assertThat(ordinary.events()).extracting(ClaimedEvent::eventId).containsExactly(again);
        runtime.ackDone(ordinary).require();
    }

    @Test
    void twoAnswersWaitingAtOnceBothReachTheRun() {
        ClaimedRun run = suspendedRun(3600, null);
        String first = write("u42", approvalReply("approval-a"));
        String second = write("u42", approvalReply("approval-b"));

        ClaimedRun resumed = claim("worker-1", "u42").require();
        assertThat(resumed.events()).extracting(ClaimedEvent::eventId).containsExactly(first, second);
        assertThat(resumed.events()).extracting(ClaimedEvent::runSeq).containsExactly(2, 3);
        runtime.ackDone(resumed).require();
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(3);
        assertThat(runtime.getRun(run.runId()).require().status()).isEqualTo("done");
    }

    @RepeatedTest(15)
    void anAnswerRacingTheTimeoutHasExactlyOneOutcome() throws Exception {
        ClaimedRun run = suspendedRun(3600, null);
        clock.advance(Duration.ofSeconds(3601));
        String late = write("u42", approvalReply("approval-7788")); // arrives after the deadline

        CyclicBarrier start = new CyclicBarrier(2);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CompletableFuture<ApiResult<ClaimedRun>> claimer = CompletableFuture.supplyAsync(() -> {
                await(start);
                return claim("worker-1", "u42");
            }, pool);
            CompletableFuture<Integer> sweeper = CompletableFuture.supplyAsync(() -> {
                await(start);
                return suspendSweeper.runOnce();
            }, pool);

            ClaimedRun claimed = claimer.get().require();
            int timedOut = sweeper.get();
            if (claimed.kind().equals(ClaimedRun.KIND_RESUME)) {
                // the claim got there first: the run continues and the sweeper finds nothing to do
                assertThat(timedOut).isZero();
                assertThat(claimed.runId()).isEqualTo(run.runId());
            } else {
                // the timeout got there first: the run is over and the answer is an ordinary event
                assertThat(timedOut).isEqualTo(1);
                assertThat(claimed.kind()).isEqualTo(ClaimedRun.KIND_START);
                assertThat(runtime.getRun(run.runId()).require().endReason()).isEqualTo("suspend_timeout");
            }
            assertThat(claimed.events()).extracting(ClaimedEvent::eventId).contains(late);
            runtime.ackDone(claimed).require();
        }
        assertThat(count("SELECT count(*) FROM event WHERE status <> 'done'")).isZero();
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
