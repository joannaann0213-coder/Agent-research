package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * OAuth tokens for an MCP server. With a refresh token, the agent platform gets new access tokens by
 * itself, from the token endpoint.
 *
 * @param accessToken required
 * @param expiresAt when the access token expires, if known
 * @param refreshToken optional; with it, {@code clientId} and {@code tokenEndpoint} are required
 * @param tokenEndpointAuth how the platform authenticates at the token endpoint: {@code none}
 *     (default), {@code client_secret_basic} or {@code client_secret_post}
 * @param clientSecret for the two {@code client_secret_*} kinds
 */
public record OAuthTokens(String accessToken, Instant expiresAt, String refreshToken, String clientId, String tokenEndpoint,
        String tokenEndpointAuth, String clientSecret, String scope) {
}
