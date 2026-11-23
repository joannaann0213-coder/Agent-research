package dev.agentflow.agent.server.actions;

import dev.agentflow.agent.api.ActionTool;
import dev.agentflow.agent.api.ActionView;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.delivery.Outbound;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.relay.InboxRelay;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * What waits for a person: a tool call that needs an approval, or a question to the user. The
 * {@code action} row is the authority on the outcome. It leaves {@code pending} once, by a guarded
 * update: whoever gets there first decides, and a decision is never undone.
 *
 * <p>A turn that has nothing left to do but wait for people suspends its run in the Event Inbox,
 * under the resume key {@link #resumeKey}. Whatever ends an action rings: it writes an event with
 * that key, which wakes the run. The event says nothing itself; the turn reads the rows. A ring that
 * comes after the turn has ended is an ordinary event of the line, and the turn that gets it ignores
 * it.
 *
 * <p>A main agent's question is different: it is asked <em>in passing</em>. Everything that concerns
 * the user comes in on the main agent's line, so a turn that waited there would be ended by the next
 * event or report, and the question with it. Such a question is on record like any other, but no turn
 * waits for it and nothing but the user's own word ends it early: the answer comes as the user's next
 * message, which is a turn of its own.
 */
@Service
public class Actions {

    /** The type of the Inbox event that wakes a turn waiting for people. */
    public static final String DOORBELL = "agent.action_resolved.v1";

    public static final String APPROVAL = "approval";
    public static final String QUESTION = "question";

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String DENIED = "denied";
    public static final String ANSWERED = "answered";
    public static final String EXPIRED = "expired";
    public static final String WITHDRAWN = "withdrawn";

    public static final String BY_APP = "app";
    public static final String BY_USER = "user";
    public static final String BY_SERVICE = "service";

    static final String EXPIRED_NOTE = "nobody decided in time";

    /** @param inPassing whether it is a question that no turn waits for: a main agent's */
    public record Action(String appId, String userId, String actionId, String kind, String status, String approver,
            String mergeKey, String runId, String conversationId, String sessionId, String toolUseId, String toolServer,
            String toolName, JsonNode toolInput, String question, List<String> options, String answer, String note,
            String resolvedBy, Instant createdAt, Instant expiresAt, Instant resolvedAt, boolean inPassing) {

        public boolean pending() {
            return PENDING.equals(status);
        }

        public ActionView view() {
            return new ActionView(actionId, kind, status, approver, conversationId,
                    APPROVAL.equals(kind) ? new ActionTool(toolServer, toolName, toolInput) : null, question, options,
                    answer, note, resolvedBy, createdAt, expiresAt, resolvedAt);
        }
    }

    /**
     * A call that waits for a person, as the turn found it.
     *
     * @param approver {@code app} or {@code user}
     */
    public record Call(String kind, String approver, String conversationId, String sessionId, String toolUseId,
            String toolServer, String toolName, JsonNode toolInput, String question, List<String> options) {
    }

    /** @param created whether the action was made by this call, rather than found */
    public record Ensured(Action action, boolean created) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final InboxRelay relay;
    private final Outbound outbound;
    private final AgentClock clock;
    private final Ids ids;

    public Actions(JdbcClient jdbc, TransactionTemplate tx, InboxRelay relay, Outbound outbound, AgentClock clock, Ids ids) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.relay = relay;
        this.outbound = outbound;
        this.clock = clock;
        this.ids = ids;
    }

    /** What a run that waits for people is suspended under in the Inbox, and what the events that wake it carry. */
    public static String resumeKey(String runId) {
        return "run:" + runId;
    }

    // ================================================================ for the turn

    /**
     * The action of a call, made pending if this is the first time the call is put to somebody. In
     * the caller's transaction, under the user's lock.
     */
    public Ensured ensure(String appId, String userId, String mergeKey, String runId, Call call, Duration ttl) {
        return ensure(appId, userId, mergeKey, runId, call, ttl, false, false);
    }

    /**
     * @param inPassing whether no turn waits for it: a main agent's question
     * @param unasked whether it is asked in a turn that the user did not begin
     */
    public Ensured ensure(String appId, String userId, String mergeKey, String runId, Call call, Duration ttl,
            boolean inPassing, boolean unasked) {
        Optional<Action> existing = ofCall(runId, call.toolUseId());
        if (existing.isPresent()) {
            return new Ensured(existing.get(), false);
        }
        Instant now = clock.now();
        String actionId = ids.next("act");
        jdbc.sql("""
                INSERT INTO action (app_id, user_id, action_id, kind, status, approver, merge_key, run_id, conversation_id,
                                    cma_session_id, tool_use_id, tool_server, tool_name, tool_input, question, options,
                                    created_at, expires_at, in_passing, unasked)
                VALUES (:appId, :userId, :actionId, :kind, 'pending', :approver, :mergeKey, :runId, :conversationId,
                        :sessionId, :toolUseId, :toolServer, :toolName, CAST(:toolInput AS jsonb), :question,
                        CAST(:options AS jsonb), :now, :expiresAt, :inPassing, :unasked)
                """)
                .param("inPassing", inPassing).param("unasked", unasked)
                .param("appId", appId).param("userId", userId).param("actionId", actionId).param("kind", call.kind())
                .param("approver", call.approver()).param("mergeKey", mergeKey).param("runId", runId)
                .param("conversationId", call.conversationId()).param("sessionId", call.sessionId())
                .param("toolUseId", call.toolUseId()).param("toolServer", call.toolServer()).param("toolName", call.toolName())
                .param("toolInput", call.toolInput() == null ? null : Json.STORED.writeValueAsString(call.toolInput()))
                .param("question", call.question())
                .param("options", call.options() == null ? null : Json.STORED.writeValueAsString(call.options()))
                .param("now", Db.ts(now)).param("expiresAt", Db.ts(now.plus(ttl)))
                .update();
        Action created = ofCall(runId, call.toolUseId()).orElseThrow();
        outbound.record(appId, userId, Update.ACTION_CREATED, created.view());
        return new Ensured(created, true);
    }

    public Optional<Action> ofCall(String runId, String toolUseId) {
        return jdbc.sql(SELECT + " WHERE run_id = :runId AND tool_use_id = :toolUseId")
                .param("runId", runId).param("toolUseId", toolUseId)
                .query(Actions::row)
                .optional();
    }

    /** Ends the run's pending actions whose time has come. */
    public int expireDue(String runId) {
        return endPending(runId, EXPIRED, EXPIRED_NOTE, true);
    }

    /** When the first of the pending actions that the run waits for expires, if it has any. */
    public Optional<Instant> earliestExpiry(String runId) {
        return jdbc.sql("SELECT min(expires_at) AS earliest FROM action WHERE run_id = :runId AND status = 'pending' AND NOT in_passing")
                .param("runId", runId)
                .query((rs, row) -> Db.instant(rs, "earliest"))
                .optional();
    }

    /** Withdraws the run's pending actions: what they are about no longer applies. */
    public int withdrawOfRun(String runId, String note) {
        return endPending(runId, WITHDRAWN, note, false);
    }

    /**
     * Ends the pending actions that the run waits for, in a transaction of its own or the caller's.
     * A question asked in passing is not among them: no turn waits for it. The user's row is locked
     * first, as everywhere that records updates for the user.
     */
    private int endPending(String runId, String status, String note, boolean onlyDue) {
        String due = " AND NOT in_passing" + (onlyDue ? " AND expires_at <= :now" : "");
        Integer ended = tx.execute(transaction -> {
            Instant now = clock.now();
            Optional<String[]> owner = jdbc.sql("SELECT app_id, user_id FROM action WHERE run_id = :runId AND status = 'pending'"
                            + due + " LIMIT 1")
                    .param("runId", runId).param("now", Db.ts(now))
                    .query((rs, row) -> new String[] {rs.getString("app_id"), rs.getString("user_id")})
                    .optional();
            if (owner.isEmpty()) {
                return 0;
            }
            jdbc.sql("SELECT 1 FROM app_user WHERE app_id = :appId AND user_id = :userId FOR UPDATE")
                    .param("appId", owner.get()[0]).param("userId", owner.get()[1])
                    .query(Integer.class).single();
            List<Action> changed = jdbc.sql("""
                    UPDATE action SET status = :status, note = :note, resolved_by = 'service', resolved_at = :now
                     WHERE run_id = :runId AND status = 'pending'""" + due + "\n RETURNING " + COLUMNS)
                    .param("status", status).param("note", note).param("now", Db.ts(now)).param("runId", runId)
                    .query(Actions::row)
                    .list();
            changed.forEach(action -> outbound.record(action.appId(), action.userId(), Update.ACTION_RESOLVED, action.view()));
            return changed.size();
        });
        return ended == null ? 0 : ended;
    }

    // ================================================================ for people

    public Optional<Action> find(String appId, String userId, String actionId) {
        return jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId AND action_id = :actionId")
                .param("appId", appId).param("userId", userId).param("actionId", actionId)
                .query(Actions::row)
                .optional();
    }

    /**
     * The user's actions that wait for a decision, oldest first.
     *
     * @param approver only the ones this approver decides; {@code null} for all
     * @param conversationId only this conversation's; {@code null} for all
     */
    public List<Action> pending(String appId, String userId, String approver, String conversationId, int limit) {
        return jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND status = 'pending' AND expires_at > :now
                   AND (CAST(:approver AS text) IS NULL OR approver = :approver)
                   AND (CAST(:conversationId AS text) IS NULL OR conversation_id = :conversationId)
                 ORDER BY created_at, action_id
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("now", Db.ts(clock.now()))
                .param("approver", approver).param("conversationId", conversationId).param("limit", limit)
                .query(Actions::row)
                .list();
    }

    /** The question in the conversation that the user would answer by writing to it: the latest one that waits. */
    public Optional<Action> latestPendingQuestion(String appId, String userId, String conversationId) {
        return jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND kind = 'question'
                   AND status = 'pending' AND expires_at > :now
                 ORDER BY created_at DESC, action_id DESC
                 LIMIT 1
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("now", Db.ts(clock.now()))
                .query(Actions::row)
                .optional();
    }

    /**
     * Moves a pending action to its end. Whoever gets there first decides.
     *
     * @return whether the action was still pending, and so whether this call decided it
     */
    public boolean settle(Action action, String status, String answer, String note, String resolvedBy) {
        boolean decided = jdbc.sql("""
                UPDATE action SET status = :status, answer = :answer, note = :note, resolved_by = :resolvedBy,
                       resolved_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND action_id = :actionId AND status = 'pending'
                """)
                .param("status", status).param("answer", answer).param("note", note).param("resolvedBy", resolvedBy)
                .param("now", Db.ts(clock.now())).param("appId", action.appId()).param("userId", action.userId())
                .param("actionId", action.actionId())
                .update() == 1;
        if (decided) {
            outbound.record(action.appId(), action.userId(), Update.ACTION_RESOLVED,
                    find(action.appId(), action.userId(), action.actionId()).orElseThrow().view());
        }
        return decided;
    }

    // ================================================================ waking a turn

    /** Wakes the run whose turn waits: an event that carries its resume key. In the caller's transaction. */
    public void ring(String appId, String userId, String mergeKey, String runId) {
        boolean chat = mergeKey.startsWith(Threads.CHAT_PREFIX);
        ObjectNode payload = InboxJson.mapper().createObjectNode();
        payload.put("run_id", runId);
        relay.enqueue(appId, userId, WriteEventRequest.builder(chat ? EventSource.CONVERSATION : EventSource.BIZ_WEBHOOK, DOORBELL)
                .sourceEventId("ring:" + ids.next("ring"))
                .mergeKey(mergeKey)
                .lane(chat ? Lane.INTERACTIVE : Lane.BACKGROUND)
                .resumeKey(resumeKey(runId))
                .interrupt(false)
                .occurredAt(clock.now())
                .payload(payload)
                .build());
    }

    /**
     * New input arrived on a line: whatever the line waits for people to decide no longer applies.
     * The pending actions are withdrawn, the turns that wait for them are told to finish without
     * waiting again, and are woken. In the caller's transaction, under the user's lock.
     *
     * @return the runs that were waiting
     */
    public List<String> supersede(String appId, String userId, String mergeKey, String note) {
        List<String> runs = jdbc.sql("""
                SELECT DISTINCT run_id FROM action
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND status = 'pending'
                   AND NOT in_passing
                """)
                .param("appId", appId).param("userId", userId).param("mergeKey", mergeKey)
                .query(String.class)
                .list();
        List<String> woken = new ArrayList<>();
        for (String runId : runs) {
            withdrawOfRun(runId, note);
            close(runId, note);
            ring(appId, userId, mergeKey, runId);
            woken.add(runId);
        }
        return woken;
    }

    /**
     * Withdraws the questions that were asked in passing on a line and still wait, because the agent
     * has asked a newer one: one question at a time is put before the user. In the caller's
     * transaction, under the user's lock.
     */
    public int withdrawInPassing(String appId, String userId, String mergeKey, String note) {
        List<Action> changed = jdbc.sql("""
                UPDATE action SET status = 'withdrawn', note = :note, resolved_by = 'service', resolved_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND status = 'pending' AND in_passing
                """ + " RETURNING " + COLUMNS)
                .param("note", note).param("now", Db.ts(clock.now())).param("appId", appId).param("userId", userId)
                .param("mergeKey", mergeKey)
                .query(Actions::row)
                .list();
        changed.forEach(action -> outbound.record(action.appId(), action.userId(), Update.ACTION_RESOLVED, action.view()));
        return changed.size();
    }

    /** The questions asked in passing whose time is up, oldest first. Nothing wakes for them: the sweep ends them. */
    public List<Action> inPassingDue(int limit) {
        return jdbc.sql(SELECT + " WHERE status = 'pending' AND in_passing AND expires_at <= :now ORDER BY expires_at, action_id LIMIT :limit")
                .param("now", Db.ts(clock.now())).param("limit", limit)
                .query(Actions::row)
                .list();
    }

    /** How many questions the user was asked since {@code since} in turns that they did not begin. */
    public long unaskedSince(String appId, String userId, Instant since) {
        return jdbc.sql("SELECT count(*) FROM action WHERE app_id = :appId AND user_id = :userId AND unasked AND created_at > :since")
                .param("appId", appId).param("userId", userId).param("since", Db.ts(since))
                .query(Long.class).single();
    }

    /** Tells the run's turn that new input arrived on its line: it finishes without waiting for anybody again. */
    public void close(String runId, String note) {
        jdbc.sql("UPDATE turn SET closing = :note, updated_at = :now WHERE run_id = :runId AND state = 'open' AND closing IS NULL")
                .param("note", note).param("now", Db.ts(clock.now())).param("runId", runId)
                .update();
    }

    /** Why the input that superseded the line's waiting is in the agent's next turn, in words for the agent and the user. */
    public static String supersededNote(String mergeKey) {
        return mergeKey.startsWith(Threads.CHAT_PREFIX)
                ? "the user wrote a new message before this was decided; it comes in the next turn"
                : "something new happened on this subject before this was decided; it comes in the next turn";
    }

    // ================================================================ rows

    private static final String COLUMNS = """
            app_id, user_id, action_id, kind, status, approver, merge_key, run_id, conversation_id, cma_session_id,
                   tool_use_id, tool_server, tool_name, tool_input::text AS tool_input, question, options::text AS options,
                   answer, note, resolved_by, created_at, expires_at, resolved_at, in_passing
            """;

    private static final String SELECT = "SELECT " + COLUMNS + " FROM action\n";

    private static Action row(ResultSet rs, int rowNumber) throws SQLException {
        String input = rs.getString("tool_input");
        String options = rs.getString("options");
        List<String> optionList = null;
        if (options != null) {
            optionList = new ArrayList<>();
            for (JsonNode option : Json.STORED.readTree(options)) {
                optionList.add(option.asString());
            }
        }
        return new Action(rs.getString("app_id"), rs.getString("user_id"), rs.getString("action_id"), rs.getString("kind"),
                rs.getString("status"), rs.getString("approver"), rs.getString("merge_key"), rs.getString("run_id"),
                rs.getString("conversation_id"), rs.getString("cma_session_id"), rs.getString("tool_use_id"),
                rs.getString("tool_server"), rs.getString("tool_name"), input == null ? null : Json.STORED.readTree(input),
                rs.getString("question"), optionList, rs.getString("answer"), rs.getString("note"),
                rs.getString("resolved_by"), Db.instant(rs, "created_at"), Db.instant(rs, "expires_at"),
                Db.instant(rs, "resolved_at"), rs.getBoolean("in_passing"));
    }
}
