package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.agentflow.inbox.server.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * Invariants I1 and I7 are enforced by the database itself: an event's content cannot be rewritten
 * and its history cannot be edited, whatever code runs against the tables.
 */
class EventImmutabilityIT extends IntegrationTestBase {

    @Test
    void eventContentCannotBeUpdated() {
        String eventId = acme.writeEvent("u42", webhook("order-1", "{\"amount\":299}").build()).require().eventId();

        assertThatThrownBy(() -> jdbc.sql("UPDATE event SET payload = '{\"amount\":1}'::jsonb WHERE event_id = :id")
                .param("id", eventId).update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.sql("UPDATE event SET source_event_id = 'other' WHERE event_id = :id")
                .param("id", eventId).update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("UPDATE event SET merge_key = 'x', lane = 'batch' WHERE event_id = :id")
                .param("id", eventId).update())
                .isInstanceOf(DataAccessException.class);

        assertThat(acme.getEvent("u42", eventId).require().event().payload().get("amount").intValue()).isEqualTo(299);
    }

    @Test
    void deliveryStateCanStillChange() {
        String eventId = acme.writeEvent("u42", webhook("order-1", "{}").build()).require().eventId();
        int updated = jdbc.sql("UPDATE event SET last_error = 'probe' WHERE event_id = :id").param("id", eventId).update();
        assertThat(updated).isEqualTo(1);
    }

    @Test
    void transitionHistoryIsAppendOnly() {
        acme.writeEvent("u42", webhook("order-1", "{}").build()).require();

        assertThatThrownBy(() -> jdbc.sql("UPDATE event_transition SET reason = 'rewritten'").update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM event_transition").update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThat(count("SELECT count(*) FROM event_transition WHERE reason = 'accepted'")).isEqualTo(1);
    }
}
