package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * Something a main agent and a worker told each other about a task, or the application told them.
 *
 * @param seq the position within the task, from 1
 * @param from {@code main} for the main agent, {@code worker} for the worker, {@code app} for the
 *     application, {@code service} for what the service itself notes: a turn of the worker that
 *     failed, a task closed from outside, a worker that waits for a person
 * @param kind {@code assignment} for what starts a task of the main agent, {@code message} for a
 *     later word of the main agent, {@code event} for an event that the application handed to the
 *     worker, {@code schedule} for something that the application scheduled for the worker and
 *     that is due, {@code assigned} for what tells the main agent of either, {@code waiting} for
 *     what tells the main agent that the worker waits for a person's decision and {@code decided}
 *     for what became of it, {@code report} for what the worker says at the end of a turn,
 *     {@code failure} or {@code closed}
 */
public record TaskNoteView(int seq, String from, String kind, String text, Instant createdAt) {
}
