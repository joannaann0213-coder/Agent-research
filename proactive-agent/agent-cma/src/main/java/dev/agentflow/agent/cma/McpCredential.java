package dev.agentflow.agent.cma;

import java.time.Instant;

/**
 * A credential for an MCP server, kept in a vault on the platform. When a session that has the vault
 * calls the server at {@code mcpServerUrl}, the platform adds the credential to the request; neither
 * the agent nor its sandbox ever sees it. Exactly one of {@code bearerToken} and {@code oauth} is set.
 *
 * @param mcpServerUrl the server it is for; matched without regard to case, a default port or a trailing slash
 * @param bearerToken a token the server takes as it is
 * @param oauth an OAuth access token, which the platform refreshes when it has what it needs to
 */
public record McpCredential(String mcpServerUrl, String displayName, String bearerToken, OAuth oauth) {

    public McpCredential {
        if ((bearerToken == null) == (oauth == null)) {
            throw new IllegalArgumentException("a credential has a bearer token or OAuth tokens, exactly one of them");
        }
    }

    /**
     * @param expiresAt when the access token expires, if known
     * @param refresh what the platform needs to get a new access token; without it the credential
     *     works until the access token expires
     */
    public record OAuth(String accessToken, Instant expiresAt, Refresh refresh) {
    }

    /**
     * @param tokenEndpointAuth how the platform authenticates at the token endpoint: {@code none},
     *     {@code client_secret_basic} or {@code client_secret_post}
     * @param clientSecret for the two {@code client_secret_*} kinds
     */
    public record Refresh(String refreshToken, String clientId, String tokenEndpoint, String tokenEndpointAuth,
            String clientSecret, String scope) {
    }

    /** What the platform shows of a stored credential: never its secrets. */
    public record Stored(String id, String mcpServerUrl, boolean archived) {
    }
}
