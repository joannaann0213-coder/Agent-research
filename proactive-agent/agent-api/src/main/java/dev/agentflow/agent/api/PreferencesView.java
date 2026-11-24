package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * What a user wants of their agents.
 *
 * @param proactivity the user's own setting for how much they hear from their main agent without
 *     having asked, see {@link Proactivity}; absent when they have none, and the agent's default
 *     applies
 * @param setBy who set it last: {@code user}, {@code app}, or {@code agent} when the user told the
 *     main agent in the conversation; absent when it was never set
 * @param updatedAt when it was set last; absent when it was never set
 */
public record PreferencesView(String proactivity, String setBy, Instant updatedAt) {
}
