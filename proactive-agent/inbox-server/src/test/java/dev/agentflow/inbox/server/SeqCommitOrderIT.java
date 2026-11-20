package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Hashing;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.write.AppendCommand;
import dev.agentflow.inbox.server.write.EventWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FIFO rests on one property: {@code seq} order equals commit order. A writer may only take seq N+1
 * after the writer of seq N has committed, so no reader can ever see a higher seq without the lower.
 */
class SeqCommitOrderIT extends IntegrationTestBase {

    @Autowired
    EventWriter writer;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void lowerSeqUncommitted_blocksTheNextWriterOnThatInboxOnly() throws Exception {
        CountDownLatch firstAppended = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(3)) {
            // Writer 1 appends inside a transaction that we keep open.
            CompletableFuture<Void> first = CompletableFuture.runAsync(() -> transactions.executeWithoutResult(tx -> {
                writer.append(command("u42", "first"), clock.now());
                firstAppended.countDown();
                await(releaseFirst);
            }), pool);
            assertThat(firstAppended.await(10, TimeUnit.SECONDS)).isTrue();

            // Writer 2 targets the same inbox: it must wait for writer 1.
            CompletableFuture<ApiResult<WriteEventResponse>> second = CompletableFuture.supplyAsync(
                    () -> acme.writeEvent("u42", webhook("second", "{}").build()), pool);
            try {
                second.get(700, TimeUnit.MILLISECONDS);
                throw new AssertionError("the second writer must block while seq 1 is uncommitted");
            } catch (TimeoutException expected) {
                // blocked on the inbox row, as intended
            }

            // Another inbox is not affected at all.
            assertThat(CompletableFuture.supplyAsync(() -> acme.writeEvent("u43", webhook("other", "{}").build()), pool)
                    .get(10, TimeUnit.SECONDS).status()).isEqualTo(202);

            // Nothing of inbox u42 is visible yet: no reader can see seq 2 without seq 1.
            assertThat(count("SELECT count(*) FROM event WHERE inbox_id = 'u42'")).isZero();

            releaseFirst.countDown();
            first.get(10, TimeUnit.SECONDS);
            assertThat(second.get(10, TimeUnit.SECONDS).status()).isEqualTo(202);
        }

        List<String> keysInSeqOrder = jdbc.sql("SELECT source_event_id FROM event WHERE inbox_id = 'u42' ORDER BY seq")
                .query(String.class).list();
        assertThat(keysInSeqOrder).containsExactly("first", "second");
        assertThat(jdbc.sql("SELECT seq FROM event WHERE inbox_id = 'u42' ORDER BY seq").query(Long.class).list())
                .containsExactly(1L, 2L);
    }

    private AppendCommand command(String inbox, String key) {
        String payload = "{}";
        return new AppendCommand(ACME, inbox, "01TESTEVENT0000000000" + key.toUpperCase().substring(0, 5), "acme-bot",
                EventSource.BIZ_WEBHOOK, key, "provided", "order.paid.v1", payload,
                Hashing.sha256("order.paid.v1\n" + payload), payload.length(), null, clock.now(), null,
                Lane.INTERACTIVE, null, false, "trace-1", null, "client:acme-bot", 5, true);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test did not release the transaction");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
