package dev.agentflow.agent.api;

import java.time.Instant;
import java.util.List;

/**
 * A piece of work that a main agent handed to a worker.
 *
 * @param conversationId the conversation whose main agent delegated it
 * @param role the role it was delegated to, and {@code agent} the key of the agent that works on it
 * @param title a short name, as the main agent gave it
 * @param status {@code queued} while the worker has not taken up what the main agent last sent,
 *     {@code working} while a turn of the worker is under way, {@code idle} when the worker has
 *     reported and waits, {@code closed} when the task is over
 * @param closedBy for a closed task: {@code agent} (the main agent), {@code user}, {@code app}, or
 *     {@code service} for a task that nobody had touched for too long
 * @param report the beginning of what the worker last reported, if it has; the notes have all of it
 * @param notes everything the main agent and the worker told each other, oldest first. Only when a
 *     single task is read with an API key
 */
public record TaskView(String taskId, String conversationId, String role, String agent, String title, String status,
        String closedBy, String closedReason, String report, Instant createdAt, Instant updatedAt, Instant closedAt,
        List<TaskNoteView> notes) {
}
