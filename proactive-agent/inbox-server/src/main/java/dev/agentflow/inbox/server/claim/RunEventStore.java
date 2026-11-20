package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.server.db.Db;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The statements that move events in and out of a run. Each one changes delivery state and writes the
 * matching history rows in the same statement, so the two can never disagree. Callers hold the inbox
 * row lock.
 */
@Component
public class RunEventStore {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public RunEventStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Binds unclaimed events to a run, numbering them from {@code firstRunSeq} in the order given.
     *
     * @param reason history entry: {@code claim}, {@code steer} or {@code resume}
     */
    public void attach(String runId, List<String> eventIds, int firstRunSeq, String mode, int attempt, String reason,
            String actor, Instant now) {
        int bound = jdbc.sql("""
                WITH moved AS (
                    UPDATE event e
                       SET status = 'claimed', run_id = :run, run_seq = CAST(:firstSeq + t.ord - 1 AS int),
                           delivery_mode = :mode, attempt = :attempt,
                           first_claimed_at = COALESCE(e.first_claimed_at, :now)
                      FROM unnest(string_to_array(:ids, ',')) WITH ORDINALITY AS t(id, ord)
                     WHERE e.event_id = t.id AND e.status = 'pending' AND e.run_id IS NULL
                    RETURNING e.event_id, e.tenant_id, e.inbox_id, e.attempt, e.run_seq
                ), history AS (
                    INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                                  run_id, attempt, at)
                    SELECT event_id, tenant_id, inbox_id, 'pending', 'claimed', :reason, :actor, :run, attempt, :now
                      FROM moved
                     ORDER BY run_seq
                )
                SELECT count(*) FROM moved
                """)
                .param("run", runId).param("firstSeq", firstRunSeq).param("mode", mode).param("attempt", attempt)
                .param("now", Db.ts(now)).param("ids", String.join(",", eventIds)).param("reason", reason)
                .param("actor", actor)
                .query(Integer.class).single();
        if (bound != eventIds.size()) {
            // Cannot happen while the inbox lock is held; if it does, roll back rather than lose an event.
            throw new IllegalStateException("planned to bind " + eventIds.size() + " events but bound " + bound);
        }
    }

    /** A queued run is issued again: its events, still bound to it, become claimed. */
    public void reclaim(String runId, int attempt, String actor, Instant now) {
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = 'claimed', attempt = :attempt
                     WHERE run_id = :run AND status = 'pending'
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'pending', 'claimed', 'redelivery', :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("run", runId).param("attempt", attempt).param("actor", actor).param("now", Db.ts(now))
                .update();
    }

    /** Claimed events up to the cursor reach a final state ({@code done}, {@code failed} or {@code dead}). */
    public void finish(String runId, int cursor, String toStatus, String reason, String actor, String error, Instant now) {
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = :to, finished_at = :now, last_error = CAST(:error AS text)
                     WHERE run_id = :run AND status = 'claimed' AND run_seq <= :cursor
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'claimed', :to, :reason, :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("to", toStatus).param("now", Db.ts(now)).param("error", error).param("run", runId)
                .param("cursor", cursor).param("reason", reason).param("actor", actor)
                .update();
    }

    /** Claimed events beyond the cursor were never consumed: they leave the run and wait again. */
    public void release(String runId, int cursor, String actor, Instant now) {
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = 'pending', run_id = NULL, run_seq = NULL, delivery_mode = NULL
                     WHERE run_id = :run AND status = 'claimed' AND run_seq > :cursor
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'claimed', 'pending', 'released', :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("run", runId).param("cursor", cursor).param("actor", actor).param("now", Db.ts(now))
                .update();
    }

    /** The run goes back to the queue: its events return to {@code pending} but stay bound to it. */
    public void requeue(String runId, String reason, String actor, Instant now) {
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = 'pending'
                     WHERE run_id = :run AND status = 'claimed'
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'claimed', 'pending', :reason, :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("run", runId).param("reason", reason).param("actor", actor).param("now", Db.ts(now))
                .update();
    }

    /** Records that a run's events are now waiting with it (their status stays {@code claimed}). */
    public void noteSuspended(String runId, int cursor, String resumeKey, Instant deadline, String actor, Instant now) {
        jdbc.sql("""
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, detail, at)
                SELECT event_id, tenant_id, inbox_id, 'claimed', 'claimed', 'suspended', :actor, :run, attempt,
                       jsonb_build_object('resume_key', CAST(:resumeKey AS text), 'deadline', CAST(:deadline AS text)), :now
                  FROM event
                 WHERE run_id = :run AND status = 'claimed' AND run_seq <= :cursor
                 ORDER BY run_seq
                """)
                .param("actor", actor).param("run", runId).param("resumeKey", resumeKey)
                .param("deadline", deadline.toString()).param("now", Db.ts(now)).param("cursor", cursor)
                .update();
    }

    /** The run's claimed events with {@code run_seq} above {@code afterRunSeq}, in run order, with payloads. */
    public List<ClaimedEvent> claimedEvents(String runId, int afterRunSeq) {
        return jdbc.sql("""
                SELECT event_id, seq, run_seq, delivery_mode, source, type, source_event_id, occurred_at, received_at,
                       merge_key, resume_key, interrupt, payload, trace_id
                  FROM event
                 WHERE run_id = :run AND status = 'claimed' AND run_seq > :after
                 ORDER BY run_seq
                """)
                .param("run", runId).param("after", afterRunSeq)
                .query((rs, n) -> new ClaimedEvent(rs.getString("event_id"), rs.getLong("seq"), rs.getInt("run_seq"),
                        rs.getString("delivery_mode"), EventSource.fromWire(rs.getString("source")),
                        rs.getString("type"), rs.getString("source_event_id"), Db.instant(rs, "occurred_at"),
                        Db.instant(rs, "received_at"), rs.getString("merge_key"), rs.getString("resume_key"),
                        rs.getBoolean("interrupt"), json.readTree(rs.getString("payload")), rs.getString("trace_id")))
                .list();
    }
}
