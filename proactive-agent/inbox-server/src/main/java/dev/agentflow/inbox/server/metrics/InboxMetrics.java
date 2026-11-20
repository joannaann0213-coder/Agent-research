package dev.agentflow.inbox.server.metrics;

import dev.agentflow.inbox.api.Lane;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * The service's meters. All carry {@code tenant} and {@code lane} so a problem can be traced to who
 * and where; none carries the inbox id, which would be unbounded.
 */
@Component
public class InboxMetrics {

    private final MeterRegistry registry;

    public InboxMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Write latency and outcome; the dedupe ratio is deduped / (accepted + deduped). */
    public void write(String result, String tenant, Lane lane, long nanos) {
        Timer.builder("inbox.write")
                .description("Time to accept an event")
                .tag("result", result)
                .tag("tenant", tag(tenant))
                .tag("lane", lane == null ? "none" : lane.wire())
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }

    /** Claim latency measured from the moment the batch became eligible (the service-level target). */
    public void claimLatency(String tenant, Lane lane, Duration sinceEligible, Duration sinceReady) {
        Timer.builder("inbox.claim.latency")
                .description("Eligible to claimed")
                .tag("tenant", tag(tenant)).tag("lane", lane.wire())
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(nonNegative(sinceEligible));
        Timer.builder("inbox.ready.to.claim")
                .description("ready_at to claimed, including the quiet window")
                .tag("tenant", tag(tenant)).tag("lane", lane.wire())
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(nonNegative(sinceReady));
    }

    public void batchSize(String tenant, Lane lane, int events) {
        DistributionSummary.builder("inbox.batch.size")
                .description("Events per run at claim time")
                .tag("tenant", tag(tenant)).tag("lane", lane.wire())
                .publishPercentiles(0.5, 0.95)
                .register(registry)
                .record(events);
    }

    public void leaseReclaimed(String tenant, Lane lane) {
        Counter.builder("inbox.lease.reclaim")
                .description("Runs taken back after their lease expired")
                .tag("tenant", tag(tenant)).tag("lane", lane.wire())
                .register(registry)
                .increment();
    }

    public void replayed(String tenant, boolean force, int events) {
        Counter.builder("inbox.replay")
                .description("Events replayed by an operator")
                .tag("tenant", tag(tenant)).tag("force", Boolean.toString(force))
                .register(registry)
                .increment(events);
    }

    public MeterRegistry registry() {
        return registry;
    }

    private static String tag(String value) {
        return value == null ? "none" : value;
    }

    private static Duration nonNegative(Duration duration) {
        return duration == null || duration.isNegative() ? Duration.ZERO : duration;
    }
}
