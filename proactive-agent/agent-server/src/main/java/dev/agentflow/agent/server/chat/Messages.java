package dev.agentflow.agent.server.chat;

import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessagePage;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.relay.InboxRelay;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.ObjectNode;

/**
 * What users and agents say to each other. A user's message is stored here and announced to the
 * Event Inbox in the same transaction; the Inbox turns it into a run, and the run's result comes
 * back here as the agent's reply.
 */
@Service
public class Messages {

    /** The type of the Inbox event that announces a user's message. Its payload names the message; the text stays here. */
    public static final String EVENT_TYPE = "agent.user_message.v1";

    public static final String DEFAULT_CONVERSATION = "default";
    public static final String DEFAULT_AGENT = "default";
    public static final int MAX_TEXT = 32_000;

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Users users;
    private final Threads threads;
    private final AgentProfiles profiles;
    private final InboxRelay relay;
    private final AgentClock clock;
    private final Ids ids;

    public Messages(JdbcClient jdbc, TransactionTemplate tx, Users users, Threads threads, AgentProfiles profiles,
            InboxRelay relay, AgentClock clock, Ids ids) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.threads = threads;
        this.profiles = profiles;
        this.relay = relay;
        this.clock = clock;
        this.ids = ids;
    }

    /** Stores a user's message and queues the event that will get it answered. */
    public MessageAccepted post(String appId, String userId, PostMessageRequest request) {
        String conversationId = request.conversationId() == null ? DEFAULT_CONVERSATION
                : Names.localId(request.conversationId(), "conversation_id");
        String messageId = request.messageId() == null ? ids.next("msg") : Names.localId(request.messageId(), "message_id");
        String agentKey = request.agent() == null ? null : Names.agentKey(request.agent());
        String text = request.text();
        if (text == null || text.isBlank()) {
            throw AgentException.invalid("text is required");
        }
        if (text.length() > MAX_TEXT) {
            throw AgentException.tooLarge("text has at most " + MAX_TEXT + " characters");
        }
        return tx.execute(status -> {
            users.ensure(appId, userId);
            // From here on everything about this user happens one at a time: a retry of the same
            // message cannot slip in between the check and the insert.
            users.lock(appId, userId);

            Optional<MessageView> earlier = find(appId, userId, conversationId, messageId);
            if (earlier.isPresent()) {
                if (!earlier.get().role().equals("user") || !earlier.get().text().equals(text)) {
                    throw AgentException.conflict("message_id " + messageId + " was already used for another message of this conversation");
                }
                return new MessageAccepted(messageId, conversationId, earlier.get().seq(), earlier.get().createdAt(), true);
            }

            Optional<Threads.Thread> existing = threads.find(appId, userId, Threads.chatKey(conversationId));
            if (existing.isPresent() && agentKey != null && !existing.get().agentKey().equals(agentKey)) {
                throw AgentException.conflict("conversation " + conversationId + " is served by agent "
                        + existing.get().agentKey() + "; a conversation keeps the agent of its first message");
            }
            if (existing.isEmpty()) {
                String startWith = agentKey == null ? DEFAULT_AGENT : agentKey;
                if (!profiles.exists(appId, startWith)) {
                    throw AgentException.invalid("this application has no agent " + startWith
                            + "; create it with PUT /v1/agents/" + startWith);
                }
                threads.ensureChat(appId, userId, conversationId, startWith);
            }

            Instant now = clock.now();
            long seq = users.nextSeq(appId, userId);
            insert(appId, userId, seq, conversationId, messageId, "user", "message", text, null, now);

            ObjectNode payload = InboxJson.mapper().createObjectNode();
            payload.put("conversation_id", conversationId).put("message_id", messageId);
            relay.enqueue(appId, userId, WriteEventRequest.builder(EventSource.CONVERSATION, EVENT_TYPE)
                    .sourceEventId("msg:" + conversationId + ":" + messageId)
                    .mergeKey(Threads.chatKey(conversationId))
                    .lane(Lane.INTERACTIVE)
                    // Stopping a turn is asked for explicitly; the Inbox's guess from the words is not wanted.
                    .interrupt(false)
                    .occurredAt(now)
                    .payload(payload)
                    .build());
            return new MessageAccepted(messageId, conversationId, seq, now, false);
        });
    }

    /**
     * Stores something the agent said, inside the caller's transaction. The id is derived from the
     * run that produced it, so storing it a second time changes nothing.
     *
     * @return whether it was stored by this call
     */
    public boolean storeFromAgent(String appId, String userId, String conversationId, String messageId, String kind,
            String text, String runId) {
        users.lock(appId, userId);
        if (find(appId, userId, conversationId, messageId).isPresent()) {
            return false;
        }
        insert(appId, userId, users.nextSeq(appId, userId), conversationId, messageId, "agent", kind, text, runId, clock.now());
        return true;
    }

    /**
     * @param conversationId only this conversation; {@code null} for all of the user's
     * @param after only positions greater than this
     */
    public MessagePage list(String appId, String userId, String conversationId, long after, int limit) {
        Names.userId(userId);
        if (conversationId != null) {
            Names.localId(conversationId, "conversation_id");
        }
        List<MessageView> messages = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND seq > :after
                   AND (CAST(:conversationId AS text) IS NULL OR conversation_id = :conversationId)
                 ORDER BY seq
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("after", after)
                .param("conversationId", conversationId).param("limit", limit)
                .query(Messages::view)
                .list();
        return new MessagePage(messages, messages.isEmpty() ? after : messages.getLast().seq());
    }

    /**
     * The last {@code limit} things the user and the agent said to each other in a conversation before
     * position {@code beforeSeq}, oldest first. Notifications are not part of that exchange.
     */
    public List<MessageView> dialogueBefore(String appId, String userId, String conversationId, long beforeSeq, int limit) {
        List<MessageView> newestFirst = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND seq < :beforeSeq
                   AND kind IN ('message', 'reply')
                 ORDER BY seq DESC
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("beforeSeq", beforeSeq).param("limit", limit)
                .query(Messages::view)
                .list();
        return newestFirst.reversed();
    }

    /**
     * The notifications of a conversation after position {@code afterSeq}, oldest first; when there
     * are more than {@code limit}, the latest ones.
     */
    public List<MessageView> notificationsAfter(String appId, String userId, String conversationId, long afterSeq, int limit) {
        List<MessageView> newestFirst = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND seq > :afterSeq
                   AND kind = 'notification'
                 ORDER BY seq DESC
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("afterSeq", afterSeq).param("limit", limit)
                .query(Messages::view)
                .list();
        return newestFirst.reversed();
    }

    /** How many notifications the user was sent since {@code since}, in all conversations. */
    public long notificationsSince(String appId, String userId, Instant since) {
        return jdbc.sql("""
                SELECT count(*) FROM message
                 WHERE app_id = :appId AND user_id = :userId AND kind = 'notification' AND created_at > :since
                """)
                .param("appId", appId).param("userId", userId).param("since", Db.ts(since))
                .query(Long.class).single();
    }

    /** How many notifications a run has sent. */
    public long notificationsOf(String appId, String userId, String runId) {
        return jdbc.sql("""
                SELECT count(*) FROM message
                 WHERE app_id = :appId AND user_id = :userId AND kind = 'notification' AND run_id = :runId
                """)
                .param("appId", appId).param("userId", userId).param("runId", runId)
                .query(Long.class).single();
    }

    public Optional<MessageView> find(String appId, String userId, String conversationId, String messageId) {
        return jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND message_id = :messageId
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("messageId", messageId)
                .query(Messages::view)
                .optional();
    }

    private void insert(String appId, String userId, long seq, String conversationId, String messageId, String role,
            String kind, String text, String runId, Instant now) {
        jdbc.sql("""
                INSERT INTO message (app_id, user_id, seq, conversation_id, message_id, role, kind, text, run_id, created_at)
                VALUES (:appId, :userId, :seq, :conversationId, :messageId, :role, :kind, :text, :runId, :now)
                """)
                .param("appId", appId).param("userId", userId).param("seq", seq).param("conversationId", conversationId)
                .param("messageId", messageId).param("role", role).param("kind", kind).param("text", text)
                .param("runId", runId).param("now", Db.ts(now))
                .update();
    }

    private static final String SELECT = """
            SELECT seq, message_id, conversation_id, role, kind, text, created_at FROM message
            """;

    private static MessageView view(ResultSet rs, int rowNumber) throws SQLException {
        return new MessageView(rs.getLong("seq"), rs.getString("message_id"), rs.getString("conversation_id"),
                rs.getString("role"), rs.getString("kind"), rs.getString("text"), Db.instant(rs, "created_at"));
    }
}
