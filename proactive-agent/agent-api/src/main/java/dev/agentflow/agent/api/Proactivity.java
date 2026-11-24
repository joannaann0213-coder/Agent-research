package dev.agentflow.agent.api;

import java.util.List;

/**
 * How much a user wants to hear from their main agent without having asked: how forward the agent
 * is. An assistant that is keen at first is right for most people and too much for some, so this
 * is the user's to turn up or down, with the agent's own default to start from.
 *
 * <table>
 *   <caption>What each level changes</caption>
 *   <tr><th></th><th>{@code proactive}</th><th>{@code balanced}</th><th>{@code quiet}</th></tr>
 *   <tr><td>The main agent is woken at once when</td><td>anything happens to a task</td>
 *       <td>a worker reports or fails, a task is closed, or a worker waits for the user</td>
 *       <td>a worker reports or fails, or a task is closed</td></tr>
 *   <tr><td>It tells the user</td><td>what has started, what waits for them, and what came of it</td>
 *       <td>what needs them, and what came of it</td>
 *       <td>only what needs their decision, and the outcome of what they asked for</td></tr>
 *   <tr><td>Work of its own initiative</td><td>offered, or started where it may</td>
 *       <td>offered, or started where it may</td><td>not offered; started only where the user or the application allowed it</td></tr>
 * </table>
 *
 * <p>Whatever the level, the main agent knows of everything: what does not wake it is given to its
 * next turn.
 */
public final class Proactivity {

    /** The agent keeps the user up to date. The default. */
    public static final String PROACTIVE = "proactive";

    /** The agent tells the user what needs them and what came of the work. */
    public static final String BALANCED = "balanced";

    /** The agent tells the user only what is key, and offers nothing unasked. */
    public static final String QUIET = "quiet";

    public static final List<String> LEVELS = List.of(PROACTIVE, BALANCED, QUIET);

    private Proactivity() {
    }
}
