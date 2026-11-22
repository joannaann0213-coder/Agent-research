package dev.agentflow.agent.server.auth;

import dev.agentflow.agent.api.CreatedKey;
import dev.agentflow.agent.api.KeyView;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.Hashing;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * API keys of applications. A key reads {@code afk.<key id>.<secret>}: the id finds the row, the
 * secret is compared with the stored hash. The secret is 256 random bits, so a plain SHA-256 is
 * enough; there is nothing to brute-force.
 */
@Service
public class ApiKeys {

    public static final String PREFIX = "afk.";

    /** What a valid key says about its owner. */
    public record Verified(String keyId, String appId, boolean appActive) {
    }

    private static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(1);

    private final JdbcClient jdbc;
    private final AgentClock clock;
    private final Ids ids;
    private final SecureRandom random = new SecureRandom();

    public ApiKeys(JdbcClient jdbc, AgentClock clock, Ids ids) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.ids = ids;
    }

    public CreatedKey create(String appId, String label) {
        if (label != null && label.length() > 200) {
            throw AgentException.invalid("label has at most 200 characters");
        }
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String secretText = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String keyId = ids.next("key");
        Instant now = clock.now();
        jdbc.sql("""
                INSERT INTO api_key (key_id, app_id, secret_hash, label, created_at)
                VALUES (:keyId, :appId, :hash, :label, :now)
                """)
                .param("keyId", keyId).param("appId", appId).param("hash", Hashing.sha256(secretText))
                .param("label", label).param("now", Db.ts(now))
                .update();
        return new CreatedKey(keyId, PREFIX + keyId + "." + secretText, label, now);
    }

    public List<KeyView> list(String appId) {
        return jdbc.sql("""
                SELECT key_id, label, created_at, last_used_at, revoked_at
                  FROM api_key WHERE app_id = :appId ORDER BY created_at, key_id
                """)
                .param("appId", appId)
                .query((rs, row) -> new KeyView(rs.getString("key_id"), rs.getString("label"),
                        Db.instant(rs, "created_at"), Db.instant(rs, "last_used_at"), Db.instant(rs, "revoked_at")))
                .list();
    }

    /** @return whether a key of that application was revoked by this call or before */
    public boolean revoke(String appId, String keyId) {
        return jdbc.sql("""
                UPDATE api_key SET revoked_at = COALESCE(revoked_at, :now)
                 WHERE app_id = :appId AND key_id = :keyId
                """)
                .param("now", Db.ts(clock.now())).param("appId", appId).param("keyId", keyId)
                .update() == 1;
    }

    /** The owner of a presented key, or empty when the key is unknown, wrong or revoked. */
    public Optional<Verified> verify(String presented) {
        if (!presented.startsWith(PREFIX)) {
            return Optional.empty();
        }
        int dot = presented.indexOf('.', PREFIX.length());
        if (dot < 0) {
            return Optional.empty();
        }
        String keyId = presented.substring(PREFIX.length(), dot);
        byte[] presentedHash = Hashing.sha256(presented.substring(dot + 1));
        Instant now = clock.now();
        return jdbc.sql("""
                SELECT k.app_id, k.secret_hash, k.last_used_at, a.status
                  FROM api_key k JOIN app a USING (app_id)
                 WHERE k.key_id = :keyId AND k.revoked_at IS NULL
                """)
                .param("keyId", keyId)
                .query((rs, row) -> {
                    if (!MessageDigest.isEqual(presentedHash, rs.getBytes("secret_hash"))) {
                        return null;
                    }
                    Instant lastUsed = Db.instant(rs, "last_used_at");
                    if (lastUsed == null || lastUsed.isBefore(now.minus(LAST_USED_RESOLUTION))) {
                        jdbc.sql("UPDATE api_key SET last_used_at = :now WHERE key_id = :keyId")
                                .param("now", Db.ts(now)).param("keyId", keyId).update();
                    }
                    return new Verified(keyId, rs.getString("app_id"), "active".equals(rs.getString("status")));
                })
                .optional();
    }
}
