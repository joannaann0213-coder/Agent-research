package dev.agentflow.inbox.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** An {@link Outbox} for a single process. */
public final class MemoryOutbox implements Outbox {

    private final Map<String, OutboxMessage> byKey = new ConcurrentHashMap<>();

    @Override
    public boolean send(OutboxMessage message) {
        return byKey.putIfAbsent(message.dedupeKey(), message) == null;
    }

    @Override
    public List<OutboxMessage> delivered() {
        return List.copyOf(byKey.values());
    }
}
