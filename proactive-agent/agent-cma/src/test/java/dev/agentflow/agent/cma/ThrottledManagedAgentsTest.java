package dev.agentflow.agent.cma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ThrottledManagedAgentsTest {

    private final AtomicLong now = new AtomicLong();
    private final FakeManagedAgents fake = new FakeManagedAgents();

    @Test
    void creationsHaveABucketOfTheirOwn_andWhatIsOverTheLimitFailsAsTheLimitWould() {
        // 60 a minute: a bucket of 10, one more every second.
        ThrottledManagedAgents throttled = new ThrottledManagedAgents(fake, 60, 600, Duration.ZERO, now::get);

        for (int i = 0; i < 10; i++) {
            throttled.createVault("v" + i, Map.of());
        }
        assertThat(throttled.saturated()).isTrue();
        CmaException refused = catchThrowableOfType(CmaException.class, () -> throttled.createVault("v10", Map.of()));
        assertThat(refused.kind()).isEqualTo(CmaException.Kind.RATE_LIMITED);
        assertThat(refused.retryAfter()).isEqualTo(Duration.ofSeconds(1));
        assertThat(throttled.listCredentials(fake.vaultIds().getFirst())).as("other requests have their own bucket").isEmpty();

        now.addAndGet(Duration.ofSeconds(1).toNanos());
        assertThat(throttled.saturated()).isFalse();
        throttled.createVault("v10", Map.of());
        assertThat(fake.vaultIds()).hasSize(11);
    }

    @Test
    void aCallWaitsForItsTurn_whenItsTurnComesSoonEnough() {
        ThrottledManagedAgents throttled = new ThrottledManagedAgents(fake, 600_000, 600, Duration.ofSeconds(1), System::nanoTime);
        long started = System.nanoTime();

        for (int i = 0; i < 101; i++) {
            throttled.createVault("v" + i, Map.of());
        }

        assertThat(fake.vaultIds()).hasSize(101);
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
        assertThat(List.of(throttled.saturated())).isNotNull();
    }
}
