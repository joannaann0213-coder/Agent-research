package dev.agentflow.agent.api;

/**
 * A third-party system the agent reaches over MCP. Its credential is not part of the agent's
 * configuration; it is stored separately.
 *
 * @param name a short name, unique within the agent
 * @param url the server's HTTPS endpoint
 * @param approval when a call of one of its tools needs somebody's approval: {@code always_ask},
 *     {@code auto} (the platform judges each call) or {@code always_allow}
 * @param approver who approves: {@code app} (the application's back end) or {@code user} (the end
 *     user, in the application's client)
 */
public record McpServerConfig(String name, String url, String approval, String approver) {
}
