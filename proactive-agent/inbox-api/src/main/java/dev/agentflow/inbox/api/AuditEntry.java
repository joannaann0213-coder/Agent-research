package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** One operator action: who did what to what, with which parameters and what came of it. */
public record AuditEntry(String auditId, String tenantId, String actorClientId, String operator, String action,
        JsonNode target, JsonNode params, JsonNode result, Instant at) {
}
