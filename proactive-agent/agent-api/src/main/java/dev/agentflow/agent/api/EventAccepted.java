package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * The receipt for an event. It says the event is stored and will be handled, not that it has been,
 * and not that the user will be told: that is the agent's decision.
 *
 * @param deliverAt when the event will be handled at the earliest; absent when that is at once
 * @param deduped true when an event with this id had been accepted before; that one is returned
 * @param taskId for an event that went to a worker in a conversation that a main agent serves: the
 *     task of the main agent that it became part of
 */
public record EventAccepted(String eventId, String type, String subject, String conversationId, Instant createdAt,
        Instant deliverAt, boolean deduped, String taskId) {

    /** The receipt for an event that is handled by its agent alone, or by the main agent itself. */
    public EventAccepted(String eventId, String type, String subject, String conversationId, Instant createdAt,
            Instant deliverAt, boolean deduped) {
        this(eventId, type, subject, conversationId, createdAt, deliverAt, deduped, null);
    }
}
