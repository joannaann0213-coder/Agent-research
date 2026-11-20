package dev.agentflow.inbox.server.write;

import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Lane;
import java.time.Instant;

/**
 * Everything needed to append one event. Built by the write API for an immediate event and by the
 * timer scheduler when a timer fires; both go through the same statement.
 */
public record AppendCommand(
        String tenantId,
        String inboxId,
        String eventId,
        String clientId,
        EventSource source,
        String sourceEventId,
        String dedupeKind,
        String type,
        String payloadJson,
        byte[] payloadHash,
        int payloadBytes,
        Instant occurredAt,
        Instant readyAt,
        String mergeKey,
        Lane lane,
        String resumeKey,
        boolean interrupt,
        String traceId,
        String timerId,
        String actor,
        int maxAttempts,
        /* false when the scheduler fires a timer: its own row must not count as a duplicate */
        boolean checkDeferred) {
}
