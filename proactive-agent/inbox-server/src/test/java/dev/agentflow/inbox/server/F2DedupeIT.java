package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * F2: idempotent dedupe on {@code (inbox, source_event_id)}. A repeat is not an error: the producer
 * gets the earlier event back with {@code deduped=true}. Reusing a key for a different event is an
 * error and must be visible where it happens.
 */
class F2DedupeIT extends IntegrationTestBase {

    private static final String BODY = "{\"order_id\":\"1234\",\"amount\":299}";

    @Test
    void sameWebhookTenTimes_oneEvent() {
        WriteEventRequest request = webhook("order-1234-v7", BODY).build();
        List<WriteEventResponse> responses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            responses.add(acme.writeEvent("u42", request).require());
        }
        assertThat(responses).extracting(WriteEventResponse::eventId).containsOnly(responses.getFirst().eventId());
        assertThat(responses).filteredOn(WriteEventResponse::deduped).hasSize(9);
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
    }

    @RepeatedTest(5)
    void sameWebhookFromTenThreadsAtOnce_oneEvent() throws Exception {
        WriteEventRequest request = webhook("order-1234-v7", BODY).build();
        int threads = 10;
        CyclicBarrier start = new CyclicBarrier(threads);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<ApiResult<WriteEventResponse>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<ApiResult<WriteEventResponse>> call = () -> {
                    start.await();
                    return acme.writeEvent("u42", request);
                };
                futures.add(pool.submit(call));
            }
            List<WriteEventResponse> responses = new ArrayList<>();
            for (Future<ApiResult<WriteEventResponse>> future : futures) {
                ApiResult<WriteEventResponse> result = future.get();
                assertThat(result.status()).isEqualTo(202);
                responses.add(result.body());
            }
            assertThat(responses).extracting(WriteEventResponse::eventId).containsOnly(responses.getFirst().eventId());
            assertThat(responses).filteredOn(r -> !r.deduped()).as("exactly one request created the event").hasSize(1);
        }
        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event_transition")).isEqualTo(1);
    }

    @Test
    void sameKeyDifferentPayloadIs409_andTheFirstEventIsUntouched() {
        String eventId = acme.writeEvent("u42", webhook("order-1234-v7", BODY).build()).require().eventId();

        ApiResult<WriteEventResponse> reused = acme.writeEvent("u42",
                webhook("order-1234-v7", "{\"order_id\":\"1234\",\"amount\":300}").build());
        assertThat(reused.status()).isEqualTo(409);
        assertThat(reused.code()).isEqualTo(ProblemCodes.IDEMPOTENCY_CONFLICT);

        // the same key with another type is a different event as well
        ApiResult<WriteEventResponse> otherType = acme.writeEvent("u42",
                WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "order.refunded.v1")
                        .sourceEventId("order-1234-v7").payloadJson(BODY).build());
        assertThat(otherType.status()).isEqualTo(409);

        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
        assertThat(acme.getEvent("u42", eventId).require().event().payload().get("amount").intValue()).isEqualTo(299);
    }

    @Test
    void reorderedJsonIsTheSameEvent() {
        String first = acme.writeEvent("u42", webhook("k", "{\"a\":1,\"b\":{\"x\":true,\"y\":[1,2]}}").build())
                .require().eventId();
        WriteEventResponse again = acme.writeEvent("u42",
                webhook("k", "{ \"b\": { \"y\": [1, 2], \"x\": true }, \"a\": 1 }").build()).require();
        assertThat(again.deduped()).isTrue();
        assertThat(again.eventId()).isEqualTo(first);
    }

    @Test
    void sameKeyInAnotherInboxIsAnotherEvent() {
        String a = acme.writeEvent("u42", webhook("k", BODY).build()).require().eventId();
        WriteEventResponse b = acme.writeEvent("u43", webhook("k", BODY).build()).require();
        assertThat(b.deduped()).isFalse();
        assertThat(b.eventId()).isNotEqualTo(a);
    }

    @Test
    void missingKeyIsFingerprinted_identicalRetriesCollapse_differentPayloadsDoNot() {
        WriteEventResponse first = acme.writeEvent("u42", webhook(null, BODY).build()).require();
        assertThat(first.fingerprinted()).isTrue();
        assertThat(first.warning()).contains("idempotency key");
        assertThat(first.deduped()).isFalse();

        WriteEventResponse retry = acme.writeEvent("u42", webhook(null, BODY).build()).require();
        assertThat(retry.deduped()).isTrue();
        assertThat(retry.eventId()).isEqualTo(first.eventId());

        WriteEventResponse different = acme.writeEvent("u42", webhook(null, "{\"order_id\":\"9999\"}").build()).require();
        assertThat(different.deduped()).isFalse();
        assertThat(count("SELECT count(*) FROM event WHERE dedupe_kind = 'fingerprint'")).isEqualTo(2);
    }

    @Test
    void fingerprintUsesOccurredAtWhenGiven_otherwiseATimeBucket() {
        Instant occurred = Instant.parse("2026-11-20T09:59:00Z");
        String withTime = acme.writeEvent("u42", webhook(null, BODY).occurredAt(occurred).build()).require().eventId();
        clock.advance(Duration.ofHours(3));
        // hours later, the same occurrence is still the same event
        assertThat(acme.writeEvent("u42", webhook(null, BODY).occurredAt(occurred).build()).require().eventId())
                .isEqualTo(withTime);
        // a different occurrence is a different event
        assertThat(acme.writeEvent("u42", webhook(null, BODY).occurredAt(occurred.plusSeconds(1)).build())
                .require().deduped()).isFalse();

        // without occurred_at the arrival time is bucketed: a retry in the next bucket is a new event
        String bucketed = acme.writeEvent("u43", webhook(null, BODY).build()).require().eventId();
        assertThat(acme.writeEvent("u43", webhook(null, BODY).build()).require().eventId()).isEqualTo(bucketed);
        clock.advance(Duration.ofSeconds(61));
        assertThat(acme.writeEvent("u43", webhook(null, BODY).build()).require().deduped()).isFalse();
    }
}
