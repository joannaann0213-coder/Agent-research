package dev.agentflow.inbox.api;

import java.time.Instant;

/**
 * Answer to ack, nack and suspend.
 *
 * @param status the run's state now
 * @param nextAttemptAt when a nacked run may be issued again
 */
public record RunOutcome(String runId, String status, int epoch, int attempt, Instant nextAttemptAt) {
}
