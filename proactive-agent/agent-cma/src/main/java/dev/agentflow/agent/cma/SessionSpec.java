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
 * @param memory the memory stores mounted in the session's container, at most 8. Like vaults, they
 *     can only be attached when the session is created
 */
public record SessionSpec(AgentRef agent, String environmentId, String title, Map<String, String> metadata,
        List<String> vaultIds, BigDecimal budgetUsd, List<MemoryMount> memory) {

    /**
     * A memory store as a session sees it: a directory in its container, under {@code /mnt/memory/}.
     *
     * @param readOnly whether the agent may only read it
     * @param instructions what the store is for, as the agent is told
     */
    public record MemoryMount(String storeId, boolean readOnly, String instructions) {
    }

    public SessionSpec {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        vaultIds = vaultIds == null ? List.of() : List.copyOf(vaultIds);
        memory = memory == null ? List.of() : List.copyOf(memory);
    }

    public SessionSpec(AgentRef agent, String environmentId, String title, Map<String, String> metadata, List<String> vaultIds) {
        this(agent, environmentId, title, metadata, vaultIds, null, null);
    }

    public SessionSpec(AgentRef agent, String environmentId, String title, Map<String, String> metadata, List<String> vaultIds,
            BigDecimal budgetUsd) {
        this(agent, environmentId, title, metadata, vaultIds, budgetUsd, null);
    }
}
