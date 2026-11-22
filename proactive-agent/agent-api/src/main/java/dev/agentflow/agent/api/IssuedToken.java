package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * A user token. It is bound to one application and one user and expires on its own.
 *
 * @param token sent as {@code Authorization: Bearer <token>}
 */
public record IssuedToken(String token, Instant expiresAt) {
}
