package dev.agentflow.inbox.api;

/**
 * Body of {@code POST /v1/runs/{run_id}/nack}: "I could not process this; try again later".
 * The same run, with the same events, is issued again after a backoff, until the attempt limit.
 *
 * @param retryAfterMs overrides the exponential backoff, bounded by the configured cap
 * @param deferred {@code true} when the worker hands the run back for a reason that is not the
 *     run's: something else has to finish first, or the worker is stopping. The attempt is then not
 *     counted, so that waiting cannot make a run {@code dead}. It is honoured for a limited time
 *     after the run was made ({@code inbox.retry.max-defer}); after that it counts like any nack
 */
public record NackRequest(int epoch, String reason, Long retryAfterMs, Boolean deferred) {

    public NackRequest(int epoch, String reason, Long retryAfterMs) {
        this(epoch, reason, retryAfterMs, null);
    }

    /** Hands the run back for a reason that is not the run's; the attempt is not counted. */
    public static NackRequest deferred(int epoch, String reason, Long retryAfterMs) {
        return new NackRequest(epoch, reason, retryAfterMs, true);
    }
}
