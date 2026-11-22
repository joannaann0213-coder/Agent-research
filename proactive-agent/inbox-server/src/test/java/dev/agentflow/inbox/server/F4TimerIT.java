package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.agentflow.inbox.api.AuditEntry;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.CreateTimerRequest;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.TimerView;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.support.MutableClock;
import dev.agentflow.inbox.server.timer.TimerScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import tools.jackson.databind.JsonNode;

/**
 * F4: readiness. The ready queue holds only what can run now. Anything in the future is a timer, and
 * a due timer becomes an event through the normal path, so it gets dedupe, single flight and replay.
 */
class F4TimerIT extends IntegrationTestBase {

    private static final Instant T0 = MutableClock.T0;

    @Autowired
    TimerScheduler scheduler;

    private WriteEventRequest followUp(Instant readyAt) {
        return WriteEventRequest.builder(EventSource.AGENT_SELF, "agent.followup.v1")
                .sourceEventId("run-7:followup-1")
                .readyAt(readyAt)
                .payloadJson("{\"remind\":\"check whether they replied\"}")
                .build();
    }

    // ---- deferred events ------------------------------------------------------------------------

    @Test
    void futureReadyAt_isOnlyATimer_andNeverOccupiesTheReadyQueue() {
        WriteEventResponse accepted = acme.writeEvent("u42", followUp(T0.plus(Duration.ofHours(2)))).require();
        assertThat(accepted.status()).isEqualTo("scheduled");
        assertThat(accepted.deduped()).isFalse();

        assertThat(count("SELECT count(*) FROM event")).as("nothing in the ready queue").isZero();
        assertThat(count("SELECT count(*) FROM timer WHERE state = 'scheduled'")).isEqualTo(1);
        assertThat(claim("worker-1", "u42").status()).isEqualTo(204);

        EventDetail scheduled = acme.getEvent("u42", accepted.eventId()).require();
        assertThat(scheduled.event().status()).isEqualTo("scheduled");
        assertThat(scheduled.event().readyAt()).isEqualTo(T0.plus(Duration.ofHours(2)));
        assertThat(scheduled.transitions()).isEmpty();

        // not yet
        clock.advance(Duration.ofMinutes(119));
        assertThat(scheduler.runOnce()).isZero();
        assertThat(count("SELECT count(*) FROM event")).isZero();

        // due: the timer becomes the event the agent wrote, under the id it was promised
        clock.advance(Duration.ofMinutes(1));
        assertThat(scheduler.runOnce()).isEqualTo(1);
        EventDetail fired = acme.getEvent("u42", accepted.eventId()).require();
        assertThat(fired.event().status()).isEqualTo("pending");
        assertThat(fired.event().source()).isEqualTo(EventSource.AGENT_SELF);
        assertThat(fired.event().type()).isEqualTo("agent.followup.v1");
        assertThat(fired.event().sourceEventId()).isEqualTo("run-7:followup-1");
        assertThat(fired.event().lane()).isEqualTo(Lane.BACKGROUND);
        assertThat(fired.event().readyAt()).isEqualTo(T0.plus(Duration.ofHours(2)));
        assertThat(fired.event().receivedAt()).isEqualTo(clock.now());
        assertThat(fired.event().timerId()).isNotNull();
        assertThat(fired.transitions()).singleElement()
                .satisfies(t -> assertThat(t.actor()).isEqualTo("system:timer-scheduler"));
        assertThat(count("SELECT count(*) FROM timer WHERE state = 'fired' AND fired_event_id IS NOT NULL")).isEqualTo(1);

        // from here on it is an ordinary event
        ClaimedRun run = claim("worker-1", "u42").require();
        assertThat(run.events()).singleElement().satisfies(e -> assertThat(e.eventId()).isEqualTo(accepted.eventId()));
        runtime.ackDone(run).require();

        assertThat(scheduler.runOnce()).as("a fired timer does not fire again").isZero();
    }

