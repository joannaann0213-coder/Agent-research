package dev.agentflow.agent.api;

import java.util.List;

/**
 * Where the service delivers the updates of all the application's users.
 *
 * @param url an https URL on the public internet (required)
 * @param events the types to deliver; all when absent
 * @param enabled false pauses deliveries; true starts them again after the service stopped them
 * @param rotateSecret true makes a new signing secret, which the response shows once
 */
public record WebhookRequest(String url, List<String> events, Boolean enabled, Boolean rotateSecret) {
}
