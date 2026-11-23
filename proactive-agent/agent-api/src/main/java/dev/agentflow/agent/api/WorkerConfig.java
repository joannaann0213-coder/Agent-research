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
 * @param start when a task of this role may start. {@code ask}, which it is when absent: what the
 *     user asks for in a conversation starts at once, and a task that the main agent begins on its
 *     own initiative waits for the user's confirmation, unless the user has given a standing
 *     authorisation for the role. {@code always_ask}: every task of the role waits for the user's
 *     confirmation, and no standing authorisation is possible. {@code always_allow}: every task
 *     starts at once
 */
public record WorkerConfig(String role, String agent, String description, String start) {

    public static final String ASK = "ask";
    public static final String ALWAYS_ASK = "always_ask";
    public static final String ALWAYS_ALLOW = "always_allow";

    /** A worker whose tasks start as {@code ask} says. */
    public WorkerConfig(String role, String agent, String description) {
        this(role, agent, description, null);
    }

    /** The rule in force for the role: {@code ask} when none was set. */
    public String startRule() {
        return start == null ? ASK : start;
    }
}
