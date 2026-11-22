package dev.agentflow.agent.server.auth;

import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.time.AgentClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Token buckets per application, for its API keys, and per user, for user tokens: a caller may make
 * a burst of requests, and then as many as the bucket refills. Administrators are not limited. The
 * buckets are per instance of the service; behind a balancer the limit is that many times higher.
 */
@Component
public class RateLimiter {

    private static final int MAX_BUCKETS = 100_000;

    private static final class Bucket {
        private double tokens;
        private Instant at;

        Bucket(double tokens, Instant at) {
            this.tokens = tokens;
            this.at = at;
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AgentClock clock;
    private final AgentProperties.Limits limits;

    public RateLimiter(AgentClock clock, AgentProperties properties) {
        this.clock = clock;
        this.limits = properties.getLimits();
    }

    /** @return empty when the request may go ahead; otherwise how long until it may */
    public Optional<Duration> take(Caller caller) {
        if (caller.kind() == Caller.Kind.ADMIN) {
            return Optional.empty();
        }
        boolean user = caller.kind() == Caller.Kind.USER;
        double rate = user ? limits.getUserPerSecond() : limits.getAppPerSecond();
        double burst = user ? limits.getUserBurst() : limits.getAppBurst();
        String key = user ? "user:" + caller.appId() + "/" + caller.userId() : "app:" + caller.appId();
        Instant now = clock.now();
        if (buckets.size() > MAX_BUCKETS) {
            // Full buckets of callers that stopped calling hold nothing worth keeping.
            buckets.entrySet().removeIf(entry -> Duration.between(entry.getValue().at, now).toMinutes() > 10);
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(burst, now));
        synchronized (bucket) {
            double elapsed = Math.max(0, Duration.between(bucket.at, now).toNanos() / 1e9);
            bucket.tokens = Math.min(burst, bucket.tokens + elapsed * rate);
            bucket.at = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return Optional.empty();
            }
            return Optional.of(Duration.ofMillis((long) Math.ceil((1 - bucket.tokens) / rate * 1000)));
        }
    }

    /** Forgets every bucket. For tests. */
    public void reset() {
        buckets.clear();
    }
}
