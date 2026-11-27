package dev.agentflow.agent.api;

/**
 * What was said in a span of time.
 *
 * @param fromUser messages of users
 * @param replies answers to them
 * @param notifications what agents told users without having been asked
 * @param questions what agents asked users
 * @param proposals work that a main agent offered to do
 */
public record MessageStats(long fromUser, long replies, long notifications, long questions, long proposals) {
}
