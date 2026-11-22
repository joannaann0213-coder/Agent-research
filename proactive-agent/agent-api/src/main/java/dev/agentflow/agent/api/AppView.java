package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * An application that uses the agent service.
 *
 * @param status {@code active} or {@code suspended}
 */
public record AppView(String appId, String name, String status, Instant createdAt) {
}
