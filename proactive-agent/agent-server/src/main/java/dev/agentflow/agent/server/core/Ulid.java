package dev.agentflow.agent.server.core;

import java.security.SecureRandom;

/**
 * ULID generator: 48 bits of milliseconds and 80 random bits in 26 Crockford base-32 characters.
 * Ids from one generator are strictly increasing, also within a millisecond and when the clock steps
 * back. Ids from several service instances are only roughly time-ordered, so nothing relies on their
 * order across instances.
 */
public final class Ulid {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private long lastMillis = -1;
    private long randomHigh; // upper 16 bits of the random part
    private long randomLow;  // lower 64 bits of the random part

    public synchronized String next(long epochMillis) {
        if (epochMillis > lastMillis) {
            lastMillis = epochMillis;
            randomHigh = random.nextInt(1 << 16);
            randomLow = random.nextLong();
        } else {
            randomLow++;
            if (randomLow == 0) {
                randomHigh = (randomHigh + 1) & 0xFFFF;
            }
        }
        char[] out = new char[26];
        long time = lastMillis;
        for (int i = 9; i >= 0; i--) {
            out[i] = ALPHABET[(int) (time & 31)];
            time >>>= 5;
        }
        long high = randomHigh;
        long low = randomLow;
        for (int i = 25; i >= 10; i--) {
            out[i] = ALPHABET[(int) (low & 31)];
            low = (low >>> 5) | (high << 59);
            high >>>= 5;
        }
        return new String(out);
    }
}
