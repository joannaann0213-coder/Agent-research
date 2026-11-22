package dev.agentflow.agent.api;

/**
 * A person's decision on an action.
 *
 * @param decision for an approval: {@code approve} or {@code deny}. For a question: {@code deny}
 *     declines to answer; leave it out to answer
 * @param answer for a question: the answer
 * @param message with a denial: why, in words the agent is given
 */
public record ResolveActionRequest(String decision, String answer, String message) {
}
