package dev.agentflow.agent.api;

/**
 * A message from a user to the agent.
 *
 * @param messageId the caller's id for the message, unique within the conversation. Sending the
 *     same id with the same text again is harmless; the service makes one up when it is absent
 * @param conversationId which conversation of the user the message belongs to; {@code default}
 *     when absent. A conversation exists from its first message
 * @param agent the key of the agent that serves the conversation; {@code default} when absent. It
 *     is fixed by the conversation's first message
 * @param text what the user said (required)
 */
public record PostMessageRequest(String messageId, String conversationId, String agent, String text) {
}
