package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.InboxStatus;
import dev.agentflow.inbox.api.TransitionView;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.MemoryOutbox;
import dev.agentflow.inbox.client.OutboxMessage;
import dev.agentflow.inbox.client.ScriptedWorker;
import dev.agentflow.inbox.client.ScriptedWorkerMain;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Scenario A: a business alert, one reply. The whole path over real HTTP: a webhook writes an event,
 * a waiting worker is woken and claims it, works for longer than one lease, sends exactly one message
 * through the outbox, and acks. The scripted worker stands in for the agent.
 */
class ScenarioAE2E extends IntegrationTestBase {

    @Autowired
    LeaseSweeper leaseSweeper;

    @Test
    void alertIsWrittenClaimedWorkedOnAnsweredOnceAndAcked() throws Exception {
        MemoryOutbox outbox = new MemoryOutbox();
        ScriptedWorker worker = new ScriptedWorker(runtime, outbox, "worker-1", context -> {
            assertThat(context.events()).hasSize(1);
            // Stand-in for "open a thread, run two work agents, aggregate": 100 s of work,
            // which only survives because the lease is renewed along the way.
            for (int step = 0; step < 2; step++) {
                clock.advance(Duration.ofSeconds(50));
                assertThat(context.heartbeat().extended()).isTrue();
                assertThat(leaseSweeper.runOnce()).isZero();
            }
            context.send("im", "Order 1234 is 40 minutes late. The courier was reassigned; new ETA 18:20.");
            return ScriptedWorker.Decision.done();
        });

        WriteEventResponse accepted;
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            // The worker is already waiting when the alert arrives.
            Future<Boolean> processed = pool.submit(() -> worker.runOnce(ClaimRequest.any("worker-1").withWait(20_000)));
            Thread.sleep(300);

            accepted = acme.writeEvent("u42", WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "order.fulfilment.timeout.v1")
                    .sourceEventId("order-1234-v7").mergeKey("order:1234").traceId("trace-abc")
                    .payloadJson("{\"order_id\":\"1234\",\"late_minutes\":40}").build()).require();
            assertThat(accepted.status()).isEqualTo("pending");
            clock.advance(Duration.ofMillis(1500)); // the quiet window for order:1234 closes

            assertThat(processed.get(20, TimeUnit.SECONDS)).isTrue();
        }

        // one run, one message, carrying the trace id the producer sent
        EventDetail event = acme.getEvent("u42", accepted.eventId()).require();
        assertThat(event.event().status()).isEqualTo("done");
        assertThat(event.transitions()).extracting(TransitionView::reason).containsExactly("accepted", "claim", "ack");
        assertThat(outbox.delivered()).singleElement().satisfies(message -> {
            assertThat(message.runId()).isEqualTo(event.event().runId());
            assertThat(message.sequence()).isEqualTo(1);
            assertThat(message.traceId()).isEqualTo("trace-abc");
            assertThat(message.text()).startsWith("Order 1234 is 40 minutes late");
        });
        assertThat(event.event().traceId()).isEqualTo("trace-abc");
        assertThat(runtime.getRun(event.event().runId()).require().traceId()).isEqualTo("trace-abc");

        // the inbox has released its run
        InboxStatus status = ops.inboxStatus(ACME, "u42").require();
        assertThat(status.activeRuns()).isZero();
        assertThat(status.pending()).isZero();
        assertThat(worker.runOnce()).as("nothing left to do").isFalse();
    }

    /** F2 acceptance check, end to end: one webhook replayed ten times is one event, one run, one reply. */
    @Test
    void sameWebhookTenTimes_oneEventOneRunOneReply() {
        WriteEventRequest webhook = webhook("order-1234-v7", "{\"order_id\":\"1234\"}").build();
        for (int delivery = 0; delivery < 10; delivery++) {
            assertThat(acme.writeEvent("u42", webhook).status()).isEqualTo(202);
        }
        MemoryOutbox outbox = new MemoryOutbox();
        ScriptedWorker worker = new ScriptedWorker(runtime, outbox, "worker-1", context -> {
            context.send("im", ScriptedWorkerMain.summary(context));
            return ScriptedWorker.Decision.done();
        });
        int runs = 0;
        while (worker.runOnce()) {
            runs++;
        }

        assertThat(count("SELECT count(*) FROM event")).isEqualTo(1);
        assertThat(runs).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM run")).isEqualTo(1);
        assertThat(outbox.delivered()).extracting(OutboxMessage::text)
                .containsExactly("handled 1 event(s): order.paid.v1 order-1234-v7");
    }
}
