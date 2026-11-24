package dev.agentflow.agent.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Something that happened for a user, as the stream, the updates and the webhook deliver it.
 *
 * @param id unique; a receiver that gets an update twice can tell by it
 * @param seq the position in the user's log; it only grows, and resuming after it misses nothing
 * @param type {@code message.created}, {@code action.created}, {@code action.resolved},
 *     {@code task.created}, {@code task.updated} or {@code run.failed}
 * @param data for a message, the message; for an action, the action; for a task, the task; for a
 *     failed run, what failed
 */
public record Update(String id, long seq, String userId, String type, Instant createdAt, JsonNode data) {

    public static final String MESSAGE_CREATED = "message.created";
    public static final String ACTION_CREATED = "action.created";
    public static final String ACTION_RESOLVED = "action.resolved";
    public static final String RUN_FAILED = "run.failed";
    public static final String TASK_CREATED = "task.created";
    public static final String TASK_UPDATED = "task.updated";
    public static final String PREFERENCES_UPDATED = "preferences.updated";
}
