package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.agentflow.agent.server.relay.InboxRelay;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.EventView;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Events reach the Event Inbox through a local commit and a delivery that is retried, never through
 * a write to two systems at once.
 */
class InboxRelayIT extends AgentTestBase {

    @Autowired
    InboxRelay relay;

    @Autowired
    TransactionTemplate tx;

    @Test
    void aQueuedEventReachesTheInboxOfItsUser_inTheTenantOfItsApplication() {
        tx.executeWithoutResult(status -> relay.enqueue(ACME, "u42", message("msg:c1:m1", "hello")));
        assertThat(events(ACME, "u42")).as("nothing leaves before the relay runs").isEmpty();

        assertThat(relay.runOnce()).isEqualTo(1);

        assertThat(events(ACME, "u42")).singleElement().satisfies(event -> {
            assertThat(event.tenantId()).isEqualTo(ACME);
            assertThat(event.sourceEventId()).isEqualTo("msg:c1:m1");
            assertThat(event.mergeKey()).isEqualTo("conv:c1");
            assertThat(event.lane()).isEqualTo(Lane.INTERACTIVE);
            assertThat(event.interrupt()).as("never left to the Inbox's stop-word guess").isFalse();
            assertThat(event.payload().path("text").asString()).isEqualTo("hello");
            assertThat(event.clientId()).isEqualTo(EmbeddedInbox.AGENT_CLIENT);
        });
        assertThat(events(GLOBEX, "u42")).isEmpty();
        assertThat(relay.pending()).isZero();
        assertThat(relay.runOnce()).as("nothing is relayed twice").isZero();
        assertThat(count("SELECT count(*) FROM inbox_relay WHERE status = 'relayed' AND finished_at IS NOT NULL AND attempts = 1")).isEqualTo(1);
    }

    @Test
    void anEventIsQueuedInsideATransactionOrNotAtAll() {
        assertThatThrownBy(() -> relay.enqueue(ACME, "u42", message("msg:c1:m1", "hello")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("inside a transaction");

        tx.executeWithoutResult(status -> {
            relay.enqueue(ACME, "u42", message("msg:c1:m2", "never mind"));
            status.setRollbackOnly();
        });

        assertThat(relay.runOnce()).isZero();
        assertThat(count("SELECT count(*) FROM inbox_relay")).as("the event went with the transaction").isZero();
        assertThat(events(ACME, "u42")).isEmpty();
    }

    @Test
    void whileTheInboxIsDownTheEventWaits_andIsTriedAgainLaterAndLater() {
        tx.executeWithoutResult(status -> relay.enqueue(ACME, "u42", message("msg:c1:m1", "hello")));
        EmbeddedInbox.jdbc().sql("ALTER TABLE event RENAME TO event_offline").update();
        try {
            assertThat(relay.runOnce()).isZero();
            assertThat(relay.runOnce()).as("not again before the delay is over").isZero();
            assertThat(attempts()).isEqualTo(1);

            clock.advance(Duration.ofSeconds(1));
            assertThat(relay.runOnce()).isZero();
            assertThat(attempts()).isEqualTo(2);
            clock.advance(Duration.ofSeconds(1));
            assertThat(relay.runOnce()).as("the delay has doubled").isZero();
            assertThat(attempts()).isEqualTo(2);
            assertThat(jdbc.sql("SELECT last_error FROM inbox_relay").query(String.class).single()).contains("503");
        } finally {
            EmbeddedInbox.jdbc().sql("ALTER TABLE event_offline RENAME TO event").update();
        }

        clock.advance(Duration.ofSeconds(1));
        assertThat(relay.runOnce()).isEqualTo(1);
        assertThat(events(ACME, "u42")).hasSize(1);
        assertThat(relay.pending()).isZero();
    }

    @Test
    void theDelayStopsGrowingAtItsMaximum() {
        tx.executeWithoutResult(status -> relay.enqueue(ACME, "u42", message("msg:c1:m1", "hello")));
        EmbeddedInbox.jdbc().sql("ALTER TABLE event RENAME TO event_offline").update();
        try {
            for (int attempt = 0; attempt < 12; attempt++) {
                relay.runOnce();
                clock.advance(Duration.ofMinutes(1));
            }
            assertThat(attempts()).as("one attempt per minute once the delay has reached a minute").isGreaterThanOrEqualTo(11);
        } finally {
            EmbeddedInbox.jdbc().sql("ALTER TABLE event_offline RENAME TO event").update();
        }
        assertThat(relay.runOnce()).isEqualTo(1);
    }

    @Test
    void anEventTheInboxRefusesAsWrongIsNotTriedAgain_andDoesNotHoldUpTheNextOne() {
        tx.executeWithoutResult(status -> {
            // the prefix "timer:" is reserved for the Inbox itself
            relay.enqueue(ACME, "u42", message("timer:not-allowed", "bad"));
            relay.enqueue(ACME, "u42", message("msg:c1:m2", "good"));
        });

        assertThat(relay.runOnce()).isEqualTo(1);

        assertThat(events(ACME, "u42")).extracting(EventView::sourceEventId).containsExactly("msg:c1:m2");
        assertThat(jdbc.sql("SELECT last_error FROM inbox_relay WHERE status = 'failed'").query(String.class).single())
                .contains("400", "invalid_request");
        clock.advance(Duration.ofHours(1));
        assertThat(relay.runOnce()).isZero();
        assertThat(relay.pending()).isZero();
    }

    @Test
    void anEventRelayedTwiceIsStillOneEvent() {
        tx.executeWithoutResult(status -> {
            relay.enqueue(ACME, "u42", message("msg:c1:m1", "hello"));
            relay.enqueue(ACME, "u42", message("msg:c1:m1", "hello"));
        });

        assertThat(relay.runOnce()).isEqualTo(2);

        assertThat(events(ACME, "u42")).hasSize(1);
        assertThat(count("SELECT count(*) FROM inbox_relay WHERE status = 'relayed'")).isEqualTo(2);
    }

    @Test
    void aLongBacklogIsRelayedInOrder_inOnePass() {
        tx.executeWithoutResult(status -> {
            for (int i = 1; i <= 120; i++) {
                relay.enqueue(ACME, "u42", message("msg:c1:m" + i, "message " + i));
            }
        });

        assertThat(relay.runOnce()).isEqualTo(120);

        assertThat(events(ACME, "u42")).extracting(EventView::sourceEventId)
                .startsWith("msg:c1:m1", "msg:c1:m2", "msg:c1:m3").endsWith("msg:c1:m120").hasSize(120);
    }

    private static WriteEventRequest message(String key, String text) {
        return WriteEventRequest.builder(EventSource.CONVERSATION, "agent.message.v1")
                .sourceEventId(key)
                .mergeKey("conv:c1")
                .lane(Lane.INTERACTIVE)
                .interrupt(false)
                .payloadJson("{\"text\": \"" + text + "\"}")
                .build();
    }

    private static EventView[] events(String appId, String userId) {
        return EmbeddedInbox.operator().listEvents(userId, "tenant_id=" + appId + "&limit=500").require();
    }

    private int attempts() {
        return jdbc.sql("SELECT attempts FROM inbox_relay").query(Integer.class).single();
    }
}
