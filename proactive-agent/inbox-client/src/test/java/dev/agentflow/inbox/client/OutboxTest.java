package dev.agentflow.inbox.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutboxTest {

    private static final OutboxMessage FIRST = new OutboxMessage("run-1", 1, "im", "trace-1", "hello");

    @Test
    void aMessageIsDeliveredOncePerRunAndSequence() {
        MemoryOutbox outbox = new MemoryOutbox();

        assertThat(outbox.send(FIRST)).isTrue();
        assertThat(outbox.send(new OutboxMessage("run-1", 1, "im", "trace-1", "hello again"))).as("same key").isFalse();
        assertThat(outbox.send(new OutboxMessage("run-1", 2, "im", "trace-1", "second"))).isTrue();
        assertThat(outbox.send(new OutboxMessage("run-2", 1, "im", "trace-2", "other run"))).isTrue();

        assertThat(outbox.delivered()).hasSize(3);
        assertThat(outbox.delivered()).filteredOn(m -> m.dedupeKey().equals("run-1:1"))
                .singleElement().satisfies(m -> assertThat(m.text()).isEqualTo("hello"));
    }

    @Test
    void twoProcessesSharingADirectoryShareTheDeliveryLog(@TempDir Path directory) {
        FileOutbox crashedWorker = new FileOutbox(directory);
        FileOutbox successor = new FileOutbox(directory);

        assertThat(crashedWorker.send(FIRST)).isTrue();
        assertThat(successor.send(FIRST)).as("the successor replays the same step").isFalse();
        assertThat(successor.send(new OutboxMessage("run-1", 2, "im", "trace-1", "next step"))).isTrue();

        assertThat(crashedWorker.delivered()).hasSize(2);
        assertThat(successor.delivered()).extracting(OutboxMessage::dedupeKey).containsExactlyInAnyOrder("run-1:1", "run-1:2");
    }
}
