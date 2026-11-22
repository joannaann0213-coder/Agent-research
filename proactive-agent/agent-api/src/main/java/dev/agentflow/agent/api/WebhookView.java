package dev.agentflow.agent.api;

import java.time.Instant;
import java.util.List;

/**
 * The application's webhook.
 *
 * @param events the types it gets; absent for all
 * @param state {@code active}, or {@code disabled}: by the application, or by the service after
 *     deliveries kept failing
 * @param disabledReason why it is disabled
 * @param signingSecret the secret that signs deliveries. Shown only when it is made: when the webhook
 *     is set up, and when it is rotated
 */
public record WebhookView(String url, List<String> events, String state, String disabledReason, String signingSecret,
        Instant createdAt, Instant updatedAt) {
}