    @Test
    void aDeferredEventDedupesBeforeAndAfterItFires() {
        Instant readyAt = T0.plus(Duration.ofHours(1));
        WriteEventResponse first = acme.writeEvent("u42", followUp(readyAt)).require();

        WriteEventResponse retry = acme.writeEvent("u42", followUp(readyAt)).require();
        assertThat(retry.deduped()).isTrue();
        assertThat(retry.eventId()).isEqualTo(first.eventId());
        assertThat(retry.status()).isEqualTo("scheduled");
        assertThat(count("SELECT count(*) FROM timer")).isEqualTo(1);

        // the same key for a different follow-up is a producer bug, deferred or not
        ApiResult<WriteEventResponse> reused = acme.writeEvent("u42",
                WriteEventRequest.builder(EventSource.AGENT_SELF, "agent.followup.v1")
                        .sourceEventId("run-7:followup-1").readyAt(readyAt).payloadJson("{\"remind\":\"other\"}").build());
        assertThat(reused.status()).isEqualTo(409);
        assertThat(reused.code()).isEqualTo(ProblemCodes.IDEMPOTENCY_CONFLICT);

        // a retry that arrives after the due time but before the scheduler ran is still the same event
        clock.advance(Duration.ofMinutes(90));
        WriteEventResponse lateRetry = acme.writeEvent("u42", followUp(readyAt)).require();
        assertThat(lateRetry.deduped()).isTrue();
        assertThat(lateRetry.eventId()).isEqualTo(first.eventId());
        assertThat(count("SELECT count(*) FROM event")).isZero();

        assertThat(scheduler.runOnce()).isEqualTo(1);
        WriteEventResponse afterFiring = acme.writeEvent("u42", followUp(readyAt)).require();
        assertThat(afterFiring.deduped()).isTrue();
        assertThat(afterFiring.eventId()).isEqualTo(first.eventId());
        assertThat(afterFiring.status()).isEqualTo("pending");
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
    }

    @Test
    void aReadyAtInThePastIsJustAnEvent() {
        WriteEventResponse accepted = acme.writeEvent("u42", followUp(T0.minusSeconds(30))).require();
        assertThat(accepted.status()).isEqualTo("pending");
        assertThat(count("SELECT count(*) FROM timer")).isZero();
        assertThat(acme.getEvent("u42", accepted.eventId()).require().event().readyAt()).isEqualTo(T0);
    }

    // ---- firing exactly once --------------------------------------------------------------------

    @Test
    void severalSchedulersAtOnce_everyTimerFiresExactlyOnce() throws Exception {
        int timers = 30;
        for (int i = 0; i < timers; i++) {
            acme.createTimer("u" + (i % 5), CreateTimerRequest.once(T0.plusSeconds(60), "{\"n\":" + i + "}")).require();
        }
        clock.advance(Duration.ofSeconds(61));

        int schedulers = 4;
        CyclicBarrier start = new CyclicBarrier(schedulers);
        int fired = 0;
        try (ExecutorService pool = Executors.newFixedThreadPool(schedulers)) {
            List<Future<Integer>> passes = new ArrayList<>();
            for (int s = 0; s < schedulers; s++) {
                passes.add(pool.submit(() -> {
                    start.await();
                    return scheduler.runOnce();
                }));
            }
            for (Future<Integer> pass : passes) {
                fired += pass.get();
            }
        }
        fired += scheduler.runOnce(); // anything a pass skipped because another pass held the row

        assertThat(fired).isEqualTo(timers);
        assertThat(count("SELECT count(*) FROM event WHERE type = 'timer.due'")).isEqualTo(timers);
        assertThat(count("SELECT count(DISTINCT timer_id) FROM event")).isEqualTo(timers);
        assertThat(count("SELECT count(*) FROM timer WHERE state = 'fired'")).isEqualTo(timers);
    }

    @Test
    void aFailureWhileFiringRollsBack_theTimerFiresOnceLater() {
        String timerId = acme.createTimer("u42", CreateTimerRequest.once(T0.plusSeconds(60), "{}")).require().timerId();
        clock.advance(Duration.ofSeconds(61));

        jdbc.sql("ALTER TABLE event RENAME TO event_offline").update();
        try {
            assertThatThrownBy(() -> scheduler.runOnce()).isInstanceOf(DataAccessException.class);
        } finally {
            jdbc.sql("ALTER TABLE event_offline RENAME TO event").update();
        }
        assertThat(acme.getTimer("u42", timerId).require().state()).as("still scheduled, nothing half done")
                .isEqualTo("scheduled");
        assertThat(count("SELECT count(*) FROM event")).isZero();

        assertThat(scheduler.runOnce()).isEqualTo(1);
        assertThat(scheduler.runOnce()).isZero();
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
    }

    @Test
    void aTimerThatCameDueWhileTheSchedulerWasDownFiresOnRestart() {
        acme.createTimer("u42", CreateTimerRequest.once(T0.plus(Duration.ofHours(1)), "{}")).require();

        clock.advance(Duration.ofHours(3)); // the scheduler was not running for three hours
        assertThat(scheduler.runOnce()).isEqualTo(1);
        assertThat(scheduler.runOnce()).isZero();
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
    }

    // ---- timers made through the API ------------------------------------------------------------

