package dev.agentflow.inbox.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Body of {@code POST /v1/inbox/{inbox_id}/events}.
 *
 * @param source who produced the event (required)
 * @param sourceEventId idempotency key; when absent the server fingerprints the event
 * @param type semantic type with namespace and version, for example {@code order.paid.v1} (required)
 * @param occurredAt when it happened at the source
 * @param readyAt when it may run; absent or past means now, a future value defers the event
 * @param mergeKey batching key; events with the same key are handled in one run
 * @param lane priority channel; defaults by source
 * @param payload the event body, at most 64 KB (required)
 * @param resumeKey set on a reply that should wake a suspended run
 * @param interrupt asks the run that is handling this key to yield
 * @param traceId passed through to the runtime; generated when absent
 */
public record WriteEventRequest(
        EventSource source,
        String sourceEventId,
        String type,
        Instant occurredAt,
        Instant readyAt,
        String mergeKey,
        Lane lane,
        JsonNode payload,
        String resumeKey,
        Boolean interrupt,
        String traceId) {

    public static Builder builder(EventSource source, String type) {
        return new Builder(source, type);
    }

    public static final class Builder {
        private final EventSource source;
        private final String type;
        private String sourceEventId;
        private Instant occurredAt;
        private Instant readyAt;
        private String mergeKey;
        private Lane lane;
        private JsonNode payload = InboxJson.mapper().createObjectNode();
        private String resumeKey;
        private Boolean interrupt;
        private String traceId;

        private Builder(EventSource source, String type) {
            this.source = source;
            this.type = type;
        }

        public Builder sourceEventId(String value) {
            this.sourceEventId = value;
            return this;
        }

        public Builder occurredAt(Instant value) {
            this.occurredAt = value;
            return this;
        }

        public Builder readyAt(Instant value) {
            this.readyAt = value;
            return this;
        }

        public Builder mergeKey(String value) {
            this.mergeKey = value;
            return this;
        }

        public Builder lane(Lane value) {
            this.lane = value;
            return this;
        }

        public Builder payload(JsonNode value) {
            this.payload = value;
            return this;
        }

        /** Parses {@code json} as the payload. */
        public Builder payloadJson(String json) {
            this.payload = InboxJson.mapper().readTree(json);
            return this;
        }

        public Builder resumeKey(String value) {
            this.resumeKey = value;
            return this;
        }

        public Builder interrupt(Boolean value) {
            this.interrupt = value;
            return this;
        }

        public Builder traceId(String value) {
            this.traceId = value;
            return this;
        }

        public WriteEventRequest build() {
            return new WriteEventRequest(source, sourceEventId, type, occurredAt, readyAt, mergeKey, lane, payload,
                    resumeKey, interrupt, traceId);
        }
    }
}
