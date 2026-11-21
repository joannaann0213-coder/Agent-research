package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * A timer as its latest occurrence shows it. A one-off timer has a single occurrence. A cron timer
 * keeps its {@code timerId} across occurrences, and its view is the next scheduled occurrence, or the
 * cancelled one. Whether a firing was a catch-up after an outage is recorded in the payload of the
 * {@code timer.due} event it produced ({@code catch_up}, {@code missed_count}).
 *
 * @param state {@code scheduled}, {@code fired} or {@code cancelled}
 * @param firedEventId the event this occurrence produced, once it fired
 */
public record TimerView(
        String timerId,
        String tenantId,
        String inboxId,
        String kind,
        String state,
        Instant fireAt,
        String cron,
        String timeZone,
        String timerKey,
        String eventType,
        String mergeKey,
        Lane lane,
        JsonNode payload,
        String firedEventId,
        Instant firedAt,
        Instant createdAt) {
}
