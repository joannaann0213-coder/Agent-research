package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * A message of a conversation, from the user or from the agent.
 *
 * @param seq the position in the user's stream, across the user's conversations; it only grows
 * @param role {@code user} or {@code agent}
 * @param kind {@code message} for what the user said, {@code reply} for the agent's answer to it,
 *     {@code notification} for something the agent said of its own accord, {@code question} for a
 *     question the agent waits for the user to answer, {@code proposal} for work a main agent
 *     offers to do and that waits for the user's confirmation
 * @param actionId for a question: the action that waits for the answer. For a message of the user:
 *     the question it answered, if it did
 * @param taskId for a proposal: the task that waits for the confirmation
 */
public record MessageView(long seq, String messageId, String conversationId, String role, String kind, String text,
        Instant createdAt, String actionId, String taskId) {
}
