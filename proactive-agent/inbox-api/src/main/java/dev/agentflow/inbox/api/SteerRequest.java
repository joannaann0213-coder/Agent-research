package dev.agentflow.inbox.api;

/**
 * Body of {@code POST /v1/runs/{run_id}/steer}: pull same-key events that arrived after the run
 * started into the run. Call it at a safe point, for example between two tool calls.
 *
 * @param afterRunSeq highest {@code run_seq} the worker has already seen; everything after it is
 *        returned, so a lost response can be fetched again. Absent means "only what joins now".
 */
public record SteerRequest(int epoch, Integer afterRunSeq) {
}
