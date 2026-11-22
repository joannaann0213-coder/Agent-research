package dev.agentflow.agent.api;

/**
 * Who the service takes the caller for.
 *
 * @param kind {@code admin}, {@code app} or {@code user}
 * @param appId absent for an administrator
 * @param userId present for a user token only
 */
public record Whoami(String kind, String appId, String userId) {
}
