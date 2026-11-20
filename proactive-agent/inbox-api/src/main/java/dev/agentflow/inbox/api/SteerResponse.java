package dev.agentflow.inbox.api;

import java.util.List;

/** Events that are part of the run beyond the cursor the worker sent, in run order. */
public record SteerResponse(List<ClaimedEvent> events) {

    public int lastRunSeq(int fallback) {
        return events.stream().mapToInt(ClaimedEvent::runSeq).max().orElse(fallback);
    }
}
