package dev.agentflow.agent.api;

import java.math.BigDecimal;
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
 * @param sessionBudgetUsd what one session of the agent may spend at most, in US dollars; no limit
 *     when absent. A conversation whose session has spent it goes on in a new one
 * @param memory {@code per_user}: the agent keeps notes about each user across sessions, in files of
 *     its sandbox, which must be enabled; it writes them in conversations and reads them in the
 *     background. {@code none} when absent
 */
public record AgentProfileRequest(String name, String model, String effort, String instructions, SandboxConfig sandbox,
        List<McpServerConfig> mcpServers, BigDecimal sessionBudgetUsd, String memory) {

    public AgentProfileRequest(String name, String model, String effort, String instructions, SandboxConfig sandbox,
            List<McpServerConfig> mcpServers) {
        this(name, model, effort, instructions, sandbox, mcpServers, null, null);
    }

    public AgentProfileRequest(String name, String model, String effort, String instructions, SandboxConfig sandbox,
            List<McpServerConfig> mcpServers, BigDecimal sessionBudgetUsd) {
        this(name, model, effort, instructions, sandbox, mcpServers, sessionBudgetUsd, null);
    }
}
