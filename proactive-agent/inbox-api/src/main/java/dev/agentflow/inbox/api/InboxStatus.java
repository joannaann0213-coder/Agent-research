package dev.agentflow.inbox.api;

import tools.jackson.databind.JsonNode;

/**
 * An inbox as an operator sees it: its configuration and how much is waiting where.
 *
 * @param pending unclaimed events waiting to be picked up
 * @param retrying events bound to a run that is waiting to be issued again
 * @param claimed events held by an active or suspended run
 */
public record InboxStatus(
        String tenantId,
        String inboxId,
        Integer capInteractive,
        Integer capBackground,
        Integer capBatch,
        Integer maxActiveRuns,
        JsonNode settings,
        long pending,
        long retrying,
        long claimed,
        long dead,
        long activeRuns,
        long queuedRuns,
        long suspendedRuns) {
}
