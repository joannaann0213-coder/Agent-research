package dev.agentflow.inbox.api;

import java.time.Instant;

/**
 * Body of {@code POST /v1/admin/replay}. Exactly one selector is used:
 * a single event ({@code eventId}); an inbox and a time range ({@code inboxId}, {@code from}, {@code to});
 * or a source and a time range across the tenant ({@code source}, {@code from}, {@code to}).
 * The range is on the time the event was received and is half-open: {@code from <= t < to}.
 *
 * @param force required to replay an event that is {@code done} or {@code dead}; a {@code failed}
 *        event replays without it
 * @param reason why (required); recorded with every replayed event and in the audit log
 * @param operator the person doing it; recorded as the actor
 * @param dryRun report what would be replayed without changing anything
 */
public record ReplayRequest(String tenantId, String eventId, String inboxId, EventSource source, Instant from,
        Instant to, Boolean force, String reason, String operator, Boolean dryRun) {

    public static ReplayRequest event(String tenantId, String eventId, boolean force, String reason, String operator) {
        return new ReplayRequest(tenantId, eventId, null, null, null, null, force, reason, operator, null);
    }

    public static ReplayRequest inbox(String tenantId, String inboxId, Instant from, Instant to, boolean force,
            String reason, String operator) {
        return new ReplayRequest(tenantId, null, inboxId, null, from, to, force, reason, operator, null);
    }

    public static ReplayRequest source(String tenantId, EventSource source, Instant from, Instant to, boolean force,
            String reason, String operator) {
        return new ReplayRequest(tenantId, null, null, source, from, to, force, reason, operator, null);
    }

    public ReplayRequest asDryRun() {
        return new ReplayRequest(tenantId, eventId, inboxId, source, from, to, force, reason, operator, true);
    }
}
