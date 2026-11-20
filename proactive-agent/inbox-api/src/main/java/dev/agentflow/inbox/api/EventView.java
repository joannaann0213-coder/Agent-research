package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** An event as stored, with its current delivery state. */
public record EventView(
        String eventId,
        String tenantId,
        String inboxId,
        Long seq,
        String clientId,
        EventSource source,
        String sourceEventId,
        String dedupeKind,
        String type,
        JsonNode payload,
        Instant occurredAt,
        Instant receivedAt,
        Instant readyAt,
        String mergeKey,
        Lane lane,
        String resumeKey,
        boolean interrupt,
        String traceId,
        String timerId,
        String status,
        int attempt,
        int maxAttempts,
        String runId,
        Integer runSeq,
        String deliveryMode,
        Instant firstClaimedAt,
        Instant finishedAt,
        String lastError) {
}
