package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * A schedule: set up by the application, or a follow-up that the agent set itself.
 *
 * @param createdBy {@code app} or {@code agent}
 * @param subject the subject it runs on; for what a main agent's conversation hands to another
 *     agent, the subject its task is kept under
 * @param kind {@code once} or {@code cron}
 * @param name the application's name for it
 * @param instructions the application's instructions
 * @param note for a follow-up: the agent's note to itself
 * @param state {@code active}; {@code done} for a schedule that ran once and has run; {@code cancelled}
 * @param lastFiredAt the last time it was due, if it has been
 */
public record ScheduleView(String scheduleId, String createdBy, String kind, String name, String instructions, String note,
        Instant at, String cron, String timeZone, String subject, String agent, String conversationId, String state,
        Instant lastFiredAt, Instant createdAt) {
}
