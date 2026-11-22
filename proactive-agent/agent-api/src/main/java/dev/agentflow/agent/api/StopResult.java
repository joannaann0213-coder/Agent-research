package dev.agentflow.agent.api;

/**
 * What a stop did.
 *
 * @param stoppedThrough the user's messages up to this position are not answered any more
 * @param inProgress whether a reply was under way, or messages were waiting for one
 */
public record StopResult(String conversationId, long stoppedThrough, boolean inProgress) {
}
