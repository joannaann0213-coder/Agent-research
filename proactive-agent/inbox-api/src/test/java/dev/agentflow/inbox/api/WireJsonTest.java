package dev.agentflow.inbox.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class WireJsonTest {

    @Test
    void requestUsesSnakeCaseLowerCaseEnumsAndIsoInstants() {
        WriteEventRequest request = WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "order.paid.v1")
                .sourceEventId("order-1234-v7")
                .occurredAt(Instant.parse("2026-11-20T10:00:00Z"))
                .mergeKey("order:1234")
                .lane(Lane.INTERACTIVE)
                .payloadJson("{\"order_id\":\"1234\",\"amount\":299}")
                .build();

        String json = InboxJson.mapper().writeValueAsString(request);
        JsonNode tree = InboxJson.mapper().readTree(json);

        assertThat(tree.get("source").stringValue()).isEqualTo("biz_webhook");
        assertThat(tree.get("source_event_id").stringValue()).isEqualTo("order-1234-v7");
        assertThat(tree.get("occurred_at").stringValue()).isEqualTo("2026-11-20T10:00:00Z");
        assertThat(tree.get("lane").stringValue()).isEqualTo("interactive");
        assertThat(tree.has("ready_at")).isFalse();

        WriteEventRequest back = InboxJson.mapper().readValue(json, WriteEventRequest.class);
        assertThat(back).isEqualTo(request);
    }

    @Test
    void laneDefaultsFollowTheProducer() {
        assertThat(Lane.defaultFor(EventSource.CONVERSATION)).isEqualTo(Lane.INTERACTIVE);
        assertThat(Lane.defaultFor(EventSource.AGENT_SELF)).isEqualTo(Lane.BACKGROUND);
        assertThat(Lane.defaultFor(EventSource.RULE)).isEqualTo(Lane.BATCH);
    }
}
