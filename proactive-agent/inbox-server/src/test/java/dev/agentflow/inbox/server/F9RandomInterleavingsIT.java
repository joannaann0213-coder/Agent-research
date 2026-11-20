package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.SteerRequest;
import dev.agentflow.inbox.api.SteerResponse;
import dev.agentflow.inbox.api.SuspendRequest;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.support.InvariantChecker;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import dev.agentflow.inbox.server.sweep.SuspendSweeper;
import dev.agentflow.inbox.server.timer.TimerScheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The property behind F9's "no event is ever lost": whatever order writes, claims, steers, acks,
 * nacks, suspends, crashes, timeouts and sweeps happen in, every accepted event reaches a final state
 * exactly once, and the tables are consistent after every single step.
 *
 * <p>Each seed is one reproducible interleaving on one inbox. A failure message names the seed and step.
 */
class F9RandomInterleavingsIT extends IntegrationTestBase {

    private static final String INBOX = "u42";
    private static final int STEPS = 160;
    private static final String[] KEYS = {null, "conversation:a", "order:b"};

    @Autowired
    LeaseSweeper leaseSweeper;

    @Autowired
    SuspendSweeper suspendSweeper;

    @Autowired
    TimerScheduler timerScheduler;

    /** A run this test's simulated worker believes it holds (it may have lost it without noticing). */
    private static final class Held {
        final String runId;
        final int epoch;
        int lastSeen;

