package dev.agentflow.agent.server.chat;

import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessagePage;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.server.actions.Actions;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.delivery.Outbound;
import dev.agentflow.agent.api.Update;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.ObjectNode;

/**
 * What users and agents say to each other. A user's message is stored here and announced to the
 * Event Inbox in the same transaction; the Inbox turns it into a run, and the run's result comes
 * back here as the agent's reply.
 *
 * <p>A message that is to get a turn waits until a turn takes it. A turn takes everything that
 * waits in its conversation, whichever run announced it, so that the agent answers knowing all the
 * user has written; and a turn that is ended because the user wrote again gives its messages back,
 * for the next turn to answer with the new one.
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
    private final Actions actions;
    private final Outbound outbound;
    private final AgentClock clock;
    private final Ids ids;

    public Messages(JdbcClient jdbc, TransactionTemplate tx, Users users, Threads threads, AgentProfiles profiles,
            InboxRelay relay, Actions actions, Outbound outbound, AgentClock clock, Ids ids) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.threads = threads;
        this.profiles = profiles;
        this.relay = relay;
        this.actions = actions;
        this.outbound = outbound;
        this.clock = clock;
        this.ids = ids;
    }

    /**
     * Stores a user's message and queues the event that will get it answered.
     *
     * <p>When the agent waits for the user to answer a question in the conversation, the message is
     * that answer: the turn that asked goes on with it, and it does not start a turn of its own.
     * Otherwise it is new input for the conversation, and whatever the conversation waits for people
     * to decide is withdrawn: the waiting turn finishes, and the message comes in the next one. A
     * question that a main agent asked in passing is answered by the message too, but no turn waits
     * for it: the message is input like any other, and the agent reads its answer there.
     */
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
                return new MessageAccepted(messageId, conversationId, earlier.get().seq(), earlier.get().createdAt(), true,
                        earlier.get().actionId());
            }

            Optional<Actions.Action> question = actions.latestPendingQuestion(appId, userId, conversationId);
            if (question.isPresent() && !question.get().inPassing()) {
                Instant now = clock.now();
                long seq = users.nextSeq(appId, userId);
                // The turn that asked goes on with it: it is not to get a turn of its own.
                insert(appId, userId, seq, conversationId, messageId, "user", "message", text, null, now,
                        question.get().mergeKey(), question.get().actionId(), null, false);
                actions.settle(question.get(), Actions.ANSWERED, text, null, Actions.BY_USER);
                actions.ring(appId, userId, question.get().mergeKey(), question.get().runId());
                return new MessageAccepted(messageId, conversationId, seq, now, false, question.get().actionId());
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
            String line = Threads.chatKey(conversationId);
            // What a main agent asked in passing is answered by this message, which is a turn of its own all the same.
            String answered = question.map(Actions.Action::actionId).orElse(null);
            insert(appId, userId, seq, conversationId, messageId, "user", "message", text, null, now, line, answered, null, true);
            question.ifPresent(asked -> actions.settle(asked, Actions.ANSWERED, text, null, Actions.BY_USER));
            actions.supersede(appId, userId, line, Actions.supersededNote(line));
            announce(appId, userId, conversationId, messageId, now);
            return new MessageAccepted(messageId, conversationId, seq, now, false, answered);
        });
    }

    /**
     * Stores the user's answer to a question that a main agent asked in passing, given in the
     * application instead of by writing to the conversation, and has it answered like any message of
     * the user: no turn waited for it. In the caller's transaction, under the user's lock.
     */
    public void answerInPassing(String appId, String userId, String conversationId, String messageId, String text,
            String actionId) {
        if (find(appId, userId, conversationId, messageId).isPresent()) {
            return;
        }
        Instant now = clock.now();
        String line = Threads.chatKey(conversationId);
        long seq = users.nextSeq(appId, userId);
        insert(appId, userId, seq, conversationId, messageId, "user", "message", text, null, now, line, actionId, null, true);
        actions.supersede(appId, userId, line, Actions.supersededNote(line));
        announce(appId, userId, conversationId, messageId, now);
    }

    /**
     * Queues the Inbox event that gets a user's message answered. The message itself is stored as
     * waiting for a turn: a turn in the background that holds the conversation's line gives way to
     * it, and for a reply that is under way it is the user writing again.
     */
    private void announce(String appId, String userId, String conversationId, String messageId, Instant now) {
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
    }

    /**
     * Stores something the agent said, inside the caller's transaction. The id is derived from the
     * run that produced it, so storing it a second time changes nothing.
     *
     * @param mergeKey the line that said it
     * @param actionId for a question: the action that waits for the answer
     * @return whether it was stored by this call
     */
    public boolean storeFromAgent(String appId, String userId, String conversationId, String mergeKey, String messageId,
            String kind, String text, String runId, String actionId) {
        users.lock(appId, userId);
        if (find(appId, userId, conversationId, messageId).isPresent()) {
            return false;
        }
        insert(appId, userId, users.nextSeq(appId, userId), conversationId, messageId, "agent", kind, text, runId, clock.now(),
                mergeKey, actionId, null, false);
        return true;
    }

    /**
     * Puts work that a main agent offers to do before the user, in the conversation: what the agent
     * wrote about it for them, as a message that names the task. In the caller's transaction, under
     * the user's lock. Storing it a second time changes nothing.
     *
     * @param mergeKey the main agent's line
     */
    public void storeProposal(String appId, String userId, String conversationId, String mergeKey, String taskId, String text,
            String runId) {
        String messageId = "proposal:" + taskId;
        if (find(appId, userId, conversationId, messageId).isEmpty()) {
            insert(appId, userId, users.nextSeq(appId, userId), conversationId, messageId, "agent", "proposal", text, runId,
                    clock.now(), mergeKey, null, taskId, false);
        }
    }

    /**
     * Stores the user's answer to a question that was given another way than by writing to the
     * conversation, so that the conversation shows it. In the caller's transaction, under the user's lock.
     */
    public void storeAnswer(String appId, String userId, String conversationId, String messageId, String text,
            String mergeKey, String actionId) {
        if (find(appId, userId, conversationId, messageId).isEmpty()) {
            insert(appId, userId, users.nextSeq(appId, userId), conversationId, messageId, "user", "message", text, null,
                    clock.now(), mergeKey, actionId, null, false);
        }
    }

    // ================================================================ what waits for a turn

    /**
     * Gives the turn of a run what the user has written to a conversation and no turn has yet. The
     * messages that the run announces are its own unless another turn has them; whatever else waits
     * in the conversation is added, oldest first, while it fits. A turn that has messages keeps
     * them, so this can be asked again for what came since. In the caller's transaction, under the
     * user's lock.
     *
     * @param announced the positions of the messages that the run announces
     * @param stoppedThrough the user's messages up to this position are not answered any more: the
     *     turn takes its own of them, so that they wait for nobody, and is not given them
     * @param limit how many messages a turn has at most; its own are taken whatever their number
     * @param chars how much text a turn has at most; its own are taken whatever their length
     * @return what the turn has to answer, oldest first; empty when nothing is left for it
     */
    public List<MessageView> take(String appId, String userId, String conversationId, String runId, List<Long> announced,
            long stoppedThrough, int limit, int chars) {
        List<MessageView> own = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND role = 'user'
                   AND (taken_by = :runId OR (taken_by IS NULL AND seq IN (:announced)))
                 ORDER BY seq
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId).param("runId", runId)
                .param("announced", announced.isEmpty() ? List.of(-1L) : announced)
                .query(Messages::view)
                .list();
        List<MessageView> waiting = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId
                   AND announced AND taken_by IS NULL AND seq > :stoppedThrough
                 ORDER BY seq
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("stoppedThrough", stoppedThrough).param("limit", limit)
                .query(Messages::view)
                .list();
        TreeMap<Long, MessageView> taken = new TreeMap<>();
        int size = 0;
        for (MessageView message : own) {
            taken.put(message.seq(), message);
            size += message.seq() > stoppedThrough ? message.text().length() : 0;
        }
        for (MessageView message : waiting) {
            if (taken.containsKey(message.seq())) {
                continue;
            }
            if (taken.size() >= limit || (size > 0 && size + message.text().length() > chars)) {
                // The rest waits for the next turn, in order: nothing newer is taken past it.
                break;
            }
            taken.put(message.seq(), message);
            size += message.text().length();
        }
        if (!taken.isEmpty()) {
            jdbc.sql("""
                    UPDATE message SET taken_by = :runId, announced = true
                     WHERE app_id = :appId AND user_id = :userId AND seq IN (:seqs) AND (taken_by IS NULL OR taken_by = :runId)
                    """)
                    .param("runId", runId).param("appId", appId).param("userId", userId)
                    .param("seqs", new ArrayList<>(taken.keySet()))
                    .update();
        }
        return taken.values().stream().filter(message -> message.seq() > stoppedThrough).toList();
    }

    /**
     * Gives back the messages of a turn that was ended because the user wrote again: they wait for a
     * turn once more, and the next one answers them with what came since. In the caller's
     * transaction, under the user's lock.
     */
    public void release(String appId, String userId, List<MessageView> messages) {
        if (messages.isEmpty()) {
            return;
        }
        jdbc.sql("""
                UPDATE message SET taken_by = NULL, announced = true
                 WHERE app_id = :appId AND user_id = :userId AND seq IN (:seqs) AND role = 'user'
                """)
                .param("appId", appId).param("userId", userId)
                .param("seqs", messages.stream().map(MessageView::seq).toList())
                .update();
    }

    /**
     * Whether the user wrote to a conversation since {@code since} and no turn has taken the
     * message yet: somebody is waiting for an answer.
     */
    public boolean userWaits(String appId, String userId, String conversationId, Instant since) {
        return waits(appId, userId, conversationId, 0, since);
    }

    /**
     * Whether the user has written to a conversation after position {@code afterSeq} and no turn has
     * taken the message: for a turn that took its messages when that was the user's latest, they
     * have written again.
     */
    public boolean wroteAgain(String appId, String userId, String conversationId, long afterSeq) {
        return waits(appId, userId, conversationId, afterSeq, Instant.EPOCH);
    }

    /** Messages that the user stopped wait for nobody. */
    private boolean waits(String appId, String userId, String conversationId, long afterSeq, Instant since) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM message m
                     WHERE m.app_id = :appId AND m.user_id = :userId AND m.conversation_id = :conversationId
                       AND m.announced AND m.taken_by IS NULL AND m.seq > :afterSeq AND m.created_at > :since
                       AND m.seq > COALESCE((SELECT t.stopped_through FROM thread t
                                              WHERE t.app_id = m.app_id AND t.user_id = m.user_id AND t.merge_key = :mergeKey), 0))
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("afterSeq", afterSeq).param("since", Db.ts(since)).param("mergeKey", Threads.chatKey(conversationId))
                .query(Boolean.class).single();
    }

    // ================================================================ reading

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
     * The last {@code limit} things the user and the line {@code mergeKey} said to each other in a
     * conversation before position {@code beforeSeq}, oldest first.
     */
    public List<MessageView> dialogueBefore(String appId, String userId, String conversationId, String mergeKey,
            long beforeSeq, int limit) {
        List<MessageView> newestFirst = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND seq < :beforeSeq
                   AND merge_key = :mergeKey
                 ORDER BY seq DESC
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("beforeSeq", beforeSeq).param("mergeKey", mergeKey).param("limit", limit)
                .query(Messages::view)
                .list();
        return newestFirst.reversed();
    }

    /**
     * What other lines than {@code mergeKey} exchanged with the user in a conversation after position
     * {@code afterSeq}: notifications and questions of work in the background, and the user's answers
     * to them. Oldest first; when there are more than {@code limit}, the latest ones.
     */
    public List<MessageView> outsideAfter(String appId, String userId, String conversationId, String mergeKey, long afterSeq,
            int limit) {
        List<MessageView> newestFirst = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND seq > :afterSeq
                   AND merge_key <> :mergeKey
                 ORDER BY seq DESC
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("afterSeq", afterSeq).param("mergeKey", mergeKey).param("limit", limit)
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

    /** @param announced whether the message is the user's and is to get a turn: it waits until a turn takes it */
    private void insert(String appId, String userId, long seq, String conversationId, String messageId, String role,
            String kind, String text, String runId, Instant now, String mergeKey, String actionId, String taskId,
            boolean announced) {
        jdbc.sql("""
                INSERT INTO message (app_id, user_id, seq, conversation_id, message_id, role, kind, text, run_id, created_at,
                                     merge_key, action_id, task_id, announced)
                VALUES (:appId, :userId, :seq, :conversationId, :messageId, :role, :kind, :text, :runId, :now, :mergeKey,
                        :actionId, :taskId, :announced)
                """)
                .param("appId", appId).param("userId", userId).param("seq", seq).param("conversationId", conversationId)
                .param("messageId", messageId).param("role", role).param("kind", kind).param("text", text)
                .param("runId", runId).param("now", Db.ts(now)).param("mergeKey", mergeKey).param("actionId", actionId)
                .param("taskId", taskId).param("announced", announced)
                .update();
        outbound.record(appId, userId, Update.MESSAGE_CREATED,
                new MessageView(seq, messageId, conversationId, role, kind, text, now, actionId, taskId));
    }

    private static final String SELECT = """
            SELECT seq, message_id, conversation_id, role, kind, text, created_at, action_id, task_id FROM message
            """;

    private static MessageView view(ResultSet rs, int rowNumber) throws SQLException {
        return new MessageView(rs.getLong("seq"), rs.getString("message_id"), rs.getString("conversation_id"),
                rs.getString("role"), rs.getString("kind"), rs.getString("text"), Db.instant(rs, "created_at"),
                rs.getString("action_id"), rs.getString("task_id"));
    }
}
