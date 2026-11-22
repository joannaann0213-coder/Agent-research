package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * The receipt for a message. It says the message is stored and will be answered, not that it has
 * been.
 *
 * @param seq the message's position in the user's stream; replies have higher ones
 * @param deduped true when a message with this id had been accepted before; that one is returned
 */
public record MessageAccepted(String messageId, String conversationId, long seq, Instant createdAt, boolean deduped) {
}
