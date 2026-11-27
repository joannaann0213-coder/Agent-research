package dev.agentflow.agent.api;

/**
 * How long users waited for replies: from the oldest message that a reply answers to the reply.
 * When a reply was ended because the user wrote again, the wait goes on to the reply that came.
 *
 * @param replies how many replies
 * @param medianSeconds half of them came within this; absent when there were none
 * @param p95Seconds 95 in 100 came within this; absent when there were none
 */
public record ReplyStats(long replies, Double medianSeconds, Double p95Seconds) {
}
