package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * A credential as this service shows it: never its secrets, which only the agent platform has.
 *
 * @param kind {@code bearer} or {@code oauth}
 * @param userId the user whose credential it is; absent for the application's
 */
public record McpCredentialView(String name, String mcpServerUrl, String kind, String userId, Instant createdAt,
        Instant updatedAt) {
}
