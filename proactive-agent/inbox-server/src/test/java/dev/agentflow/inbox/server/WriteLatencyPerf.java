package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Measures the write path against the design target (p99 at most 50 ms, network excluded). This is a
 * measurement, not part of the normal build: the result depends on the machine and above all on how
 * fast its disk confirms a commit. Run it on purpose:
 *
 * <pre>
 * mvn -pl inbox-server -am verify -Dit.test=WriteLatencyPerf \
 *     -Dfailsafe.failIfNoSpecifiedTests=false -Dsurefire.failIfNoSpecifiedTests=false
 * </pre>
 *
 * It prints lines starting with {@code WRITE LATENCY}. The times are taken by the client over
 * loopback HTTP, so they include signing, the HTTP round trip and JSON on both sides.
 */
class WriteLatencyPerf extends IntegrationTestBase {

    private static final int WARM_UP = 500;
    private static final int SAMPLES = 2000;
    private static final int INBOXES = 50;

    @Test
    void measureWriteLatency() throws Exception {
        for (int i = 0; i < WARM_UP; i++) {
            acme.writeEvent("warm-" + (i % INBOXES), webhook("warm-" + i, "{\"n\":" + i + "}").build()).require();
        }

        // one producer, one request at a time
        long[] sequential = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long started = System.nanoTime();
            acme.writeEvent("user-" + (i % INBOXES), webhook("seq-" + i, "{\"order_id\":\"" + i + "\",\"amount\":299}")
                    .mergeKey("order:" + i).build()).require();
            sequential[i] = System.nanoTime() - started;
        }
        report("sequential, 1 producer", sequential);

        // duplicates take the read-only path: no inbox lock, nothing written
        long[] duplicates = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long started = System.nanoTime();
            assertThat(acme.writeEvent("user-" + (i % INBOXES), webhook("seq-" + i,
                    "{\"order_id\":\"" + i + "\",\"amount\":299}").mergeKey("order:" + i).build()).require().deduped()).isTrue();
            duplicates[i] = System.nanoTime() - started;
        }
        report("duplicates", duplicates);

        // eight producers at once, spread over the same inboxes
        int producers = 8;
        List<Future<long[]>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(producers)) {
            for (int p = 0; p < producers; p++) {
                int producer = p;
                futures.add(pool.submit(() -> {
                    long[] times = new long[SAMPLES / producers];
                    for (int i = 0; i < times.length; i++) {
                        long started = System.nanoTime();
                        acme.writeEvent("user-" + ((i * producers + producer) % INBOXES),
                                webhook("par-" + producer + "-" + i, "{\"n\":" + i + "}").build()).require();
                        times[i] = System.nanoTime() - started;
                    }
                    return times;
                }));
            }
        }
        long[] concurrent = new long[SAMPLES];
        int offset = 0;
        for (Future<long[]> future : futures) {
            long[] times = future.get();
            System.arraycopy(times, 0, concurrent, offset, times.length);
            offset += times.length;
        }
        double p99 = report("concurrent, " + producers + " producers", concurrent);

        assertThat(count("SELECT count(*) FROM event")).isEqualTo(WARM_UP + 2L * SAMPLES);
        assertThat(p99).as("sanity bound only; the design target is 50 ms").isLessThan(1000.0);
    }

    /** Prints the distribution and returns the p99 in milliseconds. */
    private static double report(String label, long[] nanos) {
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        double p50 = sorted[(int) (sorted.length * 0.50)] / 1e6;
        double p95 = sorted[(int) (sorted.length * 0.95)] / 1e6;
        double p99 = sorted[(int) (sorted.length * 0.99)] / 1e6;
        double max = sorted[sorted.length - 1] / 1e6;
        System.out.printf("WRITE LATENCY %-28s n=%d  p50=%.2f ms  p95=%.2f ms  p99=%.2f ms  max=%.2f ms%n",
                label, sorted.length, p50, p95, p99, max);
        return p99;
    }
}
