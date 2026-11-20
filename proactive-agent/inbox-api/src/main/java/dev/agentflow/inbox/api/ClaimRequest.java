package dev.agentflow.inbox.api;

import java.util.List;

/**
 * Body of {@code POST /v1/runs/claim}: "give me one run to execute".
 *
 * @param workerId who is asking; recorded as the lease owner and in the event history (required)
 * @param tenantId restricts the search to one tenant; required when {@code inboxId} is given
 * @param inboxId claim from this inbox only (used after a wake hint); absent means any inbox
 * @param lanes restricts the claim to these lanes; absent means all, highest priority first
 * @param acceptsSteer whether same-key events that arrive later may be pulled into the run
 * @param waitMs how long to wait for work before answering 204; absent or 0 means do not wait
 */
public record ClaimRequest(String workerId, String tenantId, String inboxId, List<Lane> lanes, Boolean acceptsSteer,
        Long waitMs) {

    public static ClaimRequest any(String workerId) {
        return new ClaimRequest(workerId, null, null, null, null, null);
    }

    public static ClaimRequest inbox(String workerId, String tenantId, String inboxId) {
        return new ClaimRequest(workerId, tenantId, inboxId, null, null, null);
    }

    public ClaimRequest withLanes(Lane... only) {
        return new ClaimRequest(workerId, tenantId, inboxId, List.of(only), acceptsSteer, waitMs);
    }

    public ClaimRequest withSteer(boolean accepts) {
        return new ClaimRequest(workerId, tenantId, inboxId, lanes, accepts, waitMs);
    }

    public ClaimRequest withWait(long millis) {
        return new ClaimRequest(workerId, tenantId, inboxId, lanes, acceptsSteer, millis);
    }
}
