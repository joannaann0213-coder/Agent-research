package dev.agentflow.agent.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An agent of an application, with every default filled in.
 *
 * @param agentKey the application's name for the agent
 * @param revision counts the changes of the configuration, from 1. Conversations that are under way
 *     pick up a new revision at their next turn
 */
public record AgentProfileView(String agentKey, String name, String model, String effort, String instructions,
        SandboxConfig sandbox, List<McpServerConfig> mcpServers, BigDecimal sessionBudgetUsd, String memory, int revision,
        Instant createdAt, Instant updatedAt) {
}
