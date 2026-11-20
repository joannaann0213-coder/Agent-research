package dev.agentflow.inbox.api;

/**
 * Answer to an accepted write. 202 means accepted, not processed.
 *
 * @param eventId id of the event (pre-allocated when the event is deferred)
 * @param deduped true when this key had been accepted before; the earlier event is returned
 * @param status {@code pending}, a later state when deduped, or {@code scheduled} for a deferred event
 * @param fingerprinted true when no {@code source_event_id} was sent and the server derived one
 * @param warning advice for the producer, for example to send an explicit idempotency key
 */
public record WriteEventResponse(String eventId, boolean deduped, String status, Boolean fingerprinted,
        String warning) {
}
