package dev.agentflow.agent.api;

import java.util.List;

/**
 * Makes an agent a main agent: the one session through which everything about a user passes, and
 * which hands work to workers.
 *
 * <p>In a conversation that a main agent serves, the application's events and schedules for that
 * agent are handled in the conversation's own session instead of in a session per subject, so the
 * main agent knows of everything that happens for the user. Workers run in sessions of their own
 * and report back to it.
 *
 * @param workers the agents it may delegate to; none when absent, and the main agent then does
 *     everything itself
 * @param maxActiveTasks how many delegated tasks may be queued or being worked on at a time, for
 *     one conversation; 4 when absent
 * @param proactivity how forward the main agent is with a user who has not said how they want it:
 *     {@code proactive}, {@code balanced} or {@code quiet}, see {@link Proactivity}; the service's
 *     default when absent, which is {@code proactive} unless it was configured otherwise
 */
public record CoordinatorConfig(List<WorkerConfig> workers, Integer maxActiveTasks, String proactivity) {

    public CoordinatorConfig(List<WorkerConfig> workers, Integer maxActiveTasks) {
        this(workers, maxActiveTasks, null);
    }
}