        Held(String runId, int epoch, int lastSeen) {
            this.runId = runId;
            this.epoch = epoch;
            this.lastSeen = lastSeen;
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void everyAcceptedEventEndsExactlyOnce_whateverTheInterleaving(long seed) {
        Random random = new Random(seed);
        List<Held> held = new ArrayList<>();
        List<String> resumeKeys = new ArrayList<>();
        int written = 0;
        int resumeKeyCounter = 0;

        for (int step = 0; step < STEPS; step++) {
            String where = "seed " + seed + ", step " + step;
            int dice = random.nextInt(100);
            if (dice < 28) {
                written += write(random, resumeKeys, "e" + step) ? 1 : 0;
            } else if (dice < 48) {
                ApiResult<ClaimedRun> claimed = runtime.claim(ClaimRequest.inbox("worker-" + step, ACME, INBOX)
                        .withSteer(random.nextBoolean()));
                assertThat(claimed.status()).as(where).isIn(200, 204);
                if (claimed.status() == 200) {
                    held.add(new Held(claimed.body().runId(), claimed.body().epoch(), claimed.body().lastRunSeq()));
                }
            } else if (dice < 56 && !held.isEmpty()) {
                Held run = pick(random, held);
                ApiResult<SteerResponse> steered = runtime.steer(run.runId, new SteerRequest(run.epoch, run.lastSeen));
                assertThat(steered.status()).as(where).isIn(200, 409);
                if (steered.status() == 200) {
                    run.lastSeen = steered.body().lastRunSeq(run.lastSeen);
                } else {
                    held.remove(run);
                }
            } else if (dice < 70 && !held.isEmpty()) {
                Held run = pick(random, held);
                int cursor = random.nextInt(5) == 0 ? random.nextInt(run.lastSeen + 1) : run.lastSeen;
                AckRequest ack = random.nextInt(7) == 0
                        ? AckRequest.failed(run.epoch, cursor, "simulated final failure")
                        : AckRequest.done(run.epoch, cursor);
                assertThat(runtime.ack(run.runId, ack).status()).as(where).isIn(200, 409);
                held.remove(run);
            } else if (dice < 76 && !held.isEmpty()) {
                Held run = pick(random, held);
                Long retryAfter = random.nextBoolean() ? null : (long) random.nextInt(20_000);
                assertThat(runtime.nack(run.runId, new NackRequest(run.epoch, "simulated failure", retryAfter)).status())
                        .as(where).isIn(200, 409);
                held.remove(run);
            } else if (dice < 82 && !held.isEmpty()) {
                Held run = pick(random, held);
                String key = "resume-" + seed + "-" + resumeKeyCounter++;
                ApiResult<RunOutcome> suspended = runtime.suspend(run.runId, new SuspendRequest(run.epoch, key,
                        5L + random.nextInt(55), random.nextBoolean() ? SuspendRequest.RESUME : SuspendRequest.FINISH,
                        random.nextInt(4) == 0 ? random.nextInt(run.lastSeen + 1) : run.lastSeen));
                assertThat(suspended.status()).as(where).isIn(200, 409);
                if (suspended.status() == 200) {
                    resumeKeys.add(key);
                }
                held.remove(run);
            } else if (dice < 87 && !held.isEmpty()) {
                Held run = pick(random, held);
                int status = runtime.heartbeat(run.runId, new HeartbeatRequest(run.epoch, random.nextBoolean())).status();
                assertThat(status).as(where).isIn(200, 409);
                if (status == 409) {
                    held.remove(run);
                }
            } else if (dice < 95) {
                clock.advance(Duration.ofMillis(100 + random.nextInt(45_000)));
            } else {
                sweep();
            }
            try {
                InvariantChecker.assertAll(jdbc);
            } catch (AssertionError e) {
                throw new AssertionError(where + ": " + e.getMessage(), e);
            }
        }

        drain("seed " + seed);

        assertThat(count("SELECT count(*) FROM event")).as("seed %d: every accepted event exists", seed).isEqualTo(written);
        assertThat(count("SELECT count(*) FROM event WHERE status NOT IN ('done', 'failed', 'dead')"))
                .as("seed %d: every event reached a final state", seed).isZero();
        assertThat(count("""
                SELECT count(*) FROM (
                    SELECT event_id FROM event_transition
                     WHERE to_status IN ('done', 'failed', 'dead')
                     GROUP BY event_id HAVING count(*) <> 1) more_than_once
                """)).as("seed %d: and reached it exactly once", seed).isZero();
        assertThat(count("SELECT count(*) FROM run WHERE status IN ('active', 'queued', 'suspended')"))
                .as("seed %d: no run is left open", seed).isZero();
    }

    /** Writes one event; returns true when it was accepted as new. */
    private boolean write(Random random, List<String> resumeKeys, String id) {
        int laneDice = random.nextInt(10);
        Lane lane = laneDice < 7 ? Lane.INTERACTIVE : laneDice < 9 ? Lane.BACKGROUND : Lane.BATCH;
        WriteEventRequest.Builder event = WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "random.v1")
                .sourceEventId(id)
                .mergeKey(KEYS[random.nextInt(KEYS.length)])
                .lane(lane)
                .payloadJson("{\"id\":\"" + id + "\"}");
        if (random.nextInt(12) == 0) {
            event.interrupt(true);
        }
        if (!resumeKeys.isEmpty() && random.nextInt(3) == 0) {
            event.resumeKey(resumeKeys.get(random.nextInt(resumeKeys.size())));
        }
        if (random.nextInt(15) == 0) {
            event.readyAt(clock.now().plusSeconds(1 + random.nextInt(30))); // deferred: a timer first
        }
        return !acme.writeEvent(INBOX, event.build()).require().deduped();
    }

    private void sweep() {
        timerScheduler.runOnce();
        leaseSweeper.runOnce();
        suspendSweeper.runOnce();
    }

    /** The simulated workers are gone. Let time pass and a well-behaved worker finish everything. */
    private void drain(String where) {
        for (int round = 0; round < 60; round++) {
            clock.advance(Duration.ofSeconds(90));
            sweep();
            while (true) {
                ApiResult<ClaimedRun> claimed = runtime.claim(ClaimRequest.inbox("drain", ACME, INBOX));
                if (claimed.status() == 204) {
                    break;
                }
                runtime.ackDone(claimed.require()).require();
            }
            InvariantChecker.assertAll(jdbc);
            boolean settled = count("SELECT count(*) FROM event WHERE status NOT IN ('done', 'failed', 'dead')") == 0
                    && count("SELECT count(*) FROM run WHERE status IN ('active', 'queued', 'suspended')") == 0
                    && count("SELECT count(*) FROM timer WHERE state = 'scheduled'") == 0;
            if (settled) {
                return;
            }
        }
        throw new AssertionError(where + ": the inbox did not settle");
    }

    private static Held pick(Random random, List<Held> held) {
        return held.get(random.nextInt(held.size()));
    }
}
