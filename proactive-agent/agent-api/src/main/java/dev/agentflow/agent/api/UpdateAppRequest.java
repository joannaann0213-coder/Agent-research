package dev.agentflow.agent.api;

/**
 * Changes an application. A field that is absent stays as it is.
 *
 * @param status {@code active} or {@code suspended}. A suspended application's API keys and user
 *     tokens are refused until it is active again
 */
public record UpdateAppRequest(String name, String status) {
}
