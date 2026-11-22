package dev.agentflow.agent.api;

/**
 * Asks for a new API key of an application.
 *
 * @param label what the key is for, to tell keys apart later; optional
 */
public record CreateKeyRequest(String label) {
}
