package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.support.StatementCounter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * F1: one write entry with a fast acknowledgement. 202 means accepted, not processed; the write path
 * only authenticates, validates, dedupes and appends.
 */
class F1WriteApiIT extends IntegrationTestBase {

    private static final String BODY = "{\"order_id\":\"1234\",\"amount\":299}";

    @Test
    void acceptsWithNoWorkerRunning_andStartsNoRun() {
        ApiResult<WriteEventResponse> result = acme.writeEvent("u42", webhook("order-1234-v7", BODY)
                .mergeKey("order:1234").build());

        assertThat(result.status()).isEqualTo(202);
        assertThat(result.body().deduped()).isFalse();
        assertThat(result.body().status()).isEqualTo("pending");
        assertThat(result.body().eventId()).hasSize(26);

        // Accepted, durable, and nothing has been executed: no run exists.
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM run")).isZero();
        assertThat(eventStatus(result.body().eventId())).isEqualTo("pending");

        EventDetail detail = acme.getEvent("u42", result.body().eventId()).require();
        assertThat(detail.event().seq()).isEqualTo(1);
        assertThat(detail.event().clientId()).isEqualTo("acme-bot");
        assertThat(detail.event().receivedAt()).isEqualTo(clock.now());
        assertThat(detail.event().readyAt()).isEqualTo(clock.now());
        assertThat(detail.transitions()).singleElement().satisfies(t -> {
            assertThat(t.fromStatus()).isNull();
            assertThat(t.toStatus()).isEqualTo("pending");
            assertThat(t.reason()).isEqualTo("accepted");
            assertThat(t.actor()).isEqualTo("client:acme-bot");
            assertThat(t.at()).isEqualTo(clock.now());
        });
    }

    @Test
    void retryThreeTimes_singleEvent() {
        WriteEventRequest request = webhook("order-1234-v7", BODY).build();
        Set<String> ids = new HashSet<>();
        for (int attempt = 0; attempt < 4; attempt++) {
            ApiResult<WriteEventResponse> result = acme.writeEvent("u42", request);
            assertThat(result.status()).isEqualTo(202);
            assertThat(result.body().deduped()).isEqualTo(attempt > 0);
            ids.add(result.body().eventId());
        }
        assertThat(ids).hasSize(1);
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'accepted'")).isEqualTo(1);
    }

    @Test
    void happyPathIsOneStatement() {
        acme.writeEvent("u42", webhook("warm-up", BODY).build()).require();

        StatementCounter.reset();
        acme.writeEvent("u42", webhook("order-1", BODY).build()).require();
        assertThat(StatementCounter.count()).as("a new event is one round trip").isEqualTo(1);

        StatementCounter.reset();
        acme.writeEvent("u42", webhook("order-1", BODY).build()).require();
        assertThat(StatementCounter.count()).as("a duplicate is one round trip too").isEqualTo(1);
    }

    @Test
    void sequenceIsPerInboxAndMonotonic() {
        String first = acme.writeEvent("u42", webhook("a", BODY).build()).require().eventId();
        String second = acme.writeEvent("u42", webhook("b", BODY).build()).require().eventId();
        String other = acme.writeEvent("u43", webhook("a", BODY).build()).require().eventId();

        assertThat(acme.getEvent("u42", first).require().event().seq()).isEqualTo(1);
        assertThat(acme.getEvent("u42", second).require().event().seq()).isEqualTo(2);
        assertThat(acme.getEvent("u43", other).require().event().seq()).isEqualTo(1);
    }

    @Test
    void databaseFailureIsAnExplicit503_neverADowngrade() {
        jdbc.sql("ALTER TABLE event RENAME TO event_offline").update();
        try {
            ApiResult<WriteEventResponse> result = acme.writeEvent("u42", webhook("order-1", BODY).build());
            assertThat(result.status()).isEqualTo(503);
            assertThat(result.code()).isEqualTo(ProblemCodes.DATABASE_UNAVAILABLE);
        } finally {
            jdbc.sql("ALTER TABLE event_offline RENAME TO event").update();
        }
        assertThat(count("SELECT count(*) FROM event")).isZero();

        // The producer retries with the same key and the event is accepted once.
        assertThat(acme.writeEvent("u42", webhook("order-1", BODY).build()).status()).isEqualTo(202);
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
    }

    @Test
    void rejectsInvalidRequestsWithoutStoringAnything() {
        // missing type
        assertThat(acme.send("POST", "/v1/inbox/u42/events",
                "{\"source\":\"biz_webhook\",\"payload\":{}}".getBytes(StandardCharsets.UTF_8), WriteEventResponse.class))
                .satisfies(r -> {
                    assertThat(r.status()).isEqualTo(400);
                    assertThat(r.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
                });
        // a misspelt field must not be silently ignored (it would make the event lose its idempotency key)
        assertThat(acme.send("POST", "/v1/inbox/u42/events",
                "{\"source\":\"biz_webhook\",\"type\":\"t.v1\",\"payload\":{},\"source_event_idd\":\"x\"}"
                        .getBytes(StandardCharsets.UTF_8), WriteEventResponse.class).status()).isEqualTo(400);
        // malformed JSON
        assertThat(acme.send("POST", "/v1/inbox/u42/events", "{not json".getBytes(StandardCharsets.UTF_8),
                WriteEventResponse.class).status()).isEqualTo(400);
        // unknown enum value
        assertThat(acme.send("POST", "/v1/inbox/u42/events",
                "{\"source\":\"carrier_pigeon\",\"type\":\"t.v1\",\"payload\":{}}".getBytes(StandardCharsets.UTF_8),
                WriteEventResponse.class).status()).isEqualTo(400);
        // bad inbox id
        assertThat(acme.writeEvent("bad id!", webhook("k", BODY).build()).status()).isEqualTo(400);
        // reserved key prefix
        assertThat(acme.writeEvent("u42", webhook("timer:abc", BODY).build()).status()).isEqualTo(400);

        assertThat(count("SELECT count(*) FROM event")).isZero();
        assertThat(count("SELECT count(*) FROM inbox")).isZero();
    }

    @Test
    void payloadOver64KbIs413() {
        String big = "{\"blob\":\"" + "x".repeat(64 * 1024) + "\"}";
        ApiResult<WriteEventResponse> result = acme.writeEvent("u42", webhook("big", big).build());
        assertThat(result.status()).isEqualTo(413);
        assertThat(result.code()).isEqualTo(ProblemCodes.PAYLOAD_TOO_LARGE);
        assertThat(count("SELECT count(*) FROM event")).isZero();

        String justUnder = "{\"blob\":\"" + "x".repeat(64 * 1024 - 20) + "\"}";
        assertThat(acme.writeEvent("u42", webhook("fits", justUnder).build()).status()).isEqualTo(202);
    }
}
