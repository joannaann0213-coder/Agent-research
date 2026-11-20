package dev.agentflow.inbox.api;

/**
 * Error body ({@code application/problem+json}). {@code code} is the stable, machine-readable part.
 */
public record Problem(String type, String title, Integer status, String detail, String instance, String code) {
}
