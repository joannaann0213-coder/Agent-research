package dev.agentflow.inbox.api;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Request signing shared by clients and the server.
 *
 * <p>The signed string is {@code v1\n{timestamp}\n{METHOD}\n{path?query}\n{hex sha256(body)}} and the
 * header value is {@code hmac-sha256=<hex>}. Binding the method, path and timestamp into the signature
 * stops a captured request from being replayed against another endpoint or at a later time.
 */
public final class HmacSigner {

    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String SCHEME = "hmac-sha256=";

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSigner() {
    }

    public static String stringToSign(long epochSeconds, String method, String pathWithQuery, byte[] body) {
        return "v1\n" + epochSeconds + "\n" + method.toUpperCase(Locale.ROOT) + "\n" + pathWithQuery + "\n"
                + Hashing.sha256Hex(body == null ? new byte[0] : body);
    }

    public static String sign(String secret, long epochSeconds, String method, String pathWithQuery, byte[] body) {
        return SCHEME + Hashing.hex(mac(secret, stringToSign(epochSeconds, method, pathWithQuery, body)));
    }

    /** Constant-time comparison of a presented header value with the expected signature. */
    public static boolean verify(String secret, String presented, long epochSeconds, String method,
            String pathWithQuery, byte[] body) {
        if (presented == null) {
            return false;
        }
        String expected = sign(secret, epochSeconds, method, pathWithQuery, body);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] mac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is required by every Java runtime", e);
        }
    }
}
