package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.api.RunController;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * F5: batching. Events of one inbox with the same merge key that arrive inside the quiet window
 * become one run. Producers still post one by one and never learn that a window exists; every merged
 * event keeps its own record and shares the run's id.
 */
class F5BatchingIT extends IntegrationTestBase {

    @Test
    void fiveMessagesInARow_areOneRun() {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ids.add(write("u42", message("msg-" + i, "u42", "part " + i)));
            clock.advance(Duration.ofMillis(100));
        }

        // The first message is 500 ms old, the last one 100 ms: the user may still be typing.
        ApiResult<ClaimedRun> early = claim("worker-1", "u42");
        assertThat(early.status()).isEqualTo(204);
        assertThat(early.header(RunController.HEADER_REASON)).contains("window");
        assertThat(early.header(RunController.HEADER_RETRY_AFTER_MS)).contains("1400");
        assertThat(count("SELECT count(*) FROM run")).isZero();

        clock.advance(Duration.ofMillis(1400)); // 1.5 s of silence after the last message
        ClaimedRun run = claim("worker-1", "u42").require();
        assertThat(run.mergeKey()).isEqualTo("conversation:u42");
        assertThat(run.events()).extracting(ClaimedEvent::eventId).containsExactlyElementsOf(ids);
        assertThat(run.events()).extracting(ClaimedEvent::runSeq).containsExactly(1, 2, 3, 4, 5);
        assertThat(run.events()).extracting(e -> e.payload().get("text").stringValue())
                .containsExactly("part 1", "part 2", "part 3", "part 4", "part 5");
        assertThat(run.lastRunSeq()).isEqualTo(5);

        // merged, not cancelled: five records, one run id, and they finish together
        assertThat(count("SELECT count(DISTINCT run_id) FROM event WHERE status = 'claimed'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'claimed'")).isEqualTo(5);
        runtime.ackDone(run).require();
        assertThat(count("SELECT count(*) FROM event WHERE status = 'done' AND run_id = '" + run.runId() + "'")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM run")).isEqualTo(1);
        assertThat(claim("worker-1", "u42").status()).isEqualTo(204);
    }

    @Test
    void aBatchIsCappedAtTwentyEvents_andAFullBatchDoesNotWait() {
        for (int i = 1; i <= 25; i++) {
            write("u42", message("msg-" + i, "u42", "m" + i));
        }
        // 25 messages at the same instant: the first 20 go now, without any quiet window
        ClaimedRun first = claim("worker-1", "u42").require();
        assertThat(first.events()).hasSize(20);
        assertThat(first.events().getFirst().seq()).isEqualTo(1);
        assertThat(first.events().getLast().seq()).isEqualTo(20);
        runtime.ackDone(first).require();

        // the other five are an ordinary batch: they wait for their window
        assertThat(claim("worker-1", "u42").header(RunController.HEADER_REASON)).contains("window");
        clock.advance(Duration.ofMillis(1500));
        ClaimedRun second = claim("worker-1", "u42").require();
        assertThat(second.events()).hasSize(5);
        assertThat(second.events().getFirst().seq()).isEqualTo(21);
        runtime.ackDone(second).require();
    }

    @Test
    void aBatchIsCappedAt256Kb() {
        String sixtyKb = "x".repeat(60_000);
        for (int i = 1; i <= 6; i++) {
            write("u42", WriteEventRequest.builder(EventSource.CONNECTOR, "mail.received.v1")
                    .sourceEventId("mail-" + i).mergeKey("thread:9").payloadJson("{\"body\":\"" + sixtyKb + "\"}"));
        }
        ClaimedRun first = claim("worker-1", "u42").require();
        assertThat(first.events()).as("four fit under 256 KB, the fifth would not").hasSize(4);
        runtime.ackDone(first).require();

        clock.advance(Duration.ofMillis(1500));
        ClaimedRun second = claim("worker-1", "u42").require();
        assertThat(second.events()).hasSize(2);
        runtime.ackDone(second).require();
    }

