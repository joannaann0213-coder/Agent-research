package dev.agentflow.inbox.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 helpers. */
public final class Hashing {

    private static final HexFormat HEX = HexFormat.of();

    private Hashing() {
    }

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }

    public static byte[] sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String hex(byte[] bytes) {
        return HEX.formatHex(bytes);
    }

    public static String sha256Hex(byte[] data) {
        return hex(sha256(data));
    }
}
