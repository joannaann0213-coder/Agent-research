package dev.agentflow.inbox.api;

/**
 * Body of {@code POST /v1/runs/{run_id}/heartbeat}.
 *
 * @param acceptsSteer changes whether the run takes steered events; absent leaves it as it is
 */
public record HeartbeatRequest(int epoch, Boolean acceptsSteer) {
}