    @Test
    void aOneOffTimerProducesATimerDueEvent() {
        TimerView timer = acme.createTimer("u42", CreateTimerRequest.once(T0.plusSeconds(300), "{\"job\":\"inspect\"}")
                .withMergeKey("job:inspect")).require();
        assertThat(timer.state()).isEqualTo("scheduled");
        assertThat(timer.lane()).isEqualTo(Lane.BACKGROUND);

        clock.advance(Duration.ofSeconds(300));
        assertThat(scheduler.runOnce()).isEqualTo(1);

        TimerView fired = acme.getTimer("u42", timer.timerId()).require();
        assertThat(fired.state()).isEqualTo("fired");
        assertThat(fired.firedAt()).isEqualTo(clock.now());
        EventDetail event = acme.getEvent("u42", fired.firedEventId()).require();
        assertThat(event.event().source()).isEqualTo(EventSource.TIMER);
        assertThat(event.event().type()).isEqualTo("timer.due");
        assertThat(event.event().mergeKey()).isEqualTo("job:inspect");
        assertThat(event.event().sourceEventId()).isEqualTo("timer:" + timer.timerId() + ":" + T0.plusSeconds(300).getEpochSecond());
        JsonNode payload = event.event().payload();
        assertThat(payload.get("timer_id").stringValue()).isEqualTo(timer.timerId());
        assertThat(payload.get("catch_up").booleanValue()).isFalse();
        assertThat(payload.get("payload").get("job").stringValue()).isEqualTo("inspect");
    }

