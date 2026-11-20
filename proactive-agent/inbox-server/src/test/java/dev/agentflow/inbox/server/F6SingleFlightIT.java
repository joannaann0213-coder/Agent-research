package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * F6: single flight and order. Fifty claimers hammer one inbox; at any moment at most one run is in
 * flight, nothing is skipped, and losing the race is a plain 204 that costs no attempt.
 */
class F6SingleFlightIT extends IntegrationTestBase {

    private static final int WORKERS = 50;
    private static final int EVENTS = 40;

    @Test
    void fiftyConcurrentClaimers_oneRunAtATime_inOrder_noEventSkipped() throws Exception {
        for (int i = 0; i < EVENTS; i++) {
            write("u42", webhook("order-" + i, "{\"n\":" + i + "}"));
        }

        Result result = hammer("u42");

        assertThat(result.maxInFlight.get()).as("runs in flight at the same time").isEqualTo(1);
        assertThat(result.headSeqs).as("events are handed out in arrival order, none skipped")
                .containsExactlyElementsOf(java.util.stream.LongStream.rangeClosed(1, EVENTS).boxed().toList());
        assertThat(result.busy.get()).as("most attempts lose the race, and that is fine").isPositive();
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(EVENTS);
        assertThat(count("SELECT count(*) FROM run WHERE status = 'done'")).isEqualTo(EVENTS);
        assertThat(count("SELECT count(*) FROM event WHERE attempt <> 1")).as("a 204 never costs an attempt").isZero();
        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'ack'")).isEqualTo(EVENTS);
    }

    @Test
    void strictMode_oneRunPerInboxAcrossAllLanes() throws Exception {
        Lane[] lanes = Lane.values();
        for (int i = 0; i < EVENTS; i++) {
            acme.writeEvent("u42", WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "order.paid.v1")
                    .sourceEventId("order-" + i).lane(lanes[i % lanes.length]).payloadJson("{}").build()).require();
        }
        jdbc.sql("UPDATE inbox SET max_active_runs = 1 WHERE inbox_id = 'u42'").update();

        Result result = hammer("u42");

        assertThat(result.maxInFlight.get()).isEqualTo(1);
        assertThat(result.headSeqs).hasSize(EVENTS).doesNotHaveDuplicates();
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done'")).isEqualTo(EVENTS);
    }

    private record Result(AtomicInteger maxInFlight, AtomicInteger busy, List<Long> headSeqs) {
    }

    private Result hammer(String inboxId) throws Exception {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        AtomicInteger busy = new AtomicInteger();
        AtomicInteger processed = new AtomicInteger();
        List<Long> headSeqs = Collections.synchronizedList(new ArrayList<>());
        CyclicBarrier start = new CyclicBarrier(WORKERS);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);

        try (ExecutorService pool = Executors.newFixedThreadPool(WORKERS)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int w = 0; w < WORKERS; w++) {
                String workerId = "worker-" + w;
                futures.add(pool.submit(() -> {
                    start.await();
                    while (processed.get() < EVENTS && System.nanoTime() < deadline) {
                        ApiResult<ClaimedRun> claimed = runtime.claim(ClaimRequest.inbox(workerId, ACME, inboxId));
                        if (claimed.status() == 204) {
                            busy.incrementAndGet();
                            Thread.onSpinWait();
                            continue;
                        }
                        ClaimedRun run = claimed.require();
                        maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                        headSeqs.add(run.events().getFirst().seq());
                        Thread.sleep(2); // hold the run for a moment so that overlap would show
                        inFlight.decrementAndGet();
                        runtime.ackDone(run).require();
                        processed.incrementAndGet();
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(90, TimeUnit.SECONDS);
            }
        }
        assertThat(processed.get()).isEqualTo(EVENTS);
        return new Result(maxInFlight, busy, headSeqs);
    }
}
