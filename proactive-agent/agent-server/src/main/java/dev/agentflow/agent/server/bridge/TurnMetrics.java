package dev.agentflow.agent.server.bridge;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * The meters of turns: how many of which kind ended how, how long they took, what they used, and
 * how long users waited for a reply. They carry the kind of the turn and its outcome, and neither
 * an application nor a user, which would be unbounded; the numbers per user and per application
 * are read from the service's records instead ({@code GET /v1/stats}).
 */
@Component
public class TurnMetrics {

    private final MeterRegistry registry;

    public TurnMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * A turn has ended.
     *
     * @param kind chat, background or worker; {@code null} for a turn that ended before it knew
     * @param outcome how it ended, or {@code failed}
     * @param used what its requests to the model used, over all its parts
     * @param waitedMs for a reply: how long the user waited for it
     */
    public void ended(String kind, String outcome, Duration took, Usage used, Long waitedMs) {
        String of = kind == null ? "none" : kind;
        registry.counter("agent.turns", "kind", of, "outcome", outcome).increment();
        Timer.builder("agent.turn.duration").description("From the first delivery of a turn's run to its end")
                .tag("kind", of).publishPercentiles(0.5, 0.95).register(registry)
                .record(took.isNegative() ? Duration.ZERO : took);
        registry.counter("agent.turn.model.requests", "kind", of).increment(used.requests());
        registry.counter("agent.turn.tokens", "kind", of, "type", "input").increment(used.inputTokens());
        registry.counter("agent.turn.tokens", "kind", of, "type", "output").increment(used.outputTokens());
        registry.counter("agent.turn.tokens", "kind", of, "type", "cache_read").increment(used.cacheReadTokens());
        registry.counter("agent.turn.tokens", "kind", of, "type", "cache_creation").increment(used.cacheCreationTokens());
        if (waitedMs != null) {
            Timer.builder("agent.reply.wait").description("From a user's oldest unanswered message to the reply")
                    .publishPercentiles(0.5, 0.95).register(registry).record(Duration.ofMillis(Math.max(0, waitedMs)));
        }
    }

    /** A turn in the background has given way to a message of the user. */
    public void gaveWay() {
        registry.counter("agent.turn.yields").increment();
    }
}
