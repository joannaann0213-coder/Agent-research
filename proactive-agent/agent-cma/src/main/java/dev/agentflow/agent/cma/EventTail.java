package dev.agentflow.agent.cma;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Every event of a session from a point in time on, each one once and none missing.
 *
 * <p>A live stream alone cannot give that: it has no replay, so whatever happened before it was
 * opened, or while it was broken, is not on it. The tail therefore opens the stream first, then
 * reads the stored events, and hands out the stored ones before the live ones, leaving out what it
 * has handed out already. When the stream breaks it does the same again.
 *
 * <p>An event the caller sent can come twice with the same id, queued and then processed; both are
 * handed out, because they say different things. Not thread-safe.
 */
public final class EventTail implements AutoCloseable {

    private final ManagedAgents cma;
    private final String sessionId;
    private final Instant since;
    private final Set<String> seen = new HashSet<>();
    private final Deque<CmaEvent> backlog = new ArrayDeque<>();
    private EventStream stream;

    private EventTail(ManagedAgents cma, String sessionId, Instant since) {
        this.cma = cma;
        this.sessionId = sessionId;
        this.since = since;
    }

    /**
     * Opens the tail. When this returns the live stream is open, so an event sent afterwards cannot
     * be missed.
     *
     * @param since the tail starts with the stored events created at or after this instant;
     *     {@code null} starts at the session's first event
     */
    public static EventTail open(ManagedAgents cma, String sessionId, Instant since) {
        EventTail tail = new EventTail(cma, sessionId, since);
        tail.connect();
        return tail;
    }

    /**
     * The stored events that have not been handed out yet, all at once: right after {@link #open},
     * that is everything the session held from the starting point on. A caller that has to know what
     * happened before it acts reads this first; what {@link #poll} returns afterwards is live.
     */
    public List<CmaEvent> stored() {
        List<CmaEvent> events = List.copyOf(backlog);
        backlog.clear();
        return events;
    }

    /**
     * The next event, waiting up to {@code timeout} for one.
     *
     * @return empty when nothing arrived in time
     * @throws CmaException when the stream broke and could not be opened again
     */
    public Optional<CmaEvent> poll(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        boolean reconnected = false;
        while (true) {
            CmaEvent stored = backlog.poll();
            if (stored != null) {
                return Optional.of(stored);
            }
            Optional<CmaEvent> live;
            try {
                live = stream.poll(Duration.ofNanos(Math.max(0, deadline - System.nanoTime())));
            } catch (CmaException lost) {
                if (lost.kind() != CmaException.Kind.CONNECTION || reconnected) {
                    throw lost;
                }
                reconnected = true;
                stream.close();
                connect();
                continue;
            }
            if (live.isEmpty()) {
                return Optional.empty();
            }
            if (fresh(live.get())) {
                return live;
            }
        }
    }

    @Override
    public void close() {
        stream.close();
    }

    /** Stream first, then the stored events: what happens in between is on both and is handed out once. */
    private void connect() {
        stream = cma.openStream(sessionId);
        try {
            List<CmaEvent> stored = cma.listEvents(sessionId, since);
            for (CmaEvent event : stored) {
                if (fresh(event)) {
                    backlog.add(event);
                }
            }
        } catch (RuntimeException e) {
            stream.close();
            throw e;
        }
    }

    /** Whether the event has not been handed out yet. An event without an id cannot be recognised and always counts as new. */
    private boolean fresh(CmaEvent event) {
        if (event.id() == null || event.id().isEmpty()) {
            return true;
        }
        if (event.processedAt() == null) {
            // the queued form of an event is of no interest once its processed form has been handed out
            return !seen.contains(event.id()) && seen.add(event.id() + " queued");
        }
        return seen.add(event.id());
    }
}
