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
 * @param coordinator for a main agent: whom it may hand work to; absent for an ordinary agent
 */
public record AgentProfileView(String agentKey, String name, String model, String effort, String instructions,
        SandboxConfig sandbox, List<McpServerConfig> mcpServers, BigDecimal sessionBudgetUsd, String memory, int revision,
        Instant createdAt, Instant updatedAt, CoordinatorConfig coordinator) {

    /** An ordinary agent. */
    public AgentProfileView(String agentKey, String name, String model, String effort, String instructions,
            SandboxConfig sandbox, List<McpServerConfig> mcpServers, BigDecimal sessionBudgetUsd, String memory, int revision,
            Instant createdAt, Instant updatedAt) {
        this(agentKey, name, model, effort, instructions, sandbox, mcpServers, sessionBudgetUsd, memory, revision, createdAt,
                updatedAt, null);
    }
}
