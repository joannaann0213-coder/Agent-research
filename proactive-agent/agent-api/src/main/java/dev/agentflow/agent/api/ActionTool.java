package dev.agentflow.agent.api;

import tools.jackson.databind.JsonNode;

/**
 * The tool call that an approval is about.
 *
 * @param server the MCP server that has the tool
 * @param name the tool
 * @param input what the agent passes to it: what the call would do
 */
public record ActionTool(String server, String name, JsonNode input) {
}
