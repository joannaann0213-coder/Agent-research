package dev.agentflow.agent.api;

import java.time.Instant;
import java.util.List;

/**
 * What the agents did for one user, or for all users of an application, in a span of time: how
 * often they were woken and for what, how those turns ended, what they used, how long users waited
 * for a reply, and what was said. How much an agent does unasked is a setting; these are the
 * numbers to set it by.
 *
 * @param since from when, inclusive
 * @param until to when, exclusive
 * @param users for an application: how many of its users had a turn in that time; absent for one user
 * @param proactivity for one user: their own setting for how forward their main agent is, see
 *     {@link Proactivity}; absent when they have none, and for an application
 * @param turns the turns that began in that time and have ended, by kind and outcome
 * @param replies how long users waited for the replies of those turns
 * @param messages what was said in that time
 * @param tasks the tasks that were made in that time, and what became of them
 * @param levels for an application: its users by their setting for {@code proactivity}, and what
 *     each group was sent; absent for one user
 */
public record StatsView(Instant since, Instant until, Long users, String proactivity, List<TurnStats> turns,
        ReplyStats replies, MessageStats messages, TaskStats tasks, List<LevelStats> levels) {
}
