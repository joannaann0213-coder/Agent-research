package dev.agentflow.agent.cma.fake;

/**
 * The result of a tool call, as the scripted model sees it.
 *
 * @param toolUseId the id of the event that made the call
 * @param server the MCP server; {@code null} for a built-in or a custom tool
 */
public record ToolOutcome(String toolUseId, String server, String tool, String text, boolean error) {

    public static ToolOutcome ok(String text) {
        return new ToolOutcome(null, null, null, text, false);
    }

    public static ToolOutcome failed(String text) {
        return new ToolOutcome(null, null, null, text, true);
    }

    ToolOutcome of(String toolUseId, String server, String tool) {
        return new ToolOutcome(toolUseId, server, tool, text, error);
    }
}
