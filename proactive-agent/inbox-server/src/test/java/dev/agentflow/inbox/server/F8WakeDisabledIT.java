package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * F8 acceptance check: with the wake channel switched off entirely, events are still processed within
 * ten seconds. Only latency suffers; nothing is lost.
 */
@TestPropertySource(properties = "inbox.wake.enabled=false")
class F8WakeDisabledIT extends IntegrationTestBase {

    @Test
    void withoutAnyWakeHint_aPollingClaimerStillGetsTheEventWithinTenSeconds() throws Exception {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<ApiResult<ClaimedRun>> waiting = CompletableFuture.supplyAsync(
                    () -> runtime.claim(ClaimRequest.any("worker-1").withWait(20_000)), pool);
            Thread.sleep(300);

            long writtenAt = System.nanoTime();
            String eventId = write("u42", webhook("order-1", "{}"));
            ClaimedRun run = waiting.get(15, TimeUnit.SECONDS).require();
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - writtenAt);

            assertThat(run.events().getFirst().eventId()).isEqualTo(eventId);
            assertThat(elapsed).as("found by polling").isLessThan(10_000);
            runtime.ackDone(run).require();
        }
    }
}
