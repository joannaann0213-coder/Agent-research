package dev.agentflow.inbox.server.query;

import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.EventStatus;
import dev.agentflow.inbox.api.EventView;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.TransitionView;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.db.Db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Read side: an event and its history, always scoped to one tenant. */
@Service
public class EventQueries {

    public static final String EVENT_COLUMNS = """
            event_id, tenant_id, inbox_id, seq, client_id, source, source_event_id, dedupe_kind, type, payload,
            occurred_at, received_at, ready_at, merge_key, lane, resume_key, interrupt, trace_id, timer_id,
            status, attempt, max_attempts, run_id, run_seq, delivery_mode, first_claimed_at, finished_at, last_error
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public EventQueries(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * One call returns the event and its whole path from acceptance to its current state.
     *
     * @param clientId when set, only events written by this client are visible (a producer sees its own)
     */
    public EventDetail detail(String tenantId, String inboxId, String eventId, String clientId) {
        List<EventView> events = jdbc.sql("SELECT " + EVENT_COLUMNS + """
                  FROM event
                 WHERE event_id = :eventId AND tenant_id = :tenant AND inbox_id = :inbox
                   AND (CAST(:clientId AS text) IS NULL OR client_id = :clientId)
                """)
                .param("eventId", eventId).param("tenant", tenantId).param("inbox", inboxId).param("clientId", clientId)
                .query((rs, n) -> mapEvent(rs))
                .list();
        if (!events.isEmpty()) {
            return new EventDetail(events.getFirst(), transitions(eventId));
        }
        // A deferred event has no row yet: it is still a timer.
        List<EventView> scheduled = jdbc.sql("""
                SELECT event_id, tenant_id, inbox_id, client_id, event_source, source_event_id, dedupe_kind, event_type,
                       payload, occurred_at, fire_at, merge_key, lane, resume_key, interrupt, trace_id, timer_id, state
                  FROM timer
                 WHERE event_id = :eventId AND tenant_id = :tenant AND inbox_id = :inbox
                   AND (CAST(:clientId AS text) IS NULL OR client_id = :clientId)
                """)
                .param("eventId", eventId).param("tenant", tenantId).param("inbox", inboxId).param("clientId", clientId)
                .query((rs, n) -> new EventView(rs.getString("event_id"), rs.getString("tenant_id"),
                        rs.getString("inbox_id"), null, rs.getString("client_id"),
                        EventSource.fromWire(rs.getString("event_source")), rs.getString("source_event_id"),
                        rs.getString("dedupe_kind"), rs.getString("event_type"), readJson(rs.getString("payload")),
                        Db.instant(rs, "occurred_at"), null, Db.instant(rs, "fire_at"), rs.getString("merge_key"),
                        Lane.fromWire(rs.getString("lane")), rs.getString("resume_key"), rs.getBoolean("interrupt"),
                        rs.getString("trace_id"), rs.getString("timer_id"),
                        "scheduled".equals(rs.getString("state")) ? EventStatus.SCHEDULED : rs.getString("state"),
                        0, 0, null, null, null, null, null, null))
                .list();
        if (!scheduled.isEmpty()) {
            return new EventDetail(scheduled.getFirst(), List.of());
        }
        throw InboxException.notFound("no such event in this inbox");
    }

    public List<TransitionView> transitions(String eventId) {
        return jdbc.sql("""
                SELECT from_status, to_status, reason, actor, run_id, attempt, detail, at
                  FROM event_transition
                 WHERE event_id = :eventId
                 ORDER BY transition_id
                """)
                .param("eventId", eventId)
                .query((rs, n) -> new TransitionView(rs.getString("from_status"), rs.getString("to_status"),
                        rs.getString("reason"), rs.getString("actor"), rs.getString("run_id"),
                        Db.intOrNull(rs, "attempt"), readJson(rs.getString("detail")), Db.instant(rs, "at")))
                .list();
    }

    public EventView mapEvent(ResultSet rs) throws SQLException {
        return new EventView(rs.getString("event_id"), rs.getString("tenant_id"), rs.getString("inbox_id"),
                rs.getLong("seq"), rs.getString("client_id"), EventSource.fromWire(rs.getString("source")),
                rs.getString("source_event_id"), rs.getString("dedupe_kind"), rs.getString("type"),
                readJson(rs.getString("payload")), Db.instant(rs, "occurred_at"), Db.instant(rs, "received_at"),
                Db.instant(rs, "ready_at"), rs.getString("merge_key"), Lane.fromWire(rs.getString("lane")),
                rs.getString("resume_key"), rs.getBoolean("interrupt"), rs.getString("trace_id"),
                rs.getString("timer_id"), rs.getString("status"), rs.getInt("attempt"), rs.getInt("max_attempts"),
                rs.getString("run_id"), Db.intOrNull(rs, "run_seq"), rs.getString("delivery_mode"),
                Db.instant(rs, "first_claimed_at"), Db.instant(rs, "finished_at"), rs.getString("last_error"));
    }

    public JsonNode readJson(String text) {
        return text == null ? null : json.readTree(text);
    }
}
