package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * The receipt for an event. It says the event is stored and will be handled, not that it has been,
 * and not that the user will be told: that is the agent's decision.
 *
 * @param deliverAt when the event will be handled at the earliest; absent when that is at once
 * @param deduped true when an event with this id had been accepted before; that one is returned
 */
public record EventAccepted(String eventId, String type, String subject, String conversationId, Instant createdAt,
        Instant deliverAt, boolean deduped) {
}
