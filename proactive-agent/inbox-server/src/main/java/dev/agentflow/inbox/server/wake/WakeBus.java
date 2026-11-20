package dev.agentflow.inbox.server.wake;

import java.time.Duration;

/**
 * Best-effort hints that tell waiting claimers "something changed, look again". A hint may be lost
 * without harm: correctness reads only the tables, and a claimer re-checks on its poll interval.
 */
public interface WakeBus {

    /** Opaque marker of everything published so far. Read it before checking for work. */
    long generation();

    /** Signals that work may have become claimable. Must be cheap. */
    void publish();

    /**
     * Blocks until something is published after {@code seenGeneration}, or the timeout passes.
     * Comparing generations closes the gap between "checked, found nothing" and "started waiting".
     *
     * @return true when woken by a publication
     */
    boolean await(long seenGeneration, Duration timeout) throws InterruptedException;
}
