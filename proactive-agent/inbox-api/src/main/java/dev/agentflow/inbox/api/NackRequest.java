package dev.agentflow.inbox.api;

/**
 * Body of {@code POST /v1/runs/{run_id}/nack}: "I could not process this; try again later".
 * The same run, with the same events, is issued again after a backoff, until the attempt limit.
 *
 * @param retryAfterMs overrides the exponential backoff, bounded by the configured cap
 */
public record NackRequest(int epoch, String reason, Long retryAfterMs) {
}
