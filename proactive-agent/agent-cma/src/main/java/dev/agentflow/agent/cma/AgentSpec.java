package dev.agentflow.agent.cma;

import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * What an agent is: its model, its instructions and its tools.
 *
 * @param effort {@code low}, {@code medium}, {@code high}, {@code xhigh} or {@code max}; {@code null}
 *     leaves the model's default
 * @param sandbox the tools that run in the session's container; {@code null} gives the agent none
 * @param mcpServers third-party systems reached over MCP. Their credentials are not part of the
 *     agent; a session gets them from a vault
 * @param customTools tools that the caller runs itself
 * @param metadata at most 16 entries
 */
public record AgentSpec(
        String name,
        String description,
        String model,
        String effort,
        String system,
        Sandbox sandbox,
        List<McpServer> mcpServers,
        List<CustomTool> customTools,
        Map<String, String> metadata) {

    public AgentSpec {
        mcpServers = mcpServers == null ? List.of() : List.copyOf(mcpServers);
        customTools = customTools == null ? List.of() : List.copyOf(customTools);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** When a tool call needs somebody's permission. */
    public enum ToolPolicy {
        /** Calls run without asking. */
        ALWAYS_ALLOW,
        /** Every call waits for a confirmation. */
        ALWAYS_ASK,
        /** The platform judges each call: it runs it, denies it, or asks. */
        AUTO
    }

    /**
     * The built-in tools: a shell and file tools in the session's container, and optionally the web.
     *
     * @param policy applies to every built-in tool
     */
    public record Sandbox(ToolPolicy policy, boolean webSearch, boolean webFetch) {
    }

    /** An MCP server and the policy for its tools. */
    public record McpServer(String name, String url, ToolPolicy policy) {
    }

    /**
     * A tool the caller runs.
     *
     * @param inputSchema a JSON Schema of type {@code object}
     */
    public record CustomTool(String name, String description, JsonNode inputSchema) {
    }
}
