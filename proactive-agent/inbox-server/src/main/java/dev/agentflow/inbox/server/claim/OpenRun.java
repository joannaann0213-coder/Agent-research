package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.RunStatus;
import java.time.Instant;

/** A run of the inbox that is not finished: active, queued for re-issue, or suspended. */
public record OpenRun(String runId, Lane lane, String mergeKey, RunStatus status, long headSeq, Integer slotNo,
        Instant nextAttemptAt, String resumeKey) {
}
