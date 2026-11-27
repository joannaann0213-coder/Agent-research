package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.ClaimedRun;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;

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
     * @param boundarySet whether the turn has fixed where the session stood before its input; from
     *     then on the session is the turn's
     * @param boundaryEventId the last event the session held then; {@code null} when it held none
     * @param inputEventId the platform's id of the turn's input, once it is known
     * @param deadlineAt when the turn gives up; it moves on when the turn resumes after waiting for people
     * @param closing why the turn has to finish without waiting for anybody again, or {@code null}
     * @param work what the turn works on, as found at its first delivery
     * @param parked whether the turn waits for people, its run suspended in the Inbox
     * @param yields how often the turn has given way to a message of the user and started over
     */
    public record Turn(String runId, String mergeKey, int epoch, String state, Integer generation, String sessionId,
            boolean boundarySet, String boundaryEventId, Instant boundaryAt, String inputEventId, String error,
            Instant createdAt, Instant deadlineAt, String closing, JsonNode work, boolean parked, String appId, String userId,
            int yields) {

        Turn inSession(int newGeneration, String newSessionId) {
            return new Turn(runId, mergeKey, epoch, state, newGeneration, newSessionId, false, null, null, null, error,
                    createdAt, deadlineAt, closing, work, parked, appId, userId, yields);
        }

        Turn withBoundary(String eventId, Instant eventAt) {
            return new Turn(runId, mergeKey, epoch, state, generation, sessionId, true, eventId, eventAt, inputEventId, error,
                    createdAt, deadlineAt, closing, work, parked, appId, userId, yields);
        }

        Turn withWork(JsonNode newWork) {
            return new Turn(runId, mergeKey, epoch, state, generation, sessionId, boundarySet, boundaryEventId, boundaryAt,
                    inputEventId, error, createdAt, deadlineAt, closing, newWork, parked, appId, userId, yields);
        }
    }

    public static final String CHAT = "chat";
    public static final String BACKGROUND = "background";
    public static final String WORKER = "worker";

    private final JdbcClient jdbc;
    private final AgentClock clock;
    private final TurnMetrics metrics;

    public Turns(JdbcClient jdbc, AgentClock clock, TurnMetrics metrics) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.metrics = metrics;
    }

    /**
     * Starts the turn of a run, or takes it over from the worker of an earlier lease. A turn that
     * resumes after waiting for people gets its time anew.
     *
     * @param deadline how long the turn may work
     * @return empty when a later lease already owns the turn: the caller is the stale one
     */
    public Optional<Turn> open(ClaimedRun run, Duration deadline) {
        Instant now = clock.now();
        return jdbc.sql("""
                INSERT INTO turn (run_id, app_id, user_id, merge_key, epoch, state, created_at, updated_at, deadline_at)
                VALUES (:runId, :appId, :userId, :mergeKey, :epoch, 'open', :now, :now, :deadlineAt)
                ON CONFLICT (run_id) DO UPDATE SET epoch = EXCLUDED.epoch, updated_at = EXCLUDED.updated_at, parked = false,
                       deadline_at = CASE WHEN :resumed THEN EXCLUDED.deadline_at ELSE turn.deadline_at END
                 WHERE turn.epoch <= EXCLUDED.epoch
                RETURNING
                """ + COLUMNS)
                .param("runId", run.runId()).param("appId", run.tenantId()).param("userId", run.inboxId())
                .param("mergeKey", run.mergeKey() == null ? "" : run.mergeKey()).param("epoch", run.epoch())
                .param("now", Db.ts(now)).param("deadlineAt", Db.ts(now.plus(deadline)))
                .param("resumed", ClaimedRun.KIND_RESUME.equals(run.kind()))
                .query(Turns::row)
                .optional();
    }

    /**
     * Records what the turn works on, and with that what kind of turn it is.
     *
     * @param kind {@link #CHAT}, {@link #BACKGROUND} or {@link #WORKER}
     * @param agentKey the agent of the turn's line
     */
    public Turn work(Turn turn, JsonNode work, String kind, String agentKey) {
        update(turn, """
                UPDATE turn SET work = CAST(:work AS jsonb), kind = :kind, agent_key = :agentKey, updated_at = :now
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "work", Json.STORED.writeValueAsString(work), "kind", kind, "agentKey", agentKey);
        return turn.withWork(work);
    }

    /**
     * Records what kind of turn this is, for one that ends before it has work to record: a message
     * that another turn has answered, a task that was closed. Without it the turn would count as none.
     */
    public void kind(Turn turn, String kind, String agentKey) {
        update(turn, "UPDATE turn SET kind = :kind, agent_key = :agentKey, updated_at = :now "
                + "WHERE run_id = :runId AND epoch = :epoch AND state = 'open'", "kind", kind, "agentKey", agentKey);
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
        return turn.inSession(generation, sessionId);
    }

    public Turn boundary(Turn turn, String eventId, Instant eventAt) {
        update(turn, """
                UPDATE turn SET boundary_set = true, boundary_event_id = :eventId, boundary_at = :eventAt, updated_at = :now
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "eventId", eventId, "eventAt", Db.ts(eventAt));
        return turn.withBoundary(eventId, eventAt);
    }

    public void input(Turn turn, String inputEventId) {
        update(turn, """
                UPDATE turn SET input_event_id = :inputEventId, updated_at = :now
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "inputEventId", inputEventId);
    }

    /**
     * Has the turn give way: it lets go of the session and will start over, in the same session, when
     * its run comes back. From here on a later turn of the line does not wait for it. Call it in the
     * transaction that notes how far the session was read.
     */
    public void startOver(Turn turn, Usage used) {
        update(turn, """
                UPDATE turn SET boundary_set = false, boundary_event_id = NULL, boundary_at = NULL, input_event_id = NULL,
                       yields = yields + 1, updated_at = :now,
                       model_requests = model_requests + :requests, input_tokens = input_tokens + :input,
                       output_tokens = output_tokens + :output, cache_read_tokens = cache_read_tokens + :cacheRead,
                       cache_creation_tokens = cache_creation_tokens + :cacheCreation
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                """, "requests", used.requests(), "input", used.inputTokens(), "output", used.outputTokens(),
                "cacheRead", used.cacheReadTokens(), "cacheCreation", used.cacheCreationTokens());
        afterCommit(metrics::gaveWay);
    }

    /** Notes that the turn waits for people, its run about to be suspended in the Inbox. */
    public void park(Turn turn) {
        update(turn, "UPDATE turn SET parked = true, updated_at = :now WHERE run_id = :runId AND epoch = :epoch AND state = 'open'");
    }

    /** Ends the turn. Call it in the transaction that stores the turn's result, so that both happen or neither. */
    public void finish(Turn turn, String state, String outcome, String error) {
        finish(turn, state, outcome, error, null);
    }

    /** @param note what the agent wrote at the end of a turn that no user reads; {@code null} for none */
    public void finish(Turn turn, String state, String outcome, String error, String note) {
        finish(turn, state, outcome, error, note, Usage.NONE, null);
    }

    /**
     * @param used what the requests to the model used in the part of the turn that ends here; it is
     *     added to what earlier parts of the turn used, before it gave way
     * @param waitedMs for a reply: how long the user waited for it; {@code null} otherwise
     */
    public void finish(Turn turn, String state, String outcome, String error, String note, Usage used, Long waitedMs) {
        Instant now = clock.now();
        Ended ended = jdbc.sql("""
                UPDATE turn SET state = :state, outcome = :outcome, error = :error, note = :note, parked = false,
                       updated_at = :now, finished_at = :now, waited_ms = :waitedMs,
                       model_requests = model_requests + :requests, input_tokens = input_tokens + :input,
                       output_tokens = output_tokens + :output, cache_read_tokens = cache_read_tokens + :cacheRead,
                       cache_creation_tokens = cache_creation_tokens + :cacheCreation
                 WHERE run_id = :runId AND epoch = :epoch AND state = 'open'
                RETURNING kind, created_at, model_requests, input_tokens, output_tokens, cache_read_tokens, cache_creation_tokens
                """)
                .param("state", state).param("outcome", outcome).param("error", error).param("note", note)
                .param("now", Db.ts(now)).param("waitedMs", waitedMs).param("requests", used.requests())
                .param("input", used.inputTokens()).param("output", used.outputTokens())
                .param("cacheRead", used.cacheReadTokens()).param("cacheCreation", used.cacheCreationTokens())
                .param("runId", turn.runId()).param("epoch", turn.epoch())
                .query(Turns::ended)
                .optional()
                .orElseThrow(() -> new StaleTurnException(turn.runId(), turn.epoch()));
        afterCommit(() -> metrics.ended(ended.kind(), outcome != null ? outcome : state, Duration.between(ended.createdAt(), now),
                ended.used(), waitedMs));
    }

    /** What a turn that has just ended was, for the meters. */
    private record Ended(String kind, Instant createdAt, Usage used) {
    }

    private static Ended ended(ResultSet rs, int rowNumber) throws SQLException {
        return new Ended(rs.getString("kind"), Db.instant(rs, "created_at"), new Usage(rs.getInt("model_requests"),
                rs.getLong("input_tokens"), rs.getLong("output_tokens"), rs.getLong("cache_read_tokens"),
                rs.getLong("cache_creation_tokens")));
    }

    /** Counts what happened only once it is certain: when the transaction that wrote it is through, or at once without one. */
    private static void afterCommit(Runnable count) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    count.run();
                }
            });
        } else {
            count.run();
        }
    }

    /**
     * Checks, inside the caller's transaction, that the turn is still this worker's. Whatever the
     * transaction writes next to it is then written by the worker that holds the run, or not at all.
     */
    public void stillHeld(Turn turn) {
        update(turn, "UPDATE turn SET updated_at = :now WHERE run_id = :runId AND epoch = :epoch AND state = 'open'");
    }

    /** Why the turn has to finish without waiting for anybody again, as it stands now; {@code null} when it does not. */
    public String closing(String runId) {
        return jdbc.sql("SELECT closing FROM turn WHERE run_id = :runId").param("runId", runId)
                .query(String.class).optional().orElse(null);
    }

    /** The open turns of a line, oldest first. */
    public List<Turn> openOn(String appId, String userId, String mergeKey) {
        return jdbc.sql("SELECT " + COLUMNS + """
                 FROM turn WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND state = 'open'
                 ORDER BY created_at, run_id
                """)
                .param("appId", appId).param("userId", userId).param("mergeKey", mergeKey)
                .query(Turns::row)
                .list();
    }

    /** Open turns that nobody has touched since {@code before}: candidates for a run that ended without this service. */
    public List<Turn> openAndUntouchedSince(Instant before, int limit) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM turn WHERE state = 'open' AND updated_at < :before ORDER BY updated_at LIMIT :limit")
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
        Instant now = clock.now();
        Optional<Ended> ended = jdbc.sql("""
                UPDATE turn SET state = 'failed', error = :error, parked = false, updated_at = :now, finished_at = :now
                 WHERE run_id = :runId AND state = 'open'
                RETURNING kind, created_at, model_requests, input_tokens, output_tokens, cache_read_tokens, cache_creation_tokens
                """)
                .param("error", error).param("now", Db.ts(now)).param("runId", runId)
                .query(Turns::ended)
                .optional();
        ended.ifPresent(turn -> afterCommit(() -> metrics.ended(turn.kind(), FAILED, Duration.between(turn.createdAt(), now),
                turn.used(), null)));
        return ended.isPresent();
    }

    public Optional<Turn> find(String runId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM turn WHERE run_id = :runId")
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

    private static final String COLUMNS = """
             run_id, merge_key, epoch, state, generation, cma_session_id, boundary_set, boundary_event_id, boundary_at,
             input_event_id, error, created_at, deadline_at, closing, work::text AS work, parked, app_id, user_id, yields
            """;

    private static Turn row(ResultSet rs, int rowNumber) throws SQLException {
        int generation = rs.getInt("generation");
        Integer generationOrNull = rs.wasNull() ? null : generation;
        String work = rs.getString("work");
        return new Turn(rs.getString("run_id"), rs.getString("merge_key"), rs.getInt("epoch"), rs.getString("state"),
                generationOrNull, rs.getString("cma_session_id"), rs.getBoolean("boundary_set"),
                rs.getString("boundary_event_id"), Db.instant(rs, "boundary_at"), rs.getString("input_event_id"),
                rs.getString("error"), Db.instant(rs, "created_at"), Db.instant(rs, "deadline_at"), rs.getString("closing"),
                work == null ? null : Json.STORED.readTree(work), rs.getBoolean("parked"), rs.getString("app_id"),
                rs.getString("user_id"), rs.getInt("yields"));
    }
}
