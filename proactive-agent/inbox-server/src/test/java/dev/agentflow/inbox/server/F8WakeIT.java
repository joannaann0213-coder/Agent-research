package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * F8: wake and claim. A claimer may wait for work (long-poll); a write wakes it. The hint is only an
 * optimisation: correctness depends on the tables alone. A claim returns a lease that heartbeats renew.
 */
class F8WakeIT extends IntegrationTestBase {

    @Autowired
    LeaseSweeper leaseSweeper;

    @Test
    void aWaitingClaimIsWokenByAWrite_wellBeforeItsPollInterval() throws Exception {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<ApiResult<ClaimedRun>> waiting = CompletableFuture.supplyAsync(
                    () -> runtime.claim(ClaimRequest.inbox("worker-1", ACME, "u42").withWait(20_000)), pool);
            Thread.sleep(300); // the claimer is parked: there is nothing to claim
            assertThat(waiting).isNotDone();

            long writtenAt = System.nanoTime();
            write("u42", webhook("order-1", "{}"));
            ClaimedRun run = waiting.get(10, TimeUnit.SECONDS).require();
            long wokenAfterMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - writtenAt);

            assertThat(wokenAfterMillis).as("woken by the hint, not by the 2 s poll").isLessThan(1000);
            runtime.ackDone(run).require();
        }
    }

    @Test
    void noWakeUpIsLostWhenAWriteRacesWithAClaimerGoingToSleep() throws Exception {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int round = 0; round < 60; round++) {
                String inbox = "race-" + round;
                CompletableFuture<ApiResult<ClaimedRun>> claimer = CompletableFuture.supplyAsync(
                        () -> runtime.claim(ClaimRequest.inbox("worker-1", ACME, inbox).withWait(20_000)), pool);
                CompletableFuture<String> writer = CompletableFuture.supplyAsync(
                        () -> write(inbox, webhook("order-1", "{}")), pool);
                long started = System.nanoTime();
                ClaimedRun run = claimer.get(10, TimeUnit.SECONDS).require();
                assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started))
                        .as("round %d", round).isLessThan(1500);
                assertThat(run.events().getFirst().eventId()).isEqualTo(writer.get());
                runtime.ackDone(run).require();
            }
        }
    }

    @Test
    void aClaimThatFindsNothingReturns204WhenItsWaitIsOver() {
        long started = System.nanoTime();
        ApiResult<ClaimedRun> result = runtime.claim(ClaimRequest.inbox("worker-1", ACME, "u42").withWait(400));
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(result.status()).isEqualTo(204);
        assertThat(elapsed).isBetween(350L, 3000L);
    }

    @Test
    void heartbeatsRenewOnlyUpToTheLeaseCap_thenTheRunIsTakenBack() {
        write("u42", webhook("endless", "{}"));
        ClaimedRun run = claim("worker-1", "u42").require();
        Instant deadline = run.leaseDeadlineAt();
        assertThat(deadline).isEqualTo(clock.now().plus(Duration.ofMinutes(30)));

        HeartbeatResponse last = null;
        while (clock.now().plusSeconds(50).isBefore(deadline)) {
            clock.advance(Duration.ofSeconds(50));
            last = runtime.heartbeat(run.runId(), new HeartbeatRequest(run.epoch(), null)).require();
            assertThat(leaseSweeper.runOnce()).isZero();
        }
        // the last heartbeat could not get a full minute any more
        assertThat(last.extended()).isFalse();
        assertThat(last.leaseExpiresAt()).isEqualTo(deadline);

        clock.set(deadline.plusSeconds(1));
        assertThat(runtime.heartbeat(run.runId(), new HeartbeatRequest(run.epoch(), null)).require().leaseExpiresAt())
                .as("a heartbeat cannot push the lease past its deadline").isEqualTo(deadline);
        assertThat(leaseSweeper.runOnce()).isEqualTo(1);

        ApiResult<HeartbeatResponse> afterReclaim = runtime.heartbeat(run.runId(), new HeartbeatRequest(run.epoch(), null));
        assertThat(afterReclaim.status()).isEqualTo(409);
        assertThat(afterReclaim.code()).isEqualTo(ProblemCodes.LEASE_LOST);

        ClaimedRun again = claim("worker-2", "u42").require();
        assertThat(again.runId()).isEqualTo(run.runId());
        assertThat(again.leaseDeadlineAt()).as("a new grant has a new deadline")
                .isEqualTo(clock.now().plus(Duration.ofMinutes(30)));
        runtime.ackDone(again).require();
    }

    @Test
    void heartbeatOfAnUnknownRunIs404() {
        assertThat(runtime.heartbeat("01NOSUCHRUN000000000000000", new HeartbeatRequest(1, null)).status()).isEqualTo(404);
    }
}
