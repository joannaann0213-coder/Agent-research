package dev.agentflow.agent.api;

/**
 * The turns of one kind that ended one way.
 *
 * @param kind {@code chat}: a user wrote. {@code background}: something happened in the
 *     application's systems, fell due, or a worker reported. {@code worker}: a worker's turn on a task
 * @param outcome how they ended. Of a chat: {@code replied}, {@code silent}, {@code refused},
 *     {@code stopped} by the user, {@code superseded} because the user wrote again, or
 *     {@code absorbed} because another turn answered the message. In the background:
 *     {@code notified}, {@code silent}, {@code refused}, or {@code absorbed} because another turn
 *     had been given the news. Of a worker: {@code reported}, {@code silent} or {@code closed}. Of
 *     any: {@code failed}
 * @param turns how many
 * @param yields how often they gave way to a message of the user and started over
 * @param seconds how long they took together, from the first delivery of their run to their end
 * @param modelRequests the requests to the model that they made: one for every step of the agent
 * @param inputTokens input that was neither read from the cache nor written to it
 * @param outputTokens what the model wrote
 * @param cacheReadTokens input that was read from the cache
 * @param cacheCreationTokens input that was written to the cache
 */
public record TurnStats(String kind, String outcome, long turns, long yields, double seconds, long modelRequests,
        long inputTokens, long outputTokens, long cacheReadTokens, long cacheCreationTokens) {
}
