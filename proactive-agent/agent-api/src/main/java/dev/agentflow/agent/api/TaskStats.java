package dev.agentflow.agent.api;

/**
 * The tasks of main agents that were made in a span of time, and what became of them.
 *
 * @param made how many
 * @param byAgent of those, made by a main agent
 * @param byApplication of those, handed to a worker by the application itself
 * @param askedFor made in a turn that the user began: the user asked for the work
 * @param confirmed offered by a main agent, and confirmed by the user or the application
 * @param unasked begun by a main agent on its own initiative and allowed to start by the user's
 *     standing authorisation or the rule of the role
 * @param declined offered, and declined by the user or the application
 * @param expired offered, and never confirmed in time
 * @param withdrawn offered, and taken back by the main agent
 * @param waiting offered, and still waiting for a confirmation
 * @param failures turns of workers that failed in that time
 */
public record TaskStats(long made, long byAgent, long byApplication, long askedFor, long confirmed, long unasked,
        long declined, long expired, long withdrawn, long waiting, long failures) {
}
