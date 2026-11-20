package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * An event as handed to the runtime inside a run.
 *
 * @param runSeq position of the event inside its run, starting at 1; the cursor used by ack and suspend
 * @param deliveryMode how it joined the run: {@code batch} (at claim), {@code steer} or {@code resume}
 */
public record ClaimedEvent(
        String eventId,
        long seq,
        int runSeq,
        String deliveryMode,
        EventSource source,
        String type,
        String sourceEventId,
        Instant occurredAt,
        Instant receivedAt,
        String mergeKey,
        String resumeKey,
        boolean interrupt,
        JsonNode payload,
        String traceId) {
}