    @Test
    void aCronTimerSchedulesItsNextOccurrenceEachTimeItFires() {
        // every day at 9 am in Shanghai, which is 01:00 UTC
        TimerView timer = acme.createTimer("u42", CreateTimerRequest.cron("0 9 * * *", "Asia/Shanghai",
                "{\"job\":\"daily-report\"}")).require();
        assertThat(timer.fireAt()).isEqualTo(Instant.parse("2026-11-21T01:00:00Z"));
        assertThat(timer.timeZone()).isEqualTo("Asia/Shanghai");

        clock.set(Instant.parse("2026-11-21T01:00:00Z"));
        assertThat(scheduler.runOnce()).isEqualTo(1);
        TimerView next = acme.getTimer("u42", timer.timerId()).require();
        assertThat(next.state()).isEqualTo("scheduled");
        assertThat(next.fireAt()).isEqualTo(Instant.parse("2026-11-22T01:00:00Z"));
        assertThat(count("SELECT count(*) FROM timer WHERE timer_id = '" + timer.timerId() + "'")).isEqualTo(2);

        clock.set(Instant.parse("2026-11-22T01:00:05Z"));
        assertThat(scheduler.runOnce()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE type = 'timer.due'")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM event WHERE lane = 'background'")).isEqualTo(2);
    }

    @Test
    void afterAnOutage_aCronFiresOnlyItsLatestMissedOccurrence_onTheBatchLane() {
        TimerView timer = acme.createTimer("u42", CreateTimerRequest.cron("0 * * * *", null, "{\"job\":\"hourly\"}")).require();
        assertThat(timer.fireAt()).isEqualTo(Instant.parse("2026-11-20T11:00:00Z"));

        // the scheduler is down from before 11:00 until 17:20: seven occurrences came due
        clock.set(Instant.parse("2026-11-20T17:20:00Z"));
        assertThat(scheduler.runOnce()).isEqualTo(1);
        assertThat(scheduler.runOnce()).isZero();

        assertThat(count("SELECT count(*) FROM event")).as("one report, not seven").isEqualTo(1);
        JsonNode payload = jdbc.sql("SELECT payload::text FROM event").query(String.class)
                .single().transform(acme.json()::readTree);
        assertThat(payload.get("fire_at").stringValue()).isEqualTo("2026-11-20T17:00:00Z");
        assertThat(payload.get("catch_up").booleanValue()).isTrue();
        assertThat(payload.get("missed_count").intValue()).isEqualTo(6);
        assertThat(count("SELECT count(*) FROM event WHERE lane = 'batch'")).as("catch-up goes through the batch lane").isEqualTo(1);
        assertThat(count("SELECT count(*) FROM timer WHERE state = 'missed'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM timer WHERE state = 'fired' AND catch_up")).isEqualTo(1);

        TimerView next = acme.getTimer("u42", timer.timerId()).require();
        assertThat(next.state()).isEqualTo("scheduled");
        assertThat(next.fireAt()).isEqualTo(Instant.parse("2026-11-20T18:00:00Z"));

        // back to normal: on time, on its own lane
        clock.set(Instant.parse("2026-11-20T18:00:00Z"));
        assertThat(scheduler.runOnce()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE lane = 'background'")).isEqualTo(1);
    }

    @Test
    void timerKeyMakesCreationIdempotent() {
        CreateTimerRequest request = CreateTimerRequest.cron("0 9 * * *", "UTC", "{}").withTimerKey("daily-report");
        ApiResult<TimerView> first = acme.createTimer("u42", request);
        ApiResult<TimerView> second = acme.createTimer("u42", request);

        assertThat(first.status()).isEqualTo(201);
        assertThat(second.status()).isEqualTo(200);
        assertThat(second.body().timerId()).isEqualTo(first.body().timerId());
        assertThat(count("SELECT count(*) FROM timer")).isEqualTo(1);
        // the key is free again in another inbox
        assertThat(acme.createTimer("u43", request).status()).isEqualTo(201);
    }

    @Test
    void aCancelledTimerDoesNotFire() {
        String timerId = acme.createTimer("u42", CreateTimerRequest.once(T0.plusSeconds(60), "{}")).require().timerId();
        assertThat(acme.cancelTimer("u42", timerId).status()).isEqualTo(204);
        assertThat(acme.cancelTimer("u42", timerId).status()).as("nothing scheduled any more").isEqualTo(404);

        clock.advance(Duration.ofHours(1));
        assertThat(scheduler.runOnce()).isZero();
        assertThat(acme.getTimer("u42", timerId).require().state()).isEqualTo("cancelled");
        assertThat(count("SELECT count(*) FROM event")).isZero();
    }

    @Test
    void aProducerSeesAndCancelsOnlyItsOwnTimers() {
        String timerId = acme.createTimer("u42", CreateTimerRequest.once(T0.plusSeconds(60), "{}")).require().timerId();

        assertThat(acme2.getTimer("u42", timerId).status()).as("another producer of the same tenant").isEqualTo(404);
        assertThat(acme2.cancelTimer("u42", timerId).status()).isEqualTo(404);
        assertThat(acme.getTimer("u42", timerId).require().state()).isEqualTo("scheduled");

        assertThat(acme.cancelTimer("u42", timerId).status()).isEqualTo(204);
        assertThat(count("SELECT count(*) FROM admin_audit")).as("a producer cancelling its own timer is not an operator action")
                .isZero();
    }

    @Test
    void anOperatorSeesEveryTimerOfTheTenant_andItsCancelIsAudited() {
        String timerId = acme.createTimer("u42", CreateTimerRequest.once(T0.plusSeconds(60), "{}")).require().timerId();
        String path = "/v1/inbox/u42/timers/" + timerId + "?tenant_id=acme";

        assertThat(ops.get(path, TimerView.class).require().state()).isEqualTo("scheduled");
        assertThat(ops.delete(path, Void.class).status()).isEqualTo(204);
        assertThat(acme.getTimer("u42", timerId).require().state()).isEqualTo("cancelled");

        AuditEntry[] audit = ops.audit(ACME, 10).require();
        assertThat(audit).hasSize(1);
        assertThat(audit[0].action()).isEqualTo("timer_cancel");
        assertThat(audit[0].actorClientId()).isEqualTo("ops");
        assertThat(audit[0].target().get("timer_id").stringValue()).isEqualTo(timerId);

        clock.advance(Duration.ofHours(1));
        assertThat(scheduler.runOnce()).isZero();
    }

    @Test
    void invalidTimersAreRefused_andOtherTenantsCannotSeeThem() {
        assertThat(acme.createTimer("u42", CreateTimerRequest.cron("*/10 * * * * *", null, "{}")).status())
                .as("more often than once a minute").isEqualTo(400);
        assertThat(acme.createTimer("u42", CreateTimerRequest.cron("whenever", null, "{}")).status()).isEqualTo(400);
        assertThat(acme.createTimer("u42", CreateTimerRequest.cron("0 9 * * *", "Mars/Olympus", "{}")).status()).isEqualTo(400);
        assertThat(acme.createTimer("u42", new CreateTimerRequest("once", null, null, null, null, null, null, null))
                .status()).isEqualTo(400);
        assertThat(acme.createTimer("u42", new CreateTimerRequest("sometimes", T0, null, null, null, null, null, null))
                .status()).isEqualTo(400);
        String oversized = "{\"blob\":\"" + "x".repeat(33 * 1024) + "\"}";
        assertThat(acme.createTimer("u42", CreateTimerRequest.once(T0.plusSeconds(60), oversized)).status())
                .as("a timer carries at most 32 KB").isEqualTo(413);
        assertThat(count("SELECT count(*) FROM timer")).isZero();

        String timerId = acme.createTimer("u42", CreateTimerRequest.once(T0.plusSeconds(60), "{}")).require().timerId();
        assertThat(globex.getTimer("u42", timerId).status()).isEqualTo(404);
        assertThat(globex.cancelTimer("u42", timerId).status()).isEqualTo(404);
        assertThat(runtime.get("/v1/inbox/u42/timers/" + timerId + "?tenant_id=acme", TimerView.class).status())
                .as("the runtime has no business with timers").isEqualTo(403);
    }
}
