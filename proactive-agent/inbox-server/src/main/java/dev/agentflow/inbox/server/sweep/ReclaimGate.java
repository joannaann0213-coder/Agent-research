package dev.agentflow.inbox.server.sweep;

import java.time.Duration;
import java.time.Instant;

/**
 * Stops the lease sweeper from mass-reclaiming healthy runs after an outage.
 *
 * <p>While the database or this process is unavailable, workers cannot heartbeat, so every lease looks
 * expired the moment service returns. The gate holds reclaiming back for one lease interval after a
 * start, a failed pass or a stalled loop, which gives live workers the chance to renew first.
 */
public final class ReclaimGate {

    private final Duration leaseDuration;
    private Instant holdUntil;
    private Instant lastPassAt;

    public ReclaimGate(Duration leaseDuration, Instant startedAt) {
        this.leaseDuration = leaseDuration;
        this.holdUntil = startedAt.plus(leaseDuration);
        this.lastPassAt = startedAt;
    }

    /** True when a reclaim pass may run now. */
    public synchronized boolean mayReclaim(Instant now) {
        if (Duration.between(lastPassAt, now).compareTo(leaseDuration) > 0) {
            // The loop itself stalled for longer than a lease: treat it like an outage.
            holdUntil = now.plus(leaseDuration);
        }
        lastPassAt = now;
        return !now.isBefore(holdUntil);
    }

    public synchronized void passFailed(Instant now) {
        holdUntil = now.plus(leaseDuration);
    }
}
