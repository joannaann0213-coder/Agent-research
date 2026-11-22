package dev.agentflow.agent.api;

import java.time.Instant;
import java.util.List;

/**
 * Something the agent waits for a person to decide: a tool call that needs an approval, or a
 * question to the user. The agent's turn stands still until it is decided, until it expires, or
 * until something new happens on its line.
 *
 * @param kind {@code approval} or {@code question}
 * @param status {@code pending}; then {@code approved}, {@code denied} or {@code answered} when a
 *     person decided; {@code expired} when nobody did in time; {@code withdrawn} when it no longer
 *     applies, because something new happened before anybody decided
 * @param approver who decides: {@code user} (the end user, in the application's client) or
 *     {@code app} (the application's back end)
 * @param conversationId the conversation the action belongs to
 * @param tool for an approval: the call that waits for it
 * @param question for a question: what the agent asks
 * @param options for a question: answers the agent suggests, if any. Any other answer is fine too
 * @param answer for an answered question: the answer
 * @param note why it was denied, expired or withdrawn, as the agent is told
 * @param resolvedBy {@code user}, {@code app}, or {@code service} for an expiry or a withdrawal
 */
public record ActionView(String actionId, String kind, String status, String approver, String conversationId,
        ActionTool tool, String question, List<String> options, String answer, String note, String resolvedBy,
        Instant createdAt, Instant expiresAt, Instant resolvedAt) {
}
