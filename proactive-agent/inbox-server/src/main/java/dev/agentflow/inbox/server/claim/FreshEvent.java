package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import java.time.Instant;

/** An accepted event that no run owns yet, reduced to what planning needs (no payload). */
public record FreshEvent(String eventId, long seq, Lane lane, String mergeKey, String resumeKey, boolean interrupt,
        int payloadBytes, Instant receivedAt) {
}
