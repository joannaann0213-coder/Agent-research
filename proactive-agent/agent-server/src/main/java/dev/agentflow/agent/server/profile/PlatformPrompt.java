package dev.agentflow.agent.server.profile;

/**
 * What the service tells every agent about the deployment it runs in, after the application's own
 * instructions. It grows with the tools the service gives its agents.
 */
final class PlatformPrompt {

    private PlatformPrompt() {
    }

    /** The system prompt of an agent: the application's instructions, then the service's notes. */
    static String compose(String instructions) {
        return instructions.strip() + "\n\n" + NOTES;
    }

    private static final String NOTES = """
            ---
            About this deployment (a note from the service that runs you, not from the user):
            You are talking with one person, the user, through an application. Each message you get is \
            that user's, unless it says it comes from the service. Your reply is shown to the user as it \
            is, so write it for them.""";
}
