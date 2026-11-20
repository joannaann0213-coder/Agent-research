package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * One occurrence of a timer. A cron timer keeps its {@code timerId} across occurrences; the view shows
 * the latest one.
 *
 * @param state {@code scheduled}, {@code fired}, {@code missed} or {@code cancelled}
 * @param catchUp true when this occurrence fired late, standing in for {@code missedCount} missed ones
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
        boolean catchUp,
        int missedCount,
        Instant createdAt) {
}
