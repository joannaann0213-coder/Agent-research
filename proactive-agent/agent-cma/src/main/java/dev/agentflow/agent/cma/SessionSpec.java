package dev.agentflow.agent.cma;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * What a new session runs on.
 *
 * @param metadata at most 8 entries
 * @param vaultIds the vaults that hold the credentials of the agent's MCP servers. They can only be
 *     attached when the session is created
 * @param budgetUsd what the session may spend at most, in US dollars; {@code null} for no limit. When
 *     it is spent, the turn ends with {@link CmaEvent.StopReason#BUDGET_REACHED}
 */
public record SessionSpec(AgentRef agent, String environmentId, String title, Map<String, String> metadata,
        List<String> vaultIds, BigDecimal budgetUsd) {

    public SessionSpec {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        vaultIds = vaultIds == null ? List.of() : List.copyOf(vaultIds);
    }

    public SessionSpec(AgentRef agent, String environmentId, String title, Map<String, String> metadata, List<String> vaultIds) {
        this(agent, environmentId, title, metadata, vaultIds, null);
    }
}
