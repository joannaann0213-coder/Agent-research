package dev.agentflow.inbox.api;

/** Body of {@code POST /v1/admin/events/{event_id}/dead}: take a waiting event out of the queue by hand. */
public record DeadLetterRequest(String tenantId, String reason, String operator) {
}
