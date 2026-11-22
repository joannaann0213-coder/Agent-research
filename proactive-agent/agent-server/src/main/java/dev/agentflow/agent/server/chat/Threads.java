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
 * The lines of work with a user: one per conversation, and one per subject of the application's
 * events. A line's key is the merge key of its events in the Event Inbox, which is what makes the
 * Inbox run them one at a time.
 */
@Service
public class Threads {

    public static final String CHAT_PREFIX = "conv:";
    public static final String TASK_PREFIX = "task:";

    public static final String CHAT = "chat";
    public static final String TASK = "task";

    /**
     * @param generation which session of the line this is; 0 while there is none
     * @param sessionId the session on the agent platform, or {@code null}
     * @param sessionAgent the agent version the session was created on, or {@code null}
     * @param lastEventId the last event of the session that a finished turn has seen, or {@code null}
     * @param syncedSeq for a conversation: the position of the last notification its session has read
     * @param stoppedThrough for a conversation: the user's messages up to this position are not answered any more
     */
    public record Thread(String appId, String userId, String mergeKey, String kind, String conversationId,
            String agentKey, int generation, String sessionId, AgentRef sessionAgent, String lastEventId,
            Instant lastEventAt, long syncedSeq, long stoppedThrough) {

        public boolean chat() {
            return CHAT.equals(kind);
        }
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

    public static String taskKey(String subject) {
        return TASK_PREFIX + subject;
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

    /** The line of a subject of the application's events, started with {@code agentKey} if this is its first event. */
    public Thread ensureTask(String appId, String userId, String subject, String agentKey) {
        Instant now = clock.now();
        jdbc.sql("""
                INSERT INTO thread (app_id, user_id, merge_key, kind, agent_key, created_at, updated_at)
                VALUES (:appId, :userId, :mergeKey, 'task', :agentKey, :now, :now)
                ON CONFLICT (app_id, user_id, merge_key) DO NOTHING
                """)
                .param("appId", appId).param("userId", userId).param("mergeKey", taskKey(subject))
                .param("agentKey", agentKey).param("now", Db.ts(now))
                .update();
        return find(appId, userId, taskKey(subject)).orElseThrow();
    }

    public Optional<Thread> find(String appId, String userId, String mergeKey) {
        return jdbc.sql("""
                SELECT app_id, user_id, merge_key, kind, conversation_id, agent_key, generation, cma_session_id,
                       cma_agent_id, cma_agent_version, last_event_id, last_event_at, synced_seq, stopped_through
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
                   AND cma_session_id IS NULL
                """)
                .param("sessionId", sessionId).param("agentId", agent.id()).param("agentVersion", agent.version())
                .param("now", Db.ts(clock.now())).param("appId", thread.appId()).param("userId", thread.userId())
                .param("mergeKey", thread.mergeKey()).param("generation", thread.generation())
                .update();
        return find(thread.appId(), thread.userId(), thread.mergeKey()).orElseThrow();
    }

    /**
     * Gives the line's session up. The line keeps its generation count; its next turn starts a new
     * session. Does nothing when the line has moved on to another session already.
     */
    public void dropSession(Thread thread) {
        jdbc.sql("""
                UPDATE thread SET cma_session_id = NULL, last_event_id = NULL, last_event_at = NULL, updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND generation = :generation
                   AND cma_session_id = :sessionId
                """)
                .param("now", Db.ts(clock.now())).param("appId", thread.appId()).param("userId", thread.userId())
                .param("mergeKey", thread.mergeKey()).param("generation", thread.generation())
                .param("sessionId", thread.sessionId())
                .update();
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

    /** Stops the conversation's messages up to position {@code seq}: they are not answered any more. */
    public void stopThrough(Thread thread, long seq) {
        jdbc.sql("""
                UPDATE thread SET stopped_through = GREATEST(stopped_through, :seq), updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey
                """)
                .param("seq", seq).param("now", Db.ts(clock.now())).param("appId", thread.appId())
                .param("userId", thread.userId()).param("mergeKey", thread.mergeKey())
                .update();
    }

    /** How far the conversation's messages are stopped, as it stands now. */
    public long stoppedThrough(String appId, String userId, String mergeKey) {
        return jdbc.sql("SELECT stopped_through FROM thread WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey")
                .param("appId", appId).param("userId", userId).param("mergeKey", mergeKey)
                .query(Long.class).optional().orElse(0L);
    }

    /** Notes that the conversation's session has read the notifications up to position {@code seq}. */
    public void synced(Thread thread, long seq) {
        jdbc.sql("""
                UPDATE thread SET synced_seq = GREATEST(synced_seq, :seq), updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey
                """)
                .param("seq", seq).param("now", Db.ts(clock.now())).param("appId", thread.appId())
                .param("userId", thread.userId()).param("mergeKey", thread.mergeKey())
                .update();
    }

    /**
     * Notes that the conversation's session knows of nothing after position {@code seq}: it is a new
     * session, which was told only so much of what came before.
     */
    public void unsyncedAfter(Thread thread, long seq) {
        jdbc.sql("""
                UPDATE thread SET synced_seq = LEAST(synced_seq, :seq), updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey
                """)
                .param("seq", seq).param("now", Db.ts(clock.now())).param("appId", thread.appId())
                .param("userId", thread.userId()).param("mergeKey", thread.mergeKey())
                .update();
    }

    private static Thread row(ResultSet rs, int rowNumber) throws SQLException {
        String agentId = rs.getString("cma_agent_id");
        return new Thread(rs.getString("app_id"), rs.getString("user_id"), rs.getString("merge_key"), rs.getString("kind"),
                rs.getString("conversation_id"), rs.getString("agent_key"), rs.getInt("generation"),
                rs.getString("cma_session_id"), agentId == null ? null : new AgentRef(agentId, rs.getInt("cma_agent_version")),
                rs.getString("last_event_id"), Db.instant(rs, "last_event_at"), rs.getLong("synced_seq"),
                rs.getLong("stopped_through"));
    }
}
