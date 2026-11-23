package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * Something a main agent and a worker told each other about a task.
 *
 * @param seq the position within the task, from 1
 * @param from {@code main} for the main agent, {@code worker} for the worker, {@code service} for
 *     what the service itself notes: a turn of the worker that failed, a task closed from outside
 * @param kind {@code assignment} for what starts the task, {@code message} for a later word of the
 *     main agent, {@code report} for what the worker says at the end of a turn, {@code failure} or
 *     {@code closed}
 */
public record TaskNoteView(int seq, String from, String kind, String text, Instant createdAt) {
}
