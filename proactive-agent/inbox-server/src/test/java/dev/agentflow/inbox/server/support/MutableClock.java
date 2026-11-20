package dev.agentflow.inbox.server.support;

import dev.agentflow.inbox.server.time.InboxClock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/** A clock that only moves when a test moves it. */
public class MutableClock implements InboxClock {

    public static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");

    private final AtomicReference<Instant> now = new AtomicReference<>(T0);

    @Override
    public Instant now() {
        return now.get();
    }

    public void set(Instant instant) {
        now.set(instant);
    }

    public Instant advance(Duration duration) {
        return now.updateAndGet(current -> current.plus(duration));
    }

    public void reset() {
        now.set(T0);
    }
}
