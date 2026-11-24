package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * Something the agent is to do for a user at a given time: once, at {@code at}, or again and again,
 * on a {@code cron} rule read in the user's time zone. Exactly one of the two.
 *
 * @param scheduleId the caller's id for the schedule, unique for the user. Sending the same schedule
 *     again is harmless; the service makes an id up when it is absent
 * @param name a short name, for lists (required)
 * @param instructions what the agent is to do when the time comes, in the application's words
 *     (required). The agent takes them as the application's instructions
 * @param at when, for a schedule that runs once
 * @param cron when, for a schedule that repeats: five fields, minute first, for example
 *     {@code 0 8 * * 1-5} for 8:00 on weekdays
 * @param timeZone the IANA zone the cron rule is read in, for example {@code Asia/Shanghai}; UTC
 *     when absent
 * @param subject the line the agent works on, as for events: runs of the same subject are handled by
 *     an agent that remembers the earlier ones; {@code schedule-<schedule_id>} when absent
 * @param agent the key of the agent that does it; {@code default} when absent. In a conversation
 *     that a main agent serves, another agent does it as a worker of the main agent: each time the
 *     schedule is due it becomes part of a task of the main agent, which is told of it
 * @param conversationId the conversation in which the user is told, if the agent tells them;
 *     {@code default} when absent
 */
public record CreateScheduleRequest(String scheduleId, String name, String instructions, Instant at, String cron,
        String timeZone, String subject, String agent, String conversationId) {
}
