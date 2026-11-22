package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * An API key without its secret.
 *
 * @param lastUsedAt when the key last authenticated a request, to the minute; absent if never
 * @param revokedAt when the key was revoked; absent while it is valid
 */
public record KeyView(String keyId, String label, Instant createdAt, Instant lastUsedAt, Instant revokedAt) {
}
