package dev.agentflow.agent.cma;

import java.util.List;
import java.util.Map;

/**
 * The template for the containers that sessions run their tools in.
 *
 * @param allowedHosts the hosts a container may reach; {@code null} for unrestricted networking.
 *     An empty list lets it reach no host of its own choosing
 * @param allowMcpServers with restricted networking, whether the agent's MCP servers stay reachable
 * @param allowPackageManagers with restricted networking, whether package registries stay reachable
 */
public record EnvironmentSpec(
        String name,
        String description,
        List<String> allowedHosts,
        boolean allowMcpServers,
        boolean allowPackageManagers,
        Map<String, String> metadata) {

    public EnvironmentSpec {
        allowedHosts = allowedHosts == null ? null : List.copyOf(allowedHosts);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean unrestricted() {
        return allowedHosts == null;
    }
}
