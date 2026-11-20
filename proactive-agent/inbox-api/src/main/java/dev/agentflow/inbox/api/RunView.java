package dev.agentflow.inbox.api;

import java.time.Instant;
import java.util.List;

/** A run and the events bound to it, for the runtime and for operators. */
public record RunView(
        String runId,
        String tenantId,
        String inboxId,
        Lane lane,
        String mergeKey,
        String status,
        Integer slotNo,
        int epoch,
        int attempt,
        int maxAttempts,
        String leaseOwner,
        Instant leaseExpiresAt,
        Instant leaseDeadlineAt,
        Instant nextAttemptAt,
        String queuedReason,
        boolean acceptsSteer,
        String resumeKey,
        Instant suspendDeadlineAt,
        String onTimeout,
        String resumeReason,
        String traceId,
        String endReason,
        String lastError,
        Instant createdAt,
        Instant finishedAt,
        List<EventView> events) {
}
