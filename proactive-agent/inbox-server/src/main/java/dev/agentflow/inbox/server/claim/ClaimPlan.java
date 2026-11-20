package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import java.time.Instant;
import java.util.List;

/** What one claim attempt on one inbox should do. */
public sealed interface ClaimPlan {

    /** Mint a new run from a batch of unclaimed events that share a merge key. */
    record Start(Lane lane, int slotNo, String mergeKey, List<FreshEvent> events, Instant eligibleAt)
            implements ClaimPlan {
    }

    /** Issue a queued run again: same id, same events. */
    record Reissue(String runId, Lane lane, int slotNo) implements ClaimPlan {
    }

    /** Continue a suspended run with the events that carry its resume key. */
    record Resume(String runId, Lane lane, int slotNo, List<FreshEvent> events) implements ClaimPlan {
    }

    /**
     * Nothing can be claimed now.
     *
     * @param retryAt when something will become claimable by time alone (a quiet window closes or a
     *        backoff ends); null when only another run finishing or a new event can change the answer
     */
    record Nothing(Reason reason, Instant retryAt) implements ClaimPlan {
    }

    enum Reason {
        /** No unclaimed work at all. */
        EMPTY,
        /** There is work, but slots are taken or its merge key is being handled by another run. */
        BUSY,
        /** The next batch is still inside its quiet window. */
        WINDOW,
        /** The next run is waiting out its retry backoff. */
        BACKOFF;

        public String wire() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
