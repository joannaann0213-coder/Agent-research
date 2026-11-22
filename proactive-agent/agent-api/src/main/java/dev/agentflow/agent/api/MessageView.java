package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * A message of a conversation, from the user or from the agent.
 *
 * @param seq the position in the user's stream, across the user's conversations; it only grows
 * @param role {@code user} or {@code agent}
 * @param kind {@code message} for what the user said, {@code reply} for the agent's answer to it,
 *     {@code notification} for something the agent said of its own accord
 */
public record MessageView(long seq, String messageId, String conversationId, String role, String kind, String text,
        Instant createdAt) {
}
