package dev.agentflow.agent.api;

/**
 * Asks for a token that lets an end user's client call the Agent API directly.
 *
 * @param ttlSeconds how long the token is valid; the service's default when absent, and never more
 *     than its maximum
 */
public record IssueTokenRequest(Long ttlSeconds) {
}
