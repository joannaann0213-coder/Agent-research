package dev.agentflow.agent.server.profile;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.SandboxConfig;
import dev.agentflow.agent.cma.AgentSpec;
import dev.agentflow.agent.cma.AgentSpec.ToolPolicy;
import dev.agentflow.agent.cma.EnvironmentSpec;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.tools.PlatformTools;
import dev.agentflow.inbox.api.CanonicalJson;
import dev.agentflow.inbox.api.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * What the agent platform is asked to set up for a profile: an agent and an environment. It is a
 * pure function of the profile and of this version of the service, so its hash says whether the
 * platform is up to date.
 */
record ProfileSpec(AgentSpec agent, EnvironmentSpec environment) {

    static ProfileSpec of(String appId, String agentKey, AgentProfileRequest config) {
        Map<String, String> tags = Map.of("app", appId, "agent", agentKey);
        SandboxConfig sandbox = config.sandbox();
        boolean hasSandbox = Boolean.TRUE.equals(sandbox.enabled());

        // Calls of container tools are judged by the platform. One it cannot judge waits for an answer,
        // and this service answers no: a shell command is not something to put before an end user.
        AgentSpec.Sandbox tools = hasSandbox
                ? new AgentSpec.Sandbox(ToolPolicy.AUTO, Boolean.TRUE.equals(sandbox.webSearch()), Boolean.TRUE.equals(sandbox.webFetch()))
                : null;
        List<AgentSpec.McpServer> servers = config.mcpServers().stream()
                .map(server -> new AgentSpec.McpServer(server.name(), server.url(), policy(server)))
                .toList();
        AgentSpec agent = new AgentSpec(appId + "/" + agentKey, config.name(), config.model(), config.effort(),
                PlatformPrompt.compose(config.instructions()), tools, servers, PlatformTools.definitions(), tags);

        // A container exists even for an agent without container tools, so its network is closed
        // unless the profile opens it.
        List<String> hosts = !hasSandbox ? List.of() : switch (sandbox.network()) {
            case "unrestricted" -> null;
            case "limited" -> sandbox.allowedHosts();
            default -> List.of();
        };
        EnvironmentSpec environment = new EnvironmentSpec(appId + "-" + agentKey,
                "Containers of agent " + agentKey + " of application " + appId, hosts, false, false, tags);
        return new ProfileSpec(agent, environment);
    }

    private static ToolPolicy policy(McpServerConfig server) {
        return switch (server.approval()) {
            case "always_allow" -> ToolPolicy.ALWAYS_ALLOW;
            case "auto" -> ToolPolicy.AUTO;
            default -> ToolPolicy.ALWAYS_ASK;
        };
    }

    /** Equal for equal specifications, whatever the order of their map entries. */
    String hash() {
        return Hashing.sha256Hex(CanonicalJson.of(Json.STORED.valueToTree(this)).getBytes(StandardCharsets.UTF_8));
    }
}
