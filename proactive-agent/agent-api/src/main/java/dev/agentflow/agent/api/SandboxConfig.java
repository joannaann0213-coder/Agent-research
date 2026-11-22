package dev.agentflow.agent.api;

import java.util.List;

/**
 * The agent's own computer: a shell and file tools in a container that exists for one conversation.
 * Off unless enabled.
 *
 * @param enabled whether the agent has the container tools at all
 * @param webSearch whether the agent may search the web
 * @param webFetch whether the agent may read web pages
 * @param network what the container may reach: {@code none}, {@code limited} (only
 *     {@code allowedHosts}) or {@code unrestricted}
 * @param allowedHosts the hosts a {@code limited} container may reach; plain host names
 */
public record SandboxConfig(Boolean enabled, Boolean webSearch, Boolean webFetch, String network, List<String> allowedHosts) {
}
