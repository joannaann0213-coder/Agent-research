package dev.agentflow.agent.server.delivery;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.time.AgentClock;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;

/**
 * The log of what happened for each user, which the stream, the updates and the webhook deliver.
 * An update is recorded in the transaction that makes it happen, so the log has exactly what
 * happened: nothing that was rolled back, nothing that was committed without it.
 */
@Service
public class Outbound {

    private final JdbcClient jdbc;
    private final Webhooks webhooks;
    private final StreamHub hub;
    private final AgentClock clock;
    private final Ids ids;

    public Outbound(JdbcClient jdbc, Webhooks webhooks, StreamHub hub, AgentClock clock, Ids ids) {
        this.jdbc = jdbc;
        this.webhooks = webhooks;
        this.hub = hub;
        this.clock = clock;
        this.ids = ids;
    }

    /**
     * Records an update for the user. The caller's transaction must hold the lock of the user's row,
     * taken before any other lock: positions are handed out under it, in the order of the commits.
     */
    public void record(String appId, String userId, String type, Object data) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("an update is recorded in the transaction that makes it happen");
        }
        long seq = jdbc.sql("""
                UPDATE app_user SET next_event_seq = next_event_seq + 1 WHERE app_id = :appId AND user_id = :userId
                RETURNING next_event_seq - 1
                """)
                .param("appId", appId).param("userId", userId)
                .query(Long.class).single();
        String eventId = ids.next("upd");
        JsonNode json = AgentJson.mapper().valueToTree(data);
        jdbc.sql("""
                INSERT INTO outbound_event (app_id, user_id, seq, event_id, type, data, created_at)
                VALUES (:appId, :userId, :seq, :eventId, :type, CAST(:data AS jsonb), :now)
                """)
                .param("appId", appId).param("userId", userId).param("seq", seq).param("eventId", eventId)
                .param("type", type).param("data", json.toString()).param("now", Db.ts(clock.now()))
                .update();
        webhooks.enqueue(appId, userId, eventId, type);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                hub.wake(appId, userId);
            }
        });
    }

    /** What a {@code run.failed} update says: which run, on which line, and why. */
    public static java.util.Map<String, Object> failure(String runId, String mergeKey, String error) {
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("run_id", runId);
        if (mergeKey != null && mergeKey.startsWith("conv:")) {
            data.put("conversation_id", mergeKey.substring("conv:".length()));
        } else if (mergeKey != null && mergeKey.startsWith("task:")) {
            data.put("subject", mergeKey.substring("task:".length()));
        }
        data.put("error", error);
        return data;
    }

    /** The user's updates after position {@code after}, oldest first. */
    public List<Update> after(String appId, String userId, long after, int limit) {
        return jdbc.sql("""
                SELECT event_id, seq, user_id, type, created_at, data::text AS data FROM outbound_event
                 WHERE app_id = :appId AND user_id = :userId AND seq > :after
                 ORDER BY seq
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("after", after).param("limit", limit)
                .query(Outbound::row)
                .list();
    }

    /** The position of the user's latest update; 0 when there is none. */
    public long last(String appId, String userId) {
        return jdbc.sql("SELECT next_event_seq - 1 FROM app_user WHERE app_id = :appId AND user_id = :userId")
                .param("appId", appId).param("userId", userId)
                .query(Long.class).optional().orElse(0L);
    }

    static Update row(ResultSet rs, int rowNumber) throws SQLException {
        Instant createdAt = Db.instant(rs, "created_at");
        return new Update(rs.getString("event_id"), rs.getLong("seq"), rs.getString("user_id"), rs.getString("type"), createdAt,
                AgentJson.mapper().readTree(rs.getString("data")));
    }
}
