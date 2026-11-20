package dev.agentflow.inbox.api;

import java.time.Instant;

/**
 * Lease state plus the signals a running run should look at between tool calls.
 *
 * @param extended false once the maximum lease duration is reached: the run will be reclaimed at
 *        {@code leaseExpiresAt} unless it finishes or suspends first
 * @param steerPending same-key events waiting to be pulled into this run with a steer call
 * @param interruptRequested an interrupt-flagged event is waiting behind this run; the run should
 *        suspend (keeping its state) so that the event can be handled first
 */
public record HeartbeatResponse(Instant leaseExpiresAt, boolean extended, int steerPending,
        boolean interruptRequested, String interruptEventId) {
}
