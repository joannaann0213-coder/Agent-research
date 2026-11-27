package dev.agentflow.agent.api;

/**
 * The users of an application who have one setting for how forward their main agent is.
 *
 * @param proactivity {@code proactive}, {@code balanced} or {@code quiet}; {@code default} for the
 *     users who have not said, for whom the agent's or the service's default applies
 * @param users how many users
 * @param notifications how many notifications they were sent in the span of time
 */
public record LevelStats(String proactivity, long users, long notifications) {
}
