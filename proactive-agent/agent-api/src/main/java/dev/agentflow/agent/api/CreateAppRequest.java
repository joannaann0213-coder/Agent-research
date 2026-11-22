package dev.agentflow.agent.api;

/**
 * Registers an application.
 *
 * @param appId the application's id, chosen by the operator: 2 to 40 characters of {@code a-z},
 *     {@code 0-9}, {@code _} and {@code -}, starting with a letter or a digit. It cannot be changed
 * @param name a name for people
 */
public record CreateAppRequest(String appId, String name) {
}
