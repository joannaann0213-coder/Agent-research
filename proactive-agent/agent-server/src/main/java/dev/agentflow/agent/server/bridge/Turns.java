package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.ClaimedRun;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Where each turn stands. There is one row per run of the Event Inbox, and it is what lets a
 * redelivery continue a turn instead of starting it again.
 *
 * <p>Every write is conditional on the epoch the Inbox gave the worker with its lease. A worker
 * that lost its lease, and does not know it yet, therefore cannot write after its successor has
 * taken the turn over: its update matches no row, and it learns from that.
 */
@Repository
public class Turns {

    public static final String OPEN = "open";
    public static final String DONE = "done";
    public static final String FAILED = "failed";

    /** A worker tried to write to a turn that a later lease owns. It must stop and leave the run alone. */
    public static final class StaleTurnException extends RuntimeException {
        StaleTurnException(String runId, int epoch) {
            super("run " + runId + " is no longer held under epoch " + epoch);
        }
    }

    /**
     * @param boundarySet whether the turn has fixed where the session stood before its input
     * @param boundaryEventId the last event the session held then; {@code null} when it held none
     * @param inputEventId the platform's id of the turn's input, once it is known
     */
    public record Turn(String runId, int epoch, String state, Integer generation, String sessionId, boolean boundarySet,
            String boundaryEventId, Instant boundaryAt, String inputEventId, String error, Instant createdAt) {
    }

    private final JdbcClient jdbc;
    private final AgentClock clock;

    public Turns(JdbcClient jdbc, AgentClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Starts the turn of a run, or takes it over from the worker of an earlier lease.
     *
     * @return empty when a later lease already owns the turn: the caller is the stale one
     */
    public Optional<Turn> open(ClaimedRun run) {
        Instant now = clock.now();
        return jdbc.sql("""
                INSERT INTO turn (run_id, app_id, user_id, merge_key, epoch, state, created_at, updated_at)
                VALUES (:runId, :appId, :userId, :mergeKey, :epoch, 'open', :now, :now)
                ON CONFLICT (run_id) DO UPDATE SET epoch = EXCLUDED.epoch, updated_at = EXCLUDED.updated_at
                 WHERE turn.epoch <= EXCLUDED.epoch
                RETURNING run_id, epoch, state, generation, cma_session_id, boundary_set, boundary_event_id, boundary_at,
                          input_event_id, error, created_at
                """)
                .param("runId", run.runId()).param("appId", run.tenantId()).param("userId", run.inboxId())
                .param("mergeKey", run.mergeKey() == null ? "" : run.mergeKey()).param("epoch", run.epoch())
                .param("now", Db.ts(now))
                .query(Turns::row)
                .optional();
    }

    /** Records which session the turn runs in. A turn that moves to another session starts over there. */
    public Turn inSession(Turn turn, int generation, String sessionId) {
        if (sessionId.equals(turn.sessionId())) {
            return turn;
        }
        update(turn, """
                UPDATE turn SET generation = :generation, cma_session_id = :sessionId, boundary_set = false,
                       boundary_event_id = NULL, boundary_at = NULL, input_event_id = NULL, updated_at = :now
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "generation", generation, "sessionId", sessionId);
        return new Turn(turn.runId(), turn.epoch(), turn.state(), generation, sessionId, false, null, null, null, turn.error(),
                turn.createdAt());
    }

    public Turn boundary(Turn turn, String eventId, Instant eventAt) {
        update(turn, """
                UPDATE turn SET boundary_set = true, boundary_event_id = :eventId, boundary_at = :eventAt, updated_at = :now
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "eventId", eventId, "eventAt", Db.ts(eventAt));
        return new Turn(turn.runId(), turn.epoch(), turn.state(), turn.generation(), turn.sessionId(), true, eventId, eventAt,
                turn.inputEventId(), turn.error(), turn.createdAt());
    }

    public void input(Turn turn, String inputEventId) {
        update(turn, """
                UPDATE turn SET input_event_id = :inputEventId, updated_at = :now
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "inputEventId", inputEventId);
    }

    /** Ends the turn. Call it in the transaction that stores the turn's result, so that both happen or neither. */
    public void finish(Turn turn, String state, String outcome, String error) {
        update(turn, """
                UPDATE turn SET state = :state, outcome = :outcome, error = :error, updated_at = :now, finished_at = :now
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "state", state, "outcome", outcome, "error", error);
    }

    /** Open turns that nobody has touched since {@code before}: candidates for a run that ended without this service. */
    public java.util.List<Turn> openAndUntouchedSince(Instant before, int limit) {
        return jdbc.sql("""
                SELECT run_id, epoch, state, generation, cma_session_id, boundary_set, boundary_event_id, boundary_at,
                       input_event_id, error, created_at
                  FROM turn WHERE state = 'open' AND updated_at < :before ORDER BY updated_at LIMIT :limit
                """)
                .param("before", Db.ts(before)).param("limit", limit)
                .query(Turns::row)
                .list();
    }

    /** Notes that an open turn was looked at, so that the sweep does not come back to it at once. */
    public void touch(String runId) {
        jdbc.sql("UPDATE turn SET updated_at = :now WHERE run_id = :runId AND state = 'open'")
                .param("now", Db.ts(clock.now())).param("runId", runId).update();
    }

    /**
     * Closes the turn of a run that ended in the Inbox without this service ending it. Not conditional
     * on an epoch: nobody holds the run any more.
     *
     * @return whether the turn was open
     */
    public boolean abandon(String runId, String error) {
        return jdbc.sql("""
                UPDATE turn SET state = 'failed', error = :error, updated_at = :now, finished_at = :now
                 WHERE run_id = :runId AND state = 'open'
                """)
                .param("error", error).param("now", Db.ts(clock.now())).param("runId", runId)
                .update() == 1;
    }

    public Optional<Turn> find(String runId) {
        return jdbc.sql("""
                SELECT run_id, epoch, state, generation, cma_session_id, boundary_set, boundary_event_id, boundary_at,
                       input_event_id, error, created_at
                  FROM turn WHERE run_id = :runId
                """)
                .param("runId", runId)
                .query(Turns::row)
                .optional();
    }

    private void update(Turn turn, String sql, Object... namesAndValues) {
        JdbcClient.StatementSpec statement = jdbc.sql(sql)
                .param("runId", turn.runId()).param("epoch", turn.epoch()).param("now", Db.ts(clock.now()));
        for (int i = 0; i < namesAndValues.length; i += 2) {
            statement = statement.param((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        if (statement.update() == 0) {
            throw new StaleTurnException(turn.runId(), turn.epoch());
        }
    }

    private static Turn row(ResultSet rs, int rowNumber) throws SQLException {
        int generation = rs.getInt("generation");
        return new Turn(rs.getString("run_id"), rs.getInt("epoch"), rs.getString("state"), rs.wasNull() ? null : generation,
                rs.getString("cma_session_id"), rs.getBoolean("boundary_set"), rs.getString("boundary_event_id"),
                Db.instant(rs, "boundary_at"), rs.getString("input_event_id"), rs.getString("error"),
                Db.instant(rs, "created_at"));
    }
}
