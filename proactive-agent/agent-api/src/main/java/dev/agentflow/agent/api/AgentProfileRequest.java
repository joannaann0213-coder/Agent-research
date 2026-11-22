package dev.agentflow.agent.api;

import java.util.List;

/**
 * What an application's agent is. Sent with {@code PUT /v1/agents/{agent_key}}, it replaces the
 * agent's whole configuration; a field that is absent goes back to its default.
 *
 * @param name a name for people (required)
 * @param model the Claude model; {@code claude-opus-5-5} when absent
 * @param effort how hard the model thinks: {@code low}, {@code medium}, {@code high}, {@code xhigh}
 *     or {@code max}; the model's own default when absent
 * @param instructions what the agent is for and how it should behave (required). The service adds
 *     its own short notes on how the agent is woken and how it reaches the user
 * @param sandbox the agent's own computer; none when absent
 * @param mcpServers the third-party systems the agent may use; none when absent
 */
public record AgentProfileRequest(String name, String model, String effort, String instructions, SandboxConfig sandbox,
        List<McpServerConfig> mcpServers) {
}
