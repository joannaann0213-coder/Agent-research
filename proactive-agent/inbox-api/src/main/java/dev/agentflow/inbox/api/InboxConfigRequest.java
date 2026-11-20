package dev.agentflow.inbox.api;

import tools.jackson.databind.JsonNode;

/**
 * Body of {@code PUT /v1/admin/inboxes/{inbox_id}/config}. Every field replaces the stored value; an
 * absent field goes back to the service default.
 *
 * @param maxActiveRuns inbox-wide cap on concurrent runs; 1 gives strict "one run per inbox"
 * @param settings per-lane {@code quiet_window_ms} and {@code max_wait_ms}, and batch caps, for example
 *        {@code {"lanes":{"background":{"quiet_window_ms":30000}},"batch":{"max_events":20}}}
 */
public record InboxConfigRequest(String tenantId, Integer capInteractive, Integer capBackground, Integer capBatch,
        Integer maxActiveRuns, JsonNode settings, String operator) {
}
