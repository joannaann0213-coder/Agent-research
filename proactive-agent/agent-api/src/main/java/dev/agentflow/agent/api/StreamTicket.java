package dev.agentflow.agent.api;

import java.time.Instant;

/** Opens one user's stream once, before it expires, without a header: {@code GET .../stream?ticket=<ticket>}. */
public record StreamTicket(String ticket, Instant expiresAt) {
}
