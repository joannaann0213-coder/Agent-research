package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** One row of an event's append-only history: who moved it, when, from where to where, and why. */
public record TransitionView(
        String fromStatus,
        String toStatus,
        String reason,
        String actor,
        String runId,
        Integer attempt,
        JsonNode detail,
        Instant at) {
}
