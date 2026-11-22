package dev.agentflow.agent.server.credentials;

import dev.agentflow.agent.api.McpCredentialRequest;
import dev.agentflow.agent.api.McpCredentialView;
import dev.agentflow.agent.api.OAuthTokens;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.McpCredential;
import dev.agentflow.agent.cma.McpUrls;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.time.AgentClock;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The credentials of the MCP servers that agents use: the application's, which every session of
 * the application has, and each user's own, which only the user's sessions have and which come
 * first. The secrets go straight to vaults on the agent platform; this service never stores them
 * and cannot show them. A session picks up a changed token at its next call, and new vaults at the
 * next turn of its line, which then starts a new session.
 */
@Service
public class Credentials {

    private static final Logger log = LoggerFactory.getLogger(Credentials.class);

    public static final String BEARER = "bearer";
    public static final String OAUTH = "oauth";

    private static final Set<String> TOKEN_ENDPOINT_AUTH = Set.of("none", "client_secret_basic", "client_secret_post");
    private static final int MAX_SECRET = 8000;

    private record Row(String appId, String userId, String name, String mcpServerUrl, String kind, String credentialId,
            Instant createdAt, Instant updatedAt) {

        McpCredentialView view() {
            return new McpCredentialView(name, mcpServerUrl, kind, userId.isEmpty() ? null : userId, createdAt, updatedAt);
        }
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final ManagedAgents cma;
    private final AgentClock clock;

    public Credentials(JdbcClient jdbc, TransactionTemplate tx, ManagedAgents cma, AgentClock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.cma = cma;
        this.clock = clock;
    }

    /**
     * Stores a credential under a name, or replaces the one that has it.
     *
     * @param userId the user whose credential it is; {@code null} for the application's
     */
    public McpCredentialView put(String appId, String userId, String name, McpCredentialRequest request) {
        Names.localId(name, "the credential's name");
        if (userId != null) {
            Names.userId(userId);
        }
        McpCredential credential = credential(name, request);
        String kind = credential.bearerToken() != null ? BEARER : OAUTH;
        String owner = userId == null ? "" : userId;
        return tx.execute(status -> {
            // One change at a time per vault: the platform keeps one credential per server in it.
            jdbc.sql("SELECT pg_advisory_xact_lock(hashtext(:key))").param("key", "vault:" + appId + "/" + owner)
                    .query(Object.class).single();
            Optional<Row> existing = find(appId, owner, name);
            Optional<Row> sameServer = rows(appId, owner).stream()
                    .filter(row -> !row.name().equals(name) && McpUrls.same(row.mcpServerUrl(), credential.mcpServerUrl()))
                    .findFirst();
            if (sameServer.isPresent()) {
                throw AgentException.conflict("credential " + sameServer.get().name() + " is for this server already; "
                        + "replace that one, or delete it first");
            }
            String vaultId = vault(appId, owner);
            Instant now = clock.now();
            String credentialId;
            try {
                if (existing.isPresent() && BEARER.equals(kind) && BEARER.equals(existing.get().kind())
                        && McpUrls.same(existing.get().mcpServerUrl(), credential.mcpServerUrl())) {
                    // Sessions that have the vault use the new token from their next call on.
                    cma.updateCredential(vaultId, existing.get().credentialId(), credential.bearerToken());
                    credentialId = existing.get().credentialId();
                } else {
                    if (existing.isPresent()) {
                        archive(vaultId, existing.get().credentialId());
                    }
                    credentialId = create(vaultId, credential);
                }
            } catch (CmaException e) {
                throw platform(e);
            }
            jdbc.sql("""
                    INSERT INTO mcp_credential (app_id, user_id, name, mcp_server_url, kind, cma_credential_id, created_at, updated_at)
                    VALUES (:appId, :userId, :name, :url, :kind, :credentialId, :now, :now)
                    ON CONFLICT (app_id, user_id, name) DO UPDATE SET mcp_server_url = EXCLUDED.mcp_server_url, kind = EXCLUDED.kind,
                           cma_credential_id = EXCLUDED.cma_credential_id, updated_at = EXCLUDED.updated_at
                    """)
                    .param("appId", appId).param("userId", owner).param("name", name).param("url", credential.mcpServerUrl())
                    .param("kind", kind).param("credentialId", credentialId).param("now", Db.ts(now))
                    .update();
            return find(appId, owner, name).orElseThrow().view();
        });
    }

    public List<McpCredentialView> list(String appId, String userId) {
        if (userId != null) {
            Names.userId(userId);
        }
        return rows(appId, userId == null ? "" : userId).stream().map(Row::view).toList();
    }

    /** @return whether there was one */
    public boolean delete(String appId, String userId, String name) {
        Names.localId(name, "the credential's name");
        if (userId != null) {
            Names.userId(userId);
        }
        String owner = userId == null ? "" : userId;
        return Boolean.TRUE.equals(tx.execute(status -> {
            jdbc.sql("SELECT pg_advisory_xact_lock(hashtext(:key))").param("key", "vault:" + appId + "/" + owner)
                    .query(Object.class).single();
            Optional<Row> existing = find(appId, owner, name);
            if (existing.isEmpty()) {
                return false;
            }
            try {
                archive(vault(appId, owner), existing.get().credentialId());
            } catch (CmaException e) {
                throw platform(e);
            }
            jdbc.sql("DELETE FROM mcp_credential WHERE app_id = :appId AND user_id = :userId AND name = :name")
                    .param("appId", appId).param("userId", owner).param("name", name)
                    .update();
            return true;
        }));
    }

    /** The vaults a new session of the user gets: the user's own first, so that it wins, then the application's. */
    public List<String> vaultsFor(String appId, String userId) {
        List<String> vaults = new ArrayList<>();
        existingVault(appId, userId).ifPresent(vaults::add);
        existingVault(appId, "").ifPresent(vaults::add);
        return vaults;
    }

    // ================================================================ the platform

    private String vault(String appId, String owner) {
        Optional<String> existing = existingVault(appId, owner);
        if (existing.isPresent()) {
            return existing.get();
        }
        String vaultId;
        try {
            vaultId = cma.createVault(owner.isEmpty() ? appId : appId + " / " + owner,
                    owner.isEmpty() ? Map.of("app", appId) : Map.of("app", appId, "user", owner));
        } catch (CmaException e) {
            throw platform(e);
        }
        jdbc.sql("INSERT INTO vault (app_id, user_id, cma_vault_id, created_at) VALUES (:appId, :userId, :vaultId, :now)")
                .param("appId", appId).param("userId", owner).param("vaultId", vaultId).param("now", Db.ts(clock.now()))
                .update();
        return vaultId;
    }

    private Optional<String> existingVault(String appId, String owner) {
        return jdbc.sql("SELECT cma_vault_id FROM vault WHERE app_id = :appId AND user_id = :userId")
                .param("appId", appId).param("userId", owner)
                .query(String.class).optional();
    }

    /**
     * Stores the credential in the vault. A credential for the same server that the vault has and
     * this service does not know is what an earlier attempt left when its answer was lost: it goes.
     */
    private String create(String vaultId, McpCredential credential) {
        try {
            return cma.createCredential(vaultId, credential);
        } catch (CmaException e) {
            if (e.kind() != CmaException.Kind.CONFLICT) {
                throw e;
            }
            for (McpCredential.Stored stored : cma.listCredentials(vaultId)) {
                if (!stored.archived() && stored.mcpServerUrl() != null && McpUrls.same(stored.mcpServerUrl(), credential.mcpServerUrl())) {
                    log.warn("vault {} had a credential for {} that this service did not know; it is replaced", vaultId,
                            credential.mcpServerUrl());
                    cma.archiveCredential(vaultId, stored.id());
                }
            }
            return cma.createCredential(vaultId, credential);
        }
    }

    private void archive(String vaultId, String credentialId) {
        try {
            cma.archiveCredential(vaultId, credentialId);
        } catch (CmaException e) {
            if (e.kind() != CmaException.Kind.NOT_FOUND) {
                throw e;
            }
        }
    }

    private static AgentException platform(CmaException e) {
        return switch (e.kind()) {
            case INVALID_REQUEST -> AgentException.invalid("the agent platform refused the credential: " + e.getMessage());
            case CONFLICT -> AgentException.conflict("the agent platform refused the credential: " + e.getMessage());
            default -> AgentException.unavailable("the agent platform is not available right now; nothing was changed, try again");
        };
    }

    // ================================================================ checks

    private static McpCredential credential(String name, McpCredentialRequest request) {
        if (request == null) {
            throw AgentException.invalid("a request body is required");
        }
        if (!https(request.mcpServerUrl())) {
            throw AgentException.invalid("mcp_server_url must be the https URL of the server");
        }
        if ((request.token() == null) == (request.oauth() == null)) {
            throw AgentException.invalid("a credential has a token or oauth, exactly one of them");
        }
        if (request.token() != null) {
            secret(request.token(), "token");
            return new McpCredential(request.mcpServerUrl(), name, request.token(), null);
        }
        OAuthTokens oauth = request.oauth();
        secret(oauth.accessToken(), "oauth.access_token");
        McpCredential.Refresh refresh = null;
        if (oauth.refreshToken() != null) {
            secret(oauth.refreshToken(), "oauth.refresh_token");
            if (oauth.clientId() == null || oauth.clientId().isBlank() || !https(oauth.tokenEndpoint())) {
                throw AgentException.invalid("a refresh token needs oauth.client_id and oauth.token_endpoint, an https URL");
            }
            String how = oauth.tokenEndpointAuth() == null ? "none" : oauth.tokenEndpointAuth();
            if (!TOKEN_ENDPOINT_AUTH.contains(how)) {
                throw AgentException.invalid("oauth.token_endpoint_auth is none, client_secret_basic or client_secret_post");
            }
            if (how.equals("none") != (oauth.clientSecret() == null)) {
                throw AgentException.invalid("oauth.client_secret goes with client_secret_basic and client_secret_post, and only with them");
            }
            refresh = new McpCredential.Refresh(oauth.refreshToken(), oauth.clientId(), oauth.tokenEndpoint(), how,
                    oauth.clientSecret(), oauth.scope());
        } else if (oauth.clientId() != null || oauth.tokenEndpoint() != null || oauth.clientSecret() != null) {
            throw AgentException.invalid("oauth.client_id, token_endpoint and client_secret go with a refresh_token");
        }
        return new McpCredential(request.mcpServerUrl(), name, null, new McpCredential.OAuth(oauth.accessToken(), oauth.expiresAt(), refresh));
    }

    private static void secret(String value, String what) {
        if (value == null || value.isBlank() || value.length() > MAX_SECRET) {
            throw AgentException.invalid(what + " is required and has at most " + MAX_SECRET + " characters");
        }
    }

    private static boolean https(String url) {
        if (url == null || url.length() > 2000) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    // ================================================================ rows

    private Optional<Row> find(String appId, String owner, String name) {
        return jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId AND name = :name")
                .param("appId", appId).param("userId", owner).param("name", name)
                .query(Credentials::row)
                .optional();
    }

    private List<Row> rows(String appId, String owner) {
        return jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId ORDER BY name")
                .param("appId", appId).param("userId", owner)
                .query(Credentials::row)
                .list();
    }

    private static final String SELECT = """
            SELECT app_id, user_id, name, mcp_server_url, kind, cma_credential_id, created_at, updated_at FROM mcp_credential
            """;

    private static Row row(ResultSet rs, int rowNumber) throws SQLException {
        return new Row(rs.getString("app_id"), rs.getString("user_id"), rs.getString("name"), rs.getString("mcp_server_url"),
                rs.getString("kind"), rs.getString("cma_credential_id"), Db.instant(rs, "created_at"), Db.instant(rs, "updated_at"));
    }
}
