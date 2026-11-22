package dev.agentflow.agent.cma;

/** An event the agent service sends to a session. */
public sealed interface Outgoing {

    /**
     * A message for the agent. The platform reads its text as the operator's intent, so it must not
     * carry content from a third party; that reaches the agent as the result of a tool call.
     */
    record UserMessage(String text) implements Outgoing {
    }

    /** Stops the work in progress. It overtakes queued messages; a session that is idle ignores it. */
    record Interrupt() implements Outgoing {
    }

    /**
     * Allows or denies a tool call that waits for permission.
     *
     * @param toolUseId the id of the {@link CmaEvent.ToolUse} event
     * @param denyMessage told to the agent when the call is denied; may be {@code null}
     */
    record ToolConfirmation(String toolUseId, boolean allow, String denyMessage) implements Outgoing {
    }

    /**
     * The result of a custom tool call.
     *
     * @param customToolUseId the id of the {@link CmaEvent.CustomToolUse} event
     * @param error whether the call failed; the text then says why
     */
    record CustomToolResult(String customToolUseId, String text, boolean error) implements Outgoing {
    }
}
