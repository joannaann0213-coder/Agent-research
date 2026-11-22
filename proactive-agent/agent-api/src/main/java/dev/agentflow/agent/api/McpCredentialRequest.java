package dev.agentflow.agent.api;

/**
 * A credential for an MCP server: a bearer token, or OAuth tokens. Exactly one of them.
 *
 * @param mcpServerUrl the server's https URL, as the agent's configuration names it (required)
 */
public record McpCredentialRequest(String mcpServerUrl, String token, OAuthTokens oauth) {
}
