package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * A new API key.
 *
 * @param apiKey the key itself. It is shown this once; the service keeps only a hash of it
 */
public record CreatedKey(String keyId, String apiKey, String label, Instant createdAt) {
}
