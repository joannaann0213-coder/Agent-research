package dev.agentflow.inbox.client;

/**
 * Something a run wants the user to receive.
 *
 * @param sequence position of the message within its run, starting at 1
 * @param channel where it goes, for example {@code im} or {@code approval}
 */
public record OutboxMessage(String runId, int sequence, String channel, String traceId, String text) {

    /**
     * The key that makes delivery idempotent. Because a re-issued run keeps its id and replays the same
     * steps, a message sent before a crash and again after it has the same key and is sent once.
     */
    public String dedupeKey() {
        return runId + ":" + sequence;
    }
}
