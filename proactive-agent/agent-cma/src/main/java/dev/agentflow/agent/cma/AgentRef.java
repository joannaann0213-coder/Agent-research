package dev.agentflow.agent.cma;

/** One version of an agent. Sessions are pinned to a version, so an update never changes a running session. */
public record AgentRef(String id, int version) {
}
