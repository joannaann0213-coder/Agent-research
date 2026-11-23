package dev.agentflow.agent.api;

/**
 * An agent that a main agent may hand work to, under the name of what it does there.
 *
 * @param role the name the main agent delegates under, unique within the main agent, for example
 *     {@code researcher}
 * @param agent the key of the agent that does the work: another agent of the same application,
 *     which is not a main agent itself
 * @param description what this worker is good for, as the main agent reads it when it decides whom
 *     to give a task
 */
public record WorkerConfig(String role, String agent, String description) {
}
