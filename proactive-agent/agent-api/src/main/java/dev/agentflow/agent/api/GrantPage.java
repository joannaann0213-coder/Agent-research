package dev.agentflow.agent.api;

import java.util.List;

/** The standing authorisations of a user, by main agent and role. */
public record GrantPage(List<GrantView> grants) {
}
