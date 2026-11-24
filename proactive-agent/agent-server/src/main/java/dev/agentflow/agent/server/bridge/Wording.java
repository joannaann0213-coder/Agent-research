package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.Proactivity;

/** Words of the service that more than one place says to an agent, so that they are the same everywhere. */
final class Wording {

    private Wording() {
    }

    /**
     * What a main agent is told about how much its user wants to hear from it without having asked.
     * It is told with every turn in the background, because the user can change it at any time.
     */
    static String howForward(String level) {
        return switch (level) {
            case Proactivity.BALANCED -> "This user wants to hear from you what needs them and what came of the work, not "
                    + "what has merely started or moves along.";
            case Proactivity.QUIET -> "This user wants to hear from you only what is key: what needs their decision, and "
                    + "the outcome of what they asked for themselves. Offer them no work that they did not ask for.";
            default -> "This user likes to be kept up to date: tell them briefly what has started for them, what waits for "
                    + "them and what came of it.";
        };
    }
}
