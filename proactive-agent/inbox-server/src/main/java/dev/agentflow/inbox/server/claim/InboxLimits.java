package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import java.time.Duration;
import java.util.Map;

/**
 * The limits that apply to one inbox: service defaults overlaid with the inbox's own settings.
 *
 * @param maxActiveRuns inbox-wide cap on concurrent runs; null means only the lane caps apply,
 *        1 gives the strict "one run per inbox" reading of the design
 */
public record InboxLimits(Map<Lane, Integer> caps, Integer maxActiveRuns, Map<Lane, Duration> quietWindow,
        Map<Lane, Duration> maxWait, int maxBatchEvents, int maxBatchBytes) {

    public int cap(Lane lane) {
        return caps.get(lane);
    }

    public Duration quietWindow(Lane lane) {
        return quietWindow.get(lane);
    }

    public Duration maxWait(Lane lane) {
        return maxWait.get(lane);
    }
}
