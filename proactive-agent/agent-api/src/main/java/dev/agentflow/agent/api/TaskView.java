package dev.agentflow.agent.api;

import java.time.Instant;
import java.util.List;

/**
 * A piece of work that a main agent handed to a worker, or proposes to hand to one.
 *
 * @param conversationId the conversation whose main agent delegated it
 * @param role the role it was delegated to, and {@code agent} the key of the agent that works on it
 * @param title a short name, as the main agent gave it
 * @param summary what the main agent wants to do and why, written for the user. Always there for a
 *     task that waits for a confirmation
 * @param status {@code proposed} while the task waits for the user's confirmation and has not
 *     started, {@code queued} while the worker has not taken up what the main agent last sent,
 *     {@code working} while a turn of the worker is under way, {@code idle} when the worker has
 *     reported and waits, {@code closed} when the task is over
 * @param authorizedBy why the task was allowed to start: {@code conversation} (the main agent
 *     delegated or started it in a turn that the user began), {@code user} or {@code app} (the
 *     user's client or the application confirmed it), {@code grant} (the user's standing
 *     authorisation for the role), {@code policy} (the application lets the role start unasked).
 *     Absent while the task waits for a confirmation, and for one that was closed before it got one
 * @param expiresAt for a task that waits for a confirmation: until when it can be given
 * @param closedBy for a closed task: {@code agent} (the main agent), {@code user}, {@code app}, or
 *     {@code service} for a task that nobody had touched or confirmed for too long
 * @param report the beginning of what the worker last reported, if it has; the notes have all of
 *     it. Only for an API key
 * @param notes everything the main agent and the worker told each other, oldest first. Only when a
 *     single task is read with an API key
 */
public record TaskView(String taskId, String conversationId, String role, String agent, String title, String summary,
        String status, String authorizedBy, Instant expiresAt, String closedBy, String closedReason, String report,
        Instant createdAt, Instant updatedAt, Instant closedAt, List<TaskNoteView> notes) {
}
