package dev.agentflow.agent.api;

/**
 * What a user wants of their agents, as far as it can be set.
 *
 * @param proactivity how much the user wants to hear from their main agent without having asked:
 *     {@code proactive}, {@code balanced} or {@code quiet}, see {@link Proactivity}. {@code null}
 *     takes the user's own setting away, so that the agent's default applies again
 */
public record PreferencesRequest(String proactivity) {
}
