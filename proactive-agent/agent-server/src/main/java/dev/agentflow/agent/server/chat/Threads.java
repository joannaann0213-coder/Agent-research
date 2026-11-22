package dev.agentflow.agent.server.chat;

import dev.agentflow.agent.cma.AgentRef;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.time.AgentClock;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * The lines of work with a user: one per conversation. A line's key is the merge key of its events
 * in the Event Inbox, which is what makes the Inbox run them one at a time.
 */
@Service
public class Threads {

    public static final String CHAT_PREFIX = "conv:";

    /**
     * @param generation which session of the line this is; 0 while there is none
     * @param sessionId the session on the agent platform, or {@code null}
     * @param sessionAgent the agent version the session was created on, or {@code null}
     * @param lastEventId the last event of the session that a finished turn has seen, or {@code null}
     */
    public record Thread(String appId, String userId, String mergeKey, String kind, String conversationId,
            String agentKey, int generation, String sessionId, AgentRef sessionAgent, String lastEventId,
            Instant lastEventAt) {
    }

    private final JdbcClient jdbc;
    private final AgentClock clock;

    public Threads(JdbcClient jdbc, AgentClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public static String chatKey(String conversationId) {
        return CHAT_PREFIX + conversationId;
    }

    /** The conversation's line, started with {@code agentKey} if this is its first message. */
    public Thread ensureChat(String appId, String userId, String conversationId, String agentKey) {
        Instant now = clock.now();
        jdbc.sql("""
                INSERT INTO thread (app_id, user_id, merge_key, kind, conversation_id, agent_key, created_at, updated_at)
                VALUES (:appId, :userId, :mergeKey, 'chat', :conversationId, :agentKey, :now, :now)
                ON CONFLICT (app_id, user_id, merge_key) DO NOTHING
                """)
                .param("appId", appId).param("userId", userId).param("mergeKey", chatKey(conversationId))
                .param("conversationId", conversationId).param("agentKey", agentKey).param("now", Db.ts(now))
                .update();
        return find(appId, userId, chatKey(conversationId)).orElseThrow();
    }

    public Optional<Thread> find(String appId, String userId, String mergeKey) {
        return jdbc.sql("""
                SELECT app_id, user_id, merge_key, kind, conversation_id, agent_key, generation, cma_session_id,
                       cma_agent_id, cma_agent_version, last_event_id, last_event_at
                  FROM thread WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey
                """)
                .param("appId", appId).param("userId", userId).param("mergeKey", mergeKey)
                .query(Threads::row)
                .optional();
    }

    /**
     * Records the line's first session. When another worker got there first its session stays, and
     * the one passed here is simply never used: a session that gets no event does nothing and costs
     * nothing.
     */
    public Thread startSession(Thread thread, String sessionId, AgentRef agent) {
        jdbc.sql("""
                UPDATE thread
                   SET generation = generation + 1, cma_session_id = :sessionId, cma_agent_id = :agentId,
                       cma_agent_version = :agentVersion, last_event_id = NULL, last_event_at = NULL, updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND generation = :generation
                """)
                .param("sessionId", sessionId).param("agentId", agent.id()).param("agentVersion", agent.version())
                .param("now", Db.ts(clock.now())).param("appId", thread.appId()).param("userId", thread.userId())
                .param("mergeKey", thread.mergeKey()).param("generation", thread.generation())
                .update();
        return find(thread.appId(), thread.userId(), thread.mergeKey()).orElseThrow();
    }

    /** Remembers how far a finished turn has read the session. */
    public void sawUpTo(Thread thread, String eventId, Instant eventAt) {
        jdbc.sql("""
                UPDATE thread SET last_event_id = :eventId, last_event_at = :eventAt, updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND generation = :generation
                """)
                .param("eventId", eventId).param("eventAt", Db.ts(eventAt)).param("now", Db.ts(clock.now()))
                .param("appId", thread.appId()).param("userId", thread.userId()).param("mergeKey", thread.mergeKey())
                .param("generation", thread.generation())
                .update();
    }

    private static Thread row(ResultSet rs, int rowNumber) throws SQLException {
        String agentId = rs.getString("cma_agent_id");
        return new Thread(rs.getString("app_id"), rs.getString("user_id"), rs.getString("merge_key"), rs.getString("kind"),
                rs.getString("conversation_id"), rs.getString("agent_key"), rs.getInt("generation"),
                rs.getString("cma_session_id"), agentId == null ? null : new AgentRef(agentId, rs.getInt("cma_agent_version")),
                rs.getString("last_event_id"), Db.instant(rs, "last_event_at"));
    }
}
