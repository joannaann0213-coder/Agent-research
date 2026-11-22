package dev.agentflow.agent.api;

import java.util.List;

/** The actions that wait for a decision, oldest first. */
public record ActionPage(List<ActionView> actions) {
}
