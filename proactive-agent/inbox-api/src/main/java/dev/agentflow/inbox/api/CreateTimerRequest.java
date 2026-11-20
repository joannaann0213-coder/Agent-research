package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Body of {@code POST /v1/inbox/{inbox_id}/timers}. A due timer only ever produces a {@code timer.due}
 * event in the inbox; it never calls the runtime.
 *
 * @param kind {@code once} or {@code cron}
 * @param fireAt when a {@code once} timer fires
 * @param cron cron expression of a {@code cron} timer: five fields (minute first) or six (second first)
 * @param timeZone IANA zone the cron expression is evaluated in, for example {@code Asia/Shanghai};
 *        "9 am" is the user's 9 am. Defaults to UTC.
 * @param timerKey caller's idempotency key: creating a timer whose key is already scheduled returns that timer
 * @param mergeKey batching key of the events the timer produces
 * @param lane lane of those events; defaults to {@code background}
 * @param payload carried into every event the timer produces
 */
public record CreateTimerRequest(String kind, Instant fireAt, String cron, String timeZone, String timerKey,
        String mergeKey, Lane lane, JsonNode payload) {

    public static final String ONCE = "once";
    public static final String CRON = "cron";

    public static CreateTimerRequest once(Instant fireAt, String payloadJson) {
        return new CreateTimerRequest(ONCE, fireAt, null, null, null, null, null,
                InboxJson.mapper().readTree(payloadJson));
    }

    public static CreateTimerRequest cron(String cron, String timeZone, String payloadJson) {
        return new CreateTimerRequest(CRON, null, cron, timeZone, null, null, null,
                InboxJson.mapper().readTree(payloadJson));
    }

    public CreateTimerRequest withTimerKey(String key) {
        return new CreateTimerRequest(kind, fireAt, cron, timeZone, key, mergeKey, lane, payload);
    }

    public CreateTimerRequest withMergeKey(String key) {
        return new CreateTimerRequest(kind, fireAt, cron, timeZone, timerKey, key, lane, payload);
    }

    public CreateTimerRequest withLane(Lane value) {
        return new CreateTimerRequest(kind, fireAt, cron, timeZone, timerKey, mergeKey, value, payload);
    }
}
