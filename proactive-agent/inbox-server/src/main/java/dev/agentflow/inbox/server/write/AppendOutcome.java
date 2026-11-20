package dev.agentflow.inbox.server.write;

import java.util.Arrays;

/**
 * What the append statement found.
 *
 * @param insertedId set when a new event row was written
 * @param existingId set when the idempotency key already had an event (or a deferred timer)
 * @param existingStatus state of that earlier event; {@code scheduled} for a deferred one
 */
public record AppendOutcome(String insertedId, Long seq, String existingId, byte[] existingHash, String existingStatus) {

    public boolean inserted() {
        return insertedId != null;
    }

    public boolean duplicate() {
        return insertedId == null && existingId != null;
    }

    public boolean samePayload(byte[] payloadHash) {
        return Arrays.equals(existingHash, payloadHash);
    }
}
