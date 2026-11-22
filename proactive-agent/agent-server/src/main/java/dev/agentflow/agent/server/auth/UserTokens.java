package dev.agentflow.agent.server.auth;

import dev.agentflow.agent.api.IssuedToken;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.time.AgentClock;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Tokens for end users. An application's back end asks for one and hands it to the user's client,
 * which can then call the Agent API directly, for that user only.
 *
 * <p>A token reads {@code aft.<claims>.<signature>}. The claims name the application, the user, the
 * expiry and the user's token epoch; the signature is an HMAC-SHA256 over them with a key derived
 * from the service's signing secret and the application id. Nothing is stored per token. A user's
 * tokens are revoked together by raising the user's epoch, which is why verifying a token reads the
 * user's row.
 */
@Service
public class UserTokens {

    public static final String PREFIX = "aft.";

    /** What a valid token says. */
    public record Verified(String appId, String userId, Instant expiresAt) {
    }

    private static final String VERSION = "v1";
    private static final int MIN_SECRET_LENGTH = 32;

    private final List<byte[]> secrets;
    private final AgentProperties.Tokens config;
    private final AgentClock clock;
    private final Users users;

    public UserTokens(AgentProperties properties, AgentClock clock, Users users) {
        this.config = properties.getTokens();
        this.clock = clock;
        this.users = users;
        if (config.getSecrets().isEmpty()) {
            throw new IllegalStateException("agent.tokens.secrets is not set: it holds the secret that signs user "
                    + "tokens (environment variable AGENT_TOKENS_SECRETS_0)");
        }
        for (String secret : config.getSecrets()) {
            if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
                throw new IllegalStateException("every entry of agent.tokens.secrets needs at least "
                        + MIN_SECRET_LENGTH + " characters");
            }
        }
        this.secrets = config.getSecrets().stream().map(secret -> secret.getBytes(StandardCharsets.UTF_8)).toList();
    }

    /**
     * @param ttlSeconds the lifetime asked for; {@code null} for the default
     */
    public IssuedToken issue(String appId, String userId, Long ttlSeconds) {
        Duration ttl = ttlSeconds == null ? config.getDefaultTtl() : Duration.ofSeconds(ttlSeconds);
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(config.getMaxTtl()) > 0) {
            throw AgentException.invalid("ttl_seconds must be between 1 and " + config.getMaxTtl().toSeconds());
        }
        int epoch = users.ensure(appId, userId).tokenEpoch();
        Instant expiresAt = clock.now().plus(ttl);
        String claims = String.join("|", VERSION, appId, userId, Long.toString(expiresAt.getEpochSecond()),
                Integer.toString(epoch));
        byte[] claimBytes = claims.getBytes(StandardCharsets.UTF_8);
        Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
        String token = PREFIX + base64.encodeToString(claimBytes) + "."
                + base64.encodeToString(sign(secrets.getFirst(), appId, claimBytes));
        return new IssuedToken(token, Instant.ofEpochSecond(expiresAt.getEpochSecond()));
    }

    /** Invalidates every token of the user that is out. */
    public void revokeAll(String appId, String userId) {
        users.raiseTokenEpoch(appId, userId);
    }

    /** What a presented token says, or empty when it is malformed, forged, expired or revoked. */
    public Optional<Verified> verify(String presented) {
        if (!presented.startsWith(PREFIX)) {
            return Optional.empty();
        }
        int dot = presented.indexOf('.', PREFIX.length());
        if (dot < 0) {
            return Optional.empty();
        }
        byte[] claimBytes;
        byte[] signature;
        try {
            claimBytes = Base64.getUrlDecoder().decode(presented.substring(PREFIX.length(), dot));
            signature = Base64.getUrlDecoder().decode(presented.substring(dot + 1));
        } catch (IllegalArgumentException notBase64) {
            return Optional.empty();
        }
        String[] claims = new String(claimBytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (claims.length != 5 || !VERSION.equals(claims[0])) {
            return Optional.empty();
        }
        String appId = claims[1];
        String userId = claims[2];
        boolean genuine = false;
        for (byte[] secret : secrets) {
            genuine |= MessageDigest.isEqual(signature, sign(secret, appId, claimBytes));
        }
        if (!genuine) {
            return Optional.empty();
        }
        // The claims are ours from here on.
        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(claims[3]));
        if (!clock.now().isBefore(expiresAt)) {
            return Optional.empty();
        }
        Optional<Users.User> user = users.find(appId, userId);
        if (user.isEmpty() || user.get().tokenEpoch() != Integer.parseInt(claims[4])) {
            return Optional.empty();
        }
        return Optional.of(new Verified(appId, userId, expiresAt));
    }

    /** One signing key per application, so that a token can never be replayed against another one. */
    private static byte[] sign(byte[] secret, String appId, byte[] claims) {
        byte[] appKey = hmac(secret, ("user-token:" + appId).getBytes(StandardCharsets.UTF_8));
        return hmac(appKey, claims);
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 is required by every Java runtime", e);
        }
    }
}
