package dev.agentflow.agent.api;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The signature of a webhook delivery, for the service that signs and for a receiver that checks.
 *
 * <p>A delivery carries three headers: {@code webhook-id}, the update's id; {@code webhook-timestamp},
 * seconds since the epoch; and {@code webhook-signature}, {@code v1,} followed by the Base64 of
 * HMAC-SHA256 over {@code <id>.<timestamp>.<body>}, keyed with the secret's bytes: the part of the
 * secret after {@code whsec_}, Base64-decoded.
 */
public final class WebhookSignature {

    public static final String SECRET_PREFIX = "whsec_";
    public static final String ID = "webhook-id";
    public static final String TIMESTAMP = "webhook-timestamp";
    public static final String SIGNATURE = "webhook-signature";

    private WebhookSignature() {
    }

    /** The value of the {@code webhook-signature} header. */
    public static String sign(String secret, String id, long timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key(secret), "HmacSHA256"));
            mac.update((id + "." + timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return "v1," + Base64.getEncoder().encodeToString(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    /**
     * Whether a delivery is genuine and recent.
     *
     * @param tolerance how far the timestamp may be from {@code now}; five minutes is usual
     */
    public static boolean verify(String secret, String id, String timestamp, byte[] body, String signature, Instant now,
            Duration tolerance) {
        long seconds;
        try {
            seconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Duration.between(Instant.ofEpochSecond(seconds), now).abs().compareTo(tolerance) > 0) {
            return false;
        }
        byte[] expected = sign(secret, id, seconds, body).getBytes(StandardCharsets.UTF_8);
        // The header may carry several signatures, separated by spaces, while a secret is being rotated.
        for (String candidate : signature.split(" ")) {
            if (MessageDigest.isEqual(expected, candidate.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    private static byte[] key(String secret) {
        if (!secret.startsWith(SECRET_PREFIX)) {
            throw new IllegalArgumentException("a signing secret starts with " + SECRET_PREFIX);
        }
        return Base64.getDecoder().decode(secret.substring(SECRET_PREFIX.length()));
    }
}