    @Test
    void maxWaitBoundsHowLongATrickleCanHoldTheWindowOpen() {
        // one message every second: the quiet window (1.5 s) never closes on its own
        for (int i = 1; i <= 5; i++) {
            write("u42", message("msg-" + i, "u42", "m" + i)); // at 0 s, 1 s, 2 s, 3 s, 4 s
            clock.advance(Duration.ofSeconds(1));
        }
        ApiResult<ClaimedRun> stillTyping = claim("worker-1", "u42"); // at 5 s; the last message is 1 s old
        assertThat(stillTyping.status()).isEqualTo(204);
        assertThat(stillTyping.header(RunController.HEADER_RETRY_AFTER_MS)).contains("500");

        write("u42", message("msg-6", "u42", "m6")); // at 5 s: the quiet window would now run until 6.5 s
        clock.advance(Duration.ofMillis(900));
        ApiResult<ClaimedRun> almost = claim("worker-1", "u42"); // at 5.9 s
        assertThat(almost.status()).isEqualTo(204);
        assertThat(almost.header(RunController.HEADER_RETRY_AFTER_MS))
                .as("max_wait: 6 s after the first message, not 1.5 s after the last").contains("100");

        write("u42", message("msg-7", "u42", "m7")); // at 5.9 s: still typing
        clock.advance(Duration.ofMillis(100));
        // 6 s after the first message (four quiet windows) the batch goes, although the last message is 100 ms old
        ClaimedRun run = claim("worker-1", "u42").require();
        assertThat(run.events()).hasSize(7);
        runtime.ackDone(run).require();
    }

    @Test
    void eventsWithoutAMergeKeyAreNeverBatchedAndNeverWait() {
        String a = write("u42", webhook("a", "{}"));
        String b = write("u42", webhook("b", "{}"));

        ClaimedRun first = claim("worker-1", "u42").require();
        assertThat(first.events()).extracting(ClaimedEvent::eventId).containsExactly(a);
        runtime.ackDone(first).require();
        ClaimedRun second = claim("worker-1", "u42").require();
        assertThat(second.events()).extracting(ClaimedEvent::eventId).containsExactly(b);
        runtime.ackDone(second).require();
    }

    @Test
    void differentKeysAreDifferentRuns_evenWhenInterleaved() {
        String a1 = write("u42", webhook("a1", "{}").mergeKey("order:A"));
        String b1 = write("u42", webhook("b1", "{}").mergeKey("order:B"));
        String a2 = write("u42", webhook("a2", "{}").mergeKey("order:A"));
        clock.advance(Duration.ofSeconds(2));

        ClaimedRun first = claim("worker-1", "u42").require();
        assertThat(first.mergeKey()).isEqualTo("order:A");
        assertThat(first.events()).extracting(ClaimedEvent::eventId).containsExactly(a1, a2);
        runtime.ackDone(first).require();

        ClaimedRun second = claim("worker-1", "u42").require();
        assertThat(second.events()).extracting(ClaimedEvent::eventId).containsExactly(b1);
        runtime.ackDone(second).require();
    }

    @Test
    void aKeyStillInsideItsWindowDoesNotHoldBackAnotherKey() {
        write("u42", webhook("old", "{}").mergeKey("order:settled"));
        clock.advance(Duration.ofSeconds(2));
        write("u42", webhook("new", "{}").mergeKey("order:fresh"));

        ClaimedRun run = claim("worker-1", "u42").require();
        assertThat(run.mergeKey()).isEqualTo("order:settled");
        runtime.ackDone(run).require();
        assertThat(claim("worker-1", "u42").header(RunController.HEADER_REASON)).contains("window");
    }

    @Test
    void theWindowIsConfigurablePerInboxAndLane() {
        write("u42", WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "report.requested.v1")
                .sourceEventId("r1").mergeKey("report:weekly").lane(Lane.BACKGROUND).payloadJson("{}"));
        jdbc.sql("""
                UPDATE inbox SET settings = '{"lanes":{"background":{"quiet_window_ms":30000,"max_wait_ms":60000}}}'::jsonb
                 WHERE inbox_id = 'u42'
                """).update();

        clock.advance(Duration.ofSeconds(29));
        ApiResult<ClaimedRun> early = claim("worker-1", "u42");
        assertThat(early.status()).isEqualTo(204);
        assertThat(early.header(RunController.HEADER_RETRY_AFTER_MS)).contains("1000");
        clock.advance(Duration.ofSeconds(1));
        runtime.ackDone(claim("worker-1", "u42").require()).require();
    }
}
