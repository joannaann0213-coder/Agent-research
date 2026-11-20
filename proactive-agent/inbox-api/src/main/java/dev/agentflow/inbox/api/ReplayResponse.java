package dev.agentflow.inbox.api;

import java.util.List;

/**
 * Outcome of a replay.
 *
 * @param replayed ids of the events put back in the queue (or that would be, for a dry run)
 * @param skipped events that matched the selector but were left alone, and why
 */
public record ReplayResponse(List<String> replayed, List<Skipped> skipped, boolean dryRun) {

    /** @param why {@code needs_force} or {@code in_flight} */
    public record Skipped(String eventId, String status, String why) {
    }

    public static final String NEEDS_FORCE = "needs_force";
    public static final String IN_FLIGHT = "in_flight";
}
