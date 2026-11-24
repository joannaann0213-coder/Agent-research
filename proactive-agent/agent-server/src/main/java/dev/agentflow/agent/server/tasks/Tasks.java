package dev.agentflow.agent.server.tasks;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.Proactivity;
import dev.agentflow.agent.api.TaskNoteView;
import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.TaskView;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.server.actions.Actions;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Preferences;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.delivery.Outbound;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.relay.InboxRelay;
import dev.agentflow.agent.server.schedules.Schedules;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The work a main agent hands to workers. A task is a line of work of its own: the worker's agent
 * runs on it, in a session of its own on the agent platform, and the Event Inbox runs its turns one
 * at a time like those of any other line.
 *
 * <p>The main agent and the worker never talk to each other directly. What one has for the other is
 * stored here as a note of the task, and an event for the other's line is queued in the same
 * transaction: a note for the worker wakes the task's line, a note for the main agent wakes the main
 * agent's line. So a report is input like any other: it is deduplicated, it waits its turn, and it
 * survives a worker of this service that dies.
 *
 * <p>The application can hand work to a worker as well, by sending an event for another agent than
 * the main agent to a conversation that a main agent serves ({@link #assign}). That is a task of the
 * main agent like the ones it delegates itself: the main agent is told of it at once, through its
 * own line, and the worker reports to it. Nothing works for the user that the main agent does not
 * know of.
 *
 * <p>A task does not always start when it is delegated. What the main agent begins on its own
 * initiative waits for the user's confirmation, as a proposal, unless the application lets the role
 * start unasked or the user has given a standing authorisation for it ({@link Grants}). A proposal
 * costs nothing on the agent platform: its worker is woken, and gets a session, only when it starts.
 *
 * <p>The methods that change a task expect the caller's transaction to hold the lock of the user's
 * row, as everything does that records updates for a user; the ones that take it themselves say so.
 */
@Service
public class Tasks {

    private static final Logger log = LoggerFactory.getLogger(Tasks.class);

    /** The type of the Inbox event that tells a worker that the main agent has something for it. Its payload names the note. */
    public static final String MESSAGE_EVENT = "agent.task_message.v1";

    /** The type of the Inbox event that tells a main agent that a task has something for it. Its payload names the note. */
    public static final String REPORT_EVENT = "agent.task_report.v1";

    public static final String PROPOSED = "proposed";
    public static final String QUEUED = "queued";
    public static final String WORKING = "working";
    public static final String IDLE = "idle";
    public static final String CLOSED = "closed";

    public static final String TO_WORKER = "to_worker";
    public static final String TO_MAIN = "to_main";

    public static final String ASSIGNMENT = "assignment";
    public static final String MESSAGE = "message";
    /** An event that the application handed to the worker. */
    public static final String EVENT = "event";
    /** Something that the application scheduled for the worker, and that is due. */
    public static final String SCHEDULE = "schedule";
    /** What tells the main agent that the application handed its worker work itself: an event, or what it scheduled. */
    public static final String ASSIGNED = "assigned";
    /** What tells the main agent that a worker waits for a person's decision before it can go on. */
    public static final String WAITING = "waiting";
    /** What tells the main agent that a person decided: what a worker waited for, or to start a task that waited for a confirmation. */
    public static final String DECIDED = "decided";
    public static final String REPORT = "report";
    public static final String FAILURE = "failure";
    public static final String CLOSED_NOTE = "closed";

    public static final String BY_AGENT = "agent";
    public static final String BY_USER = "user";
    public static final String BY_APP = "app";
    public static final String BY_SERVICE = "service";

    /** Why a task may start: see the column {@code authorized_by}. */
    public static final String BY_CONVERSATION = "conversation";
    public static final String BY_GRANT = "grant";
    public static final String BY_POLICY = "policy";

    public static final int MAX_TITLE = 200;
    public static final int MAX_REASON = 500;
    public static final int MAX_SUMMARY = 500;
    public static final int DEFAULT_MAX_ACTIVE = 4;

    /** How much of a report a list shows. */
    private static final int EXCERPT = 1000;

    /** How much of a call's input the main agent is shown when a worker waits for its approval. */
    private static final int INPUT_EXCERPT = 500;

    /** How many notes one turn of a main agent is given at most. The rest come with its next turn. */
    private static final int MAX_NEWS = 50;

    private static final String NOT_CONFIRMED_IN_TIME = "it was not confirmed in time";

    /**
     * @param summary what the user is shown about the task, in the main agent's words; {@code null} when it gave none
     * @param authorizedBy why the task was allowed to start; {@code null} while it waits for a confirmation
     * @param expiresAt until when a task that waits for a confirmation can get it
     */
    public record Task(String appId, String userId, String taskId, String hubKey, String conversationId, String role,
            String agentKey, String title, String summary, String status, String authorizedBy, Instant expiresAt,
            String closedBy, String closedReason, Instant createdAt, Instant updatedAt, Instant closedAt, String createdBy,
            String subject) {

        /** Whether the application handed the work to the worker itself, rather than the main agent. */
        public boolean ofApplication() {
            return BY_APP.equals(createdBy);
        }

        public boolean closed() {
            return CLOSED.equals(status);
        }

        /** Whether the task waits for the user's confirmation: it has not started, and its worker knows nothing of it. */
        public boolean proposed() {
            return PROPOSED.equals(status);
        }

        /** The line the worker runs on. */
        public String line() {
            return Threads.workKey(taskId);
        }
    }

    /** @param readAt when a turn of the side the note is for ended having been given it; {@code null} until then */
    public record Note(String taskId, int seq, String direction, String kind, String text, String runId, Instant createdAt,
            Instant readAt) {

        /** Who it is from, for people and for agents: the main agent, the worker, or this service. */
        public String from() {
            if (TO_WORKER.equals(direction)) {
                return EVENT.equals(kind) || SCHEDULE.equals(kind) ? "app" : "main";
            }
            return REPORT.equals(kind) ? "worker" : ASSIGNED.equals(kind) ? "app" : "service";
        }

        public TaskNoteView view() {
            return new TaskNoteView(seq, from(), kind, text, createdAt);
        }
    }

    /** A note for the main agent, with the task it is about. */
    public record Report(Task task, Note note) {
    }

    /**
     * What delegating came to.
     *
     * @param task the task; {@code null} when there is none
     * @param created whether this call made it, rather than found the one an earlier delivery of the run made
     * @param refused why there is no task, in words for the agent; {@code null} when there is one
     * @param held why the task waits for the user although its role may start unasked, in words for the
     *     agent; {@code null} when that is not so
     */
    public record Delegated(Task task, boolean created, String refused, String held) {

        Delegated(Task task, boolean created, String refused) {
            this(task, created, refused, null);
        }
    }

    /** What starting a task came to. */
    public enum Start {
        /** It waited for a confirmation and has started now. */
        STARTED,
        /** It had started before. */
        RUNNING,
        /** It is closed. */
        CLOSED,
        /** It waited too long for a confirmation, and was closed for that just now. */
        EXPIRED
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Users users;
    private final Threads threads;
    private final Messages messages;
    private final Actions actions;
    private final Schedules schedules;
    private final Grants grants;
    private final AgentProfiles profiles;
    private final Preferences preferences;
    private final InboxRelay relay;
    private final Outbound outbound;
    private final AgentClock clock;
    private final Ids ids;
    private final AgentProperties.Tasks config;
    private final Lane lane;

    public Tasks(JdbcClient jdbc, TransactionTemplate tx, Users users, Threads threads, Messages messages, Actions actions,
            Schedules schedules, Grants grants, AgentProfiles profiles, Preferences preferences, InboxRelay relay,
            Outbound outbound, AgentClock clock, Ids ids, AgentProperties properties) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.threads = threads;
        this.messages = messages;
        this.actions = actions;
        this.schedules = schedules;
        this.grants = grants;
        this.profiles = profiles;
        this.preferences = preferences;
        this.relay = relay;
        this.outbound = outbound;
        this.clock = clock;
        this.ids = ids;
        this.config = properties.getTasks();
        this.lane = laneOf(properties);
    }

    /** The Inbox lane on which workers run. */
    public static Lane laneOf(AgentProperties properties) {
        Lane configured = Lane.fromWire(properties.getTasks().getLane());
        if (configured == Lane.INTERACTIVE) {
            throw new IllegalArgumentException("agent.tasks.lane is batch or background: a worker never runs on the lane "
                    + "of the conversations");
        }
        return configured;
    }

    // ================================================================ for the main agent's turn

    /**
     * Hands a piece of work to a worker: the task, its line, the note that says what to do, and, if
     * the task may start, the event that wakes the worker. A task that may not start yet waits for
     * the user's confirmation, and its worker is woken by {@link #startIn}. The call is named by the
     * run and its place among the run's calls, so a run that is delivered again gets the task it made
     * the first time.
     *
     * @param hub the main agent's line
     * @param summary what the user is shown about the task; needed when it has to be confirmed
     * @param userTurn whether the user began the turn in which the main agent delegates
     * @param askFirst whether the main agent wants the user to confirm the task whatever the rules say
     * @param maxActive how many tasks of the line may be queued or worked on at a time
     */
    public Delegated delegate(Threads.Thread hub, String runId, int call, WorkerConfig worker, String title, String instructions,
            String summary, boolean userTurn, boolean askFirst, int maxActive) {
        Optional<Task> earlier = jdbc.sql(SELECT + " WHERE created_by_run = :runId AND created_by_call = :call")
                .param("runId", runId).param("call", call)
                .query(Tasks::task)
                .optional();
        if (earlier.isPresent()) {
            return new Delegated(earlier.get(), false, null);
        }
        closeStale(hub);
        String authorizedBy = askFirst ? null : authorization(hub, worker, userTurn);
        String held = null;
        if (BY_GRANT.equals(authorizedBy) || BY_POLICY.equals(authorizedBy)) {
            // Nobody is asked about such a task, so there is a limit to how many of them a day brings.
            long unasked = jdbc.sql("""
                    SELECT count(*) FROM delegated_task
                     WHERE app_id = :appId AND user_id = :userId AND authorized_by IN ('grant', 'policy') AND authorized_at > :since
                    """)
                    .param("appId", hub.appId()).param("userId", hub.userId())
                    .param("since", Db.ts(clock.now().minus(java.time.Duration.ofHours(24))))
                    .query(Long.class).single();
            if (unasked >= config.getMaxUnaskedPerDay()) {
                authorizedBy = null;
                held = "You have started " + unasked + " tasks without asking the user in the last 24 hours, which is the "
                        + "limit, so this one waits for their confirmation.";
            }
        }
        if (authorizedBy == null) {
            if (!userTurn && Proactivity.QUIET.equals(preferences.proactivity(hub.appId(), hub.userId(), hub.agentKey()))) {
                // An offer is something the user has to look at and decide. This user asked not to be offered anything.
                return new Delegated(null, false, "Not delegated, and not offered to the user: they have asked to hear from "
                        + "you only what is key, and to be offered nothing that they did not ask for. Leave it. If it matters "
                        + "to them, they will ask.");
            }
            if (summary == null) {
                return new Delegated(null, false, "Not delegated: the user has to confirm this task before it starts, and is "
                        + "shown your summary of it to decide. Call again with `summary`: what you want to do and why, in a "
                        + "sentence or two written for the user.");
            }
            long waiting = count(hub, " AND status = 'proposed'");
            if (waiting >= config.getMaxProposed()) {
                return new Delegated(null, false, "Not delegated: " + waiting + " tasks already wait for the user's confirmation, "
                        + "which is the limit. Wait for the user's answers, or withdraw one with close_task.");
            }
        } else {
            long active = count(hub, " AND status IN ('queued', 'working')");
            if (active >= maxActive) {
                return new Delegated(null, false, "Not delegated: " + active + " tasks are queued or being worked on, which is the "
                        + "limit. Wait for a report, or close a task you no longer need with close_task.");
            }
        }
        long open = count(hub, " AND status <> 'closed'");
        if (open >= config.getMaxOpen()) {
            return new Delegated(null, false, "Not delegated: " + open + " tasks are open, which is the limit. Close the ones "
                    + "that are finished with close_task; list_tasks shows them.");
        }
        Instant now = clock.now();
        String taskId = ids.next("task");
        boolean starts = authorizedBy != null;
        jdbc.sql("""
                INSERT INTO delegated_task (app_id, user_id, task_id, hub_key, conversation_id, role, agent_key, title, summary,
                                            status, authorized_by, authorized_at, expires_at, created_by_run, created_by_call,
                                            created_at, updated_at)
                VALUES (:appId, :userId, :taskId, :hubKey, :conversationId, :role, :agentKey, :title, :summary, :status,
                        :authorizedBy, :authorizedAt, :expiresAt, :runId, :call, :now, :now)
                """)
                .param("appId", hub.appId()).param("userId", hub.userId()).param("taskId", taskId)
                .param("hubKey", hub.mergeKey()).param("conversationId", hub.conversationId()).param("role", worker.role())
                .param("agentKey", worker.agent()).param("title", title).param("summary", summary)
                .param("status", starts ? QUEUED : PROPOSED).param("authorizedBy", authorizedBy)
                .param("authorizedAt", starts ? Db.ts(now) : null)
                .param("expiresAt", starts ? null : Db.ts(now.plus(config.getProposalTtl())))
                .param("runId", runId).param("call", call).param("now", Db.ts(now))
                .update();
        threads.ensureWork(hub.appId(), hub.userId(), taskId, hub.conversationId(), worker.agent());
        Task task = find(hub.appId(), hub.userId(), taskId).orElseThrow();
        Written assignment = write(task, TO_WORKER, ASSIGNMENT, instructions, "assign", runId);
        if (starts) {
            wakeWorker(task, assignment.note());
        }
        outbound.record(task.appId(), task.userId(), Update.TASK_CREATED, view(task, false, false));
        if (!starts) {
            // The user is asked where they are: in the conversation, whatever else the application shows.
            messages.storeProposal(task.appId(), task.userId(), task.conversationId(), task.hubKey(), taskId, summary, runId);
        }
        return new Delegated(task, true, null, held);
    }

    /**
     * Why a task for this worker may start now, or {@code null} when the user has to confirm it first.
     * In a turn that the user began the main agent acts at the user's word. What it begins on its own
     * initiative needs the application's leave for the role, or the user's standing authorisation.
     */
    private String authorization(Threads.Thread hub, WorkerConfig worker, boolean userTurn) {
        String rule = worker.startRule();
        if (WorkerConfig.ALWAYS_ASK.equals(rule)) {
            return null;
        }
        if (userTurn) {
            return BY_CONVERSATION;
        }
        if (WorkerConfig.ALWAYS_ALLOW.equals(rule)) {
            return BY_POLICY;
        }
        return grants.granted(hub.appId(), hub.userId(), hub.agentKey(), worker.role()) ? BY_GRANT : null;
    }

    /**
     * Starts a task that waits for a confirmation, in the caller's transaction: its worker is woken
     * with what the main agent wrote for it when it proposed the task. The main agent is not woken:
     * the worker's report will be.
     *
     * @param by who confirmed it: {@code user}, {@code app}, or {@code conversation} when the user
     *     told the main agent to go ahead
     */
    public Start startIn(Task task, String by) {
        if (task.closed()) {
            return Start.CLOSED;
        }
        if (!task.proposed()) {
            return Start.RUNNING;
        }
        Instant now = clock.now();
        if (!now.isBefore(task.expiresAt())) {
            closeIn(task, BY_SERVICE, NOT_CONFIRMED_IN_TIME);
            return Start.EXPIRED;
        }
        boolean started = jdbc.sql("""
                UPDATE delegated_task SET status = 'queued', authorized_by = :by, authorized_at = :now, updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND status = 'proposed'
                """)
                .param("by", by).param("now", Db.ts(now)).param("appId", task.appId()).param("userId", task.userId())
                .param("taskId", task.taskId())
                .update() == 1;
        if (!started) {
            return Start.RUNNING;
        }
        for (Note note : notes(task)) {
            if (TO_WORKER.equals(note.direction()) && note.readAt() == null) {
                wakeWorker(task, note);
            }
        }
        if (!BY_CONVERSATION.equals(by)) {
            // The main agent did not see this happen. Its next turn is told; the worker's report wakes it.
            write(task, TO_MAIN, DECIDED, (BY_USER.equals(by) ? "The user" : "The application") + " confirmed the task \""
                    + task.title() + "\", which you proposed, outside this conversation. Its worker " + task.role()
                    + " has started.", "confirmed", null);
        }
        outbound.record(task.appId(), task.userId(), Update.TASK_UPDATED, view(find(task).orElseThrow(), false, false));
        return Start.STARTED;
    }

    // ================================================================ for the application

    /**
     * Hands an event of the application to a worker, as a task of the main agent that serves the
     * conversation. The first event of a subject makes the task; later ones go to the same task while
     * it is open, and so to the session of the worker that remembers the earlier ones. The worker is
     * woken with the event, and the main agent is told of it at once, through its own line like
     * everything else that concerns the user. In the caller's transaction, under the user's lock.
     *
     * @param hub the main agent's line
     * @param worker the agent that handles the subject, as {@link dev.agentflow.agent.server.chat.Lines} found
     * @param data what the event says, as the application sent it
     * @param readyAt when the event is to be handled at the earliest; {@code null} for at once
     */
    public Task assign(Threads.Thread hub, String worker, String eventId, String type, String subject, JsonNode data,
            Instant readyAt) {
        Instant now = clock.now();
        Task task = openOf(hub, subject).orElseGet(() -> begin(hub, worker, subject.equals(type) ? type : type + ": " + subject,
                subject, now));
        ObjectNode event = Json.STORED.createObjectNode();
        event.put("event_id", eventId).put("type", type).put("subject", subject).put("received_at", now.toString());
        event.set("data", data);
        Written handed = write(task, TO_WORKER, EVENT, event.toString(), "evt:" + eventId, null);
        if (!handed.created()) {
            return task;
        }
        wakeWorker(task, handed.note(), readyAt);
        moveTo(task, QUEUED, " AND status = 'idle'");
        // The main agent hears of it now, not when the worker reports: nothing works for the user that it does not know of.
        Written told = write(task, TO_MAIN, ASSIGNED, "The application handed your worker " + task.role() + " an event itself: "
                + type + ", on the subject " + subject + ". The worker has it and reports to you when it is done, so there is "
                + "nothing you have to do. If what you know from the user changes what the worker should do, tell it with "
                + "message_worker, or close the task. What the event says: " + Json.STORED.writeValueAsString(data),
                "assigned:" + eventId, null, readyAt);
        if (wakesMain(task, ASSIGNED, null)) {
            wakeMain(task, told.note(), readyAt);
        }
        return find(task).orElseThrow();
    }

    /**
     * Hands something that the application scheduled for a worker, and that is due, to that worker,
     * as a task of the main agent on whose line it fell due. The task is the one of the schedule's
     * subject, made if none is open. The worker is woken with the application's instructions, and
     * the main agent is told at once. A firing is handed over once, however often this is called for
     * it. In the caller's transaction, under the user's lock.
     *
     * @param hub the main agent's line
     * @param late whether it comes late, because the service was not running when it was due
     * @return whether this call handed it over
     */
    public boolean assignDue(Threads.Thread hub, Schedules.Schedule schedule, Instant dueAt, boolean late) {
        String firing = schedule.scheduleId() + ":" + dueAt;
        boolean handedBefore = jdbc.sql("SELECT count(*) FROM task_note WHERE app_id = :appId AND user_id = :userId AND note_key = :noteKey")
                .param("appId", hub.appId()).param("userId", hub.userId()).param("noteKey", "due:" + firing)
                .query(Long.class).single() > 0;
        if (handedBefore) {
            return false;
        }
        Instant now = clock.now();
        Task task = openOf(hub, schedule.subject())
                .orElseGet(() -> begin(hub, schedule.agentKey(), schedule.name(), schedule.subject(), now));
        ObjectNode due = Json.STORED.createObjectNode();
        due.put("schedule_id", schedule.scheduleId()).put("name", schedule.name()).put("instructions", schedule.instructions())
                .put("due_at", dueAt.toString()).put("late", late);
        Written handed = write(task, TO_WORKER, SCHEDULE, due.toString(), "due:" + firing, null);
        wakeWorker(task, handed.note());
        moveTo(task, QUEUED, " AND status = 'idle'");
        Written told = write(task, TO_MAIN, ASSIGNED, "It is time for \"" + schedule.name() + "\", which the application "
                + "scheduled for your worker " + task.role() + (late ? "; it comes late, because the service was not running "
                + "when it was due" : "") + ". The worker has the application's instructions for it and reports to you when it "
                + "is done, so there is nothing you have to do. If what you know from the user changes what the worker should "
                + "do, tell it with message_worker, or close the task; that does not cancel the schedule, which is the "
                + "application's. The application's instructions for it: " + schedule.instructions(),
                "assigned-due:" + firing, null);
        if (wakesMain(task, ASSIGNED, null)) {
            wakeMain(task, told.note());
        }
        return true;
    }

    // ================================================================ how a task stands, for the main agent

    /**
     * Tells the main agent that a worker waits for a person's decision before it can go on. The main
     * agent stands by for the user, who may ask about it, or may have to be told. In the caller's
     * transaction, under the user's lock.
     */
    public void waits(Task task, Actions.Action action) {
        if (task.closed()) {
            return;
        }
        String input = action.toolInput() == null ? "{}" : Json.STORED.writeValueAsString(action.toolInput());
        boolean users = Actions.BY_USER.equals(action.approver());
        Written written = write(task, TO_MAIN, WAITING, "Your worker " + task.role() + " waits for "
                + (users ? "the user's" : "the application's") + " approval before it calls " + action.toolServer() + "."
                + action.toolName() + " on the task \"" + task.title() + "\". It goes on as soon as that is decided, and does "
                + "without the call if nobody decides by " + action.expiresAt() + ". "
                + (users ? "The user decides it in the application, where it is put before them; you cannot decide it for them."
                        : "The application decides it; the user has nothing to do.")
                + " What the call would be given: " + (input.length() <= INPUT_EXCERPT ? input : input.substring(0, INPUT_EXCERPT) + "…"),
                "waiting:" + action.actionId(), null);
        if (written.created() && wakesMain(task, WAITING, action)) {
            wakeMain(task, written.note());
        }
    }

    /**
     * Whether a note that only says how a task stands wakes the main agent, or waits for its next
     * turn. That is a matter of how forward the user wants their main agent: one that keeps the user
     * up to date is woken for everything, the others only for what may need the user. What a worker
     * reports, a failure and a task closed from outside wake it at every level.
     */
    private boolean wakesMain(Task task, String kind, Actions.Action action) {
        String hubAgent = threads.find(task.appId(), task.userId(), task.hubKey()).map(Threads.Thread::agentKey).orElse(null);
        String level = hubAgent == null ? Proactivity.PROACTIVE : preferences.proactivity(task.appId(), task.userId(), hubAgent);
        return switch (level) {
            case Proactivity.PROACTIVE -> true;
            // The user is the one who is waited for: that is theirs to know.
            case Proactivity.BALANCED -> WAITING.equals(kind) && action != null && Actions.BY_USER.equals(action.approver());
            default -> false;
        };
    }

    /**
     * Tells the main agent what became of something a worker waited for. Nothing wakes the main agent
     * for it: the worker goes on and reports, and the next turn of the main agent is given this too.
     * In the caller's transaction, under the user's lock.
     */
    public void decided(Task task, Actions.Action action) {
        if (task.closed() || action.pending()) {
            return;
        }
        String call = action.toolServer() + "." + action.toolName();
        String by = Actions.BY_USER.equals(action.resolvedBy()) ? "The user" : "The application";
        String text = switch (action.status()) {
            case Actions.APPROVED -> by + " approved " + call + ", which your worker " + task.role() + " waited for. The worker goes on.";
            case Actions.DENIED -> by + " denied " + call + ", which your worker " + task.role() + " waited for"
                    + (action.note() == null ? "" : ": " + action.note()) + ". The worker has been told and goes on without it.";
            case Actions.EXPIRED -> "Nobody decided in time on " + call + ", which your worker " + task.role()
                    + " waited for. The worker has been told and goes on without it.";
            default -> "What your worker " + task.role() + " waited for, " + call + ", is no longer asked"
                    + (action.note() == null ? "" : ": " + action.note()) + ". The worker goes on.";
        };
        write(task, TO_MAIN, DECIDED, text, "decided:" + action.actionId(), null);
    }

    /** The role under which a main agent knows an agent: the one on its list, or, for an agent that is not on it, the agent's key. */
    public String roleOf(Threads.Thread hub, String agentKey) {
        return profiles.configIfThere(hub.appId(), hub.agentKey()).map(AgentProfileRequest::coordinator)
                .filter(coordinator -> coordinator.workers() != null)
                .flatMap(coordinator -> coordinator.workers().stream().filter(listed -> listed.agent().equals(agentKey))
                        .map(WorkerConfig::role).findFirst())
                .orElse(agentKey);
    }

    /** The open task that the application's work on a subject goes to, under a main agent. */
    private Optional<Task> openOf(Threads.Thread hub, String subject) {
        return jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND hub_key = :hubKey AND subject = :subject AND status <> 'closed'
                """)
                .param("appId", hub.appId()).param("userId", hub.userId()).param("hubKey", hub.mergeKey()).param("subject", subject)
                .query(Tasks::task)
                .optional();
    }

    /** The task that an event of the application became part of, if it went to a worker. */
    public Optional<String> ofEvent(String appId, String userId, String eventId) {
        return jdbc.sql("SELECT task_id FROM task_note WHERE app_id = :appId AND user_id = :userId AND note_key = :noteKey LIMIT 1")
                .param("appId", appId).param("userId", userId).param("noteKey", "evt:" + eventId)
                .query(String.class)
                .optional();
    }

    /** Makes the task for a subject of the application's work that has none open. */
    private Task begin(Threads.Thread hub, String worker, String title, String subject, Instant now) {
        AgentProfileRequest profile = profiles.configIfThere(hub.appId(), worker).orElseThrow(() -> AgentException.invalid(
                "this application has no agent " + worker + "; create it with PUT /v1/agents/" + worker));
        if (profile.coordinator() != null) {
            throw AgentException.conflict("conversation " + hub.conversationId() + " is served by agent " + hub.agentKey()
                    + "; what is for the main agent " + worker + " goes to a conversation that it serves");
        }
        closeStale(hub);
        long open = count(hub, " AND status <> 'closed'");
        if (open >= config.getMaxOpen()) {
            // The application's own task that has rested longest makes room. Its subject starts anew with its next event.
            Optional<Task> resting = jdbc.sql(SELECT + """
                     WHERE app_id = :appId AND user_id = :userId AND hub_key = :hubKey AND status = 'idle' AND created_by = 'app'
                     ORDER BY updated_at, task_id LIMIT 1
                    """)
                    .param("appId", hub.appId()).param("userId", hub.userId()).param("hubKey", hub.mergeKey())
                    .query(Tasks::task)
                    .optional();
            if (resting.isEmpty()) {
                throw AgentException.conflict(open + " tasks are open in conversation " + hub.conversationId()
                        + ", which is the limit: close the ones that are finished");
            }
            closeIn(resting.get(), BY_SERVICE, "it made room for a newer task of the application");
        }
        String taskId = ids.next("task");
        jdbc.sql("""
                INSERT INTO delegated_task (app_id, user_id, task_id, hub_key, conversation_id, role, agent_key, title, status,
                                            authorized_by, authorized_at, created_by_run, created_by_call, created_at, updated_at,
                                            created_by, subject)
                VALUES (:appId, :userId, :taskId, :hubKey, :conversationId, :role, :agentKey, :title, 'queued', 'app', :now,
                        :madeBy, 0, :now, :now, 'app', :subject)
                """)
                .param("appId", hub.appId()).param("userId", hub.userId()).param("taskId", taskId)
                .param("hubKey", hub.mergeKey()).param("conversationId", hub.conversationId()).param("role", roleOf(hub, worker))
                .param("agentKey", worker).param("title", title.length() <= MAX_TITLE ? title : title.substring(0, MAX_TITLE))
                .param("now", Db.ts(now)).param("madeBy", "app:" + taskId).param("subject", subject)
                .update();
        threads.ensureWork(hub.appId(), hub.userId(), taskId, hub.conversationId(), worker);
        Task task = find(hub.appId(), hub.userId(), taskId).orElseThrow();
        outbound.record(task.appId(), task.userId(), Update.TASK_CREATED, view(task, false, false));
        return task;
    }

    /**
     * Sends the worker of an open task a later word of the main agent. A worker that is in the middle
     * of a turn gets it in its next one.
     *
     * @return whether it was sent by this call, rather than by an earlier delivery of the run
     */
    public boolean message(Task task, String runId, int call, String text) {
        Written written = write(task, TO_WORKER, MESSAGE, text, "msg:" + runId + ":" + call, runId);
        if (!written.created()) {
            return false;
        }
        wakeWorker(task, written.note());
        moveTo(task, QUEUED, " AND status = 'idle'");
        return true;
    }

    /** The main agent's open tasks, oldest first. */
    public List<Task> open(Threads.Thread hub) {
        return jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND hub_key = :hubKey AND status <> 'closed'
                 ORDER BY created_at, task_id
                """)
                .param("appId", hub.appId()).param("userId", hub.userId()).param("hubKey", hub.mergeKey())
                .query(Tasks::task)
                .list();
    }

    /**
     * What the tasks of a main agent's line have for it and no turn of it has been given yet, oldest
     * first: what workers report, and what tells it how its tasks stand. Every turn of the main agent
     * is given all of it, whatever woke the turn, so that it never works, or answers the user, on a
     * picture that is behind. A note about something that is held back is left out until its time.
     */
    public List<Report> news(String appId, String userId, String hubKey) {
        return jdbc.sql("""
                SELECT n.task_id, n.seq
                  FROM task_note n JOIN delegated_task t USING (app_id, user_id, task_id)
                 WHERE t.app_id = :appId AND t.user_id = :userId AND t.hub_key = :hubKey
                   AND n.direction = 'to_main' AND n.read_at IS NULL AND (n.ready_at IS NULL OR n.ready_at <= :now)
                 ORDER BY n.created_at, n.task_id, n.seq
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("hubKey", hubKey).param("now", Db.ts(clock.now()))
                .param("limit", MAX_NEWS)
                .query((rs, rowNumber) -> Map.entry(rs.getString("task_id"), rs.getInt("seq")))
                .list().stream()
                .map(named -> find(appId, userId, named.getKey()).flatMap(task -> note(appId, userId, task.taskId(), named.getValue())
                        .map(note -> new Report(task, note))))
                .flatMap(Optional::stream)
                .toList();
    }

    /** Notes that a turn of the main agent ended having been given these reports. */
    public void read(List<Report> reports) {
        for (Report report : reports) {
            markRead(report.task(), List.of(report.note().seq()));
        }
    }

    // ================================================================ for the worker's turn

    /** The task whose line is {@code mergeKey}. */
    public Optional<Task> ofLine(String appId, String userId, String mergeKey) {
        if (mergeKey == null || !mergeKey.startsWith(Threads.WORK_PREFIX)) {
            return Optional.empty();
        }
        return find(appId, userId, mergeKey.substring(Threads.WORK_PREFIX.length()));
    }

    /** Whether the task is over, as it stands now. A task that is not there counts as over. */
    public boolean closed(String appId, String userId, String taskId) {
        return jdbc.sql("SELECT status FROM delegated_task WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId")
                .param("appId", appId).param("userId", userId).param("taskId", taskId)
                .query(String.class).optional().map(CLOSED::equals).orElse(true);
    }

    /** Notes that a turn of the worker has begun. Takes the user's lock itself. */
    public void working(Task task) {
        tx.executeWithoutResult(status -> {
            users.lock(task.appId(), task.userId());
            moveTo(task, WORKING, " AND status IN ('queued', 'idle')");
        });
    }

    /**
     * Stores what the worker says to the main agent at the end of a turn, and wakes the main agent
     * with it. Nothing is stored for a task that was closed meanwhile: nobody waits for its report.
     *
     * @param given the notes for the worker that this turn was given
     * @param report the worker's words; {@code null} when it has nothing to say, which only ends the turn
     */
    public void turnEnded(Task task, String runId, List<Integer> given, String report) {
        markRead(task, given);
        if (closed(task.appId(), task.userId(), task.taskId())) {
            return;
        }
        if (report != null) {
            String text = report.length() <= config.getMaxReport() ? report
                    : report.substring(0, config.getMaxReport()) + "… [the report was cut off here]";
            Written written = write(task, TO_MAIN, REPORT, text, "report:" + runId, runId);
            if (written.created()) {
                wakeMain(task, written.note());
            }
        }
        rest(task);
    }

    /**
     * Tells the main agent that a turn of the worker failed, so that it does not wait for a report
     * that will not come. Does nothing for a line that is not a task's, and for a task that is closed.
     */
    public void turnFailed(String appId, String userId, String mergeKey, String runId, String error) {
        Optional<Task> found = ofLine(appId, userId, mergeKey);
        if (found.isEmpty() || found.get().closed()) {
            return;
        }
        Task task = found.get();
        Written written = write(task, TO_MAIN, FAILURE, "The worker's turn failed, and what it was doing is not done: " + error
                + ". Send the worker a message to have it try again, or close the task.", "failure:" + runId, runId);
        if (!written.created()) {
            return;
        }
        // What the failed turn had been given is gone with it.
        jdbc.sql("""
                UPDATE task_note SET read_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND direction = 'to_worker'
                   AND read_at IS NULL AND seq < :seq
                """)
                .param("now", Db.ts(clock.now())).param("appId", appId).param("userId", userId)
                .param("taskId", task.taskId()).param("seq", written.note().seq())
                .update();
        wakeMain(task, written.note());
        rest(task);
    }

    /** The note {@code seq} of a task, if both are there. */
    public Optional<Note> note(String appId, String userId, String taskId, int seq) {
        return jdbc.sql(NOTES + " WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND seq = :seq")
                .param("appId", appId).param("userId", userId).param("taskId", taskId).param("seq", seq)
                .query(Tasks::note)
                .optional();
    }

    /** Everything the main agent and the worker told each other about the task, oldest first. */
    public List<Note> notes(Task task) {
        return jdbc.sql(NOTES + " WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId ORDER BY seq")
                .param("appId", task.appId()).param("userId", task.userId()).param("taskId", task.taskId())
                .query(Tasks::note)
                .list();
    }

    /** What the worker, or the service about the worker, last told the main agent. */
    public Optional<Note> lastForMain(Task task) {
        return jdbc.sql(NOTES + """
                 WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND direction = 'to_main'
                 ORDER BY seq DESC LIMIT 1
                """)
                .param("appId", task.appId()).param("userId", task.userId()).param("taskId", task.taskId())
                .query(Tasks::note)
                .optional();
    }

    // ================================================================ closing

    /**
     * Closes a task, in the caller's transaction: nothing more comes from it. What its worker waits
     * for people to decide is withdrawn, and a turn of the worker that is under way stops when it next
     * looks. When somebody other than the main agent closed it, the main agent is told.
     *
     * @param by {@code agent}, {@code user}, {@code app} or {@code service}
     * @return whether it was open
     */
    public boolean closeIn(Task task, String by, String reason) {
        Instant now = clock.now();
        boolean closed = jdbc.sql("""
                UPDATE delegated_task SET status = 'closed', closed_by = :by, closed_reason = :reason, closed_at = :now,
                       updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND status <> 'closed'
                """)
                .param("by", by).param("reason", reason).param("now", Db.ts(now)).param("appId", task.appId())
                .param("userId", task.userId()).param("taskId", task.taskId())
                .update() == 1;
        if (!closed) {
            return false;
        }
        actions.supersede(task.appId(), task.userId(), task.line(), "the task was closed");
        if (!BY_AGENT.equals(by)) {
            String who = BY_USER.equals(by) ? "The user" : BY_APP.equals(by) ? "The application" : "The service";
            // A task that waited for a confirmation and is closed by a person was declined: the main agent should know the difference.
            String did = task.proposed() && !BY_SERVICE.equals(by) ? " declined this task, which had not started" : " closed this task";
            Written written = write(task, TO_MAIN, CLOSED_NOTE, who + did + (reason == null ? "." : ": " + reason),
                    "closed", null);
            if (written.created() && !BY_SERVICE.equals(by)) {
                wakeMain(task, written.note());
            }
        }
        outbound.record(task.appId(), task.userId(), Update.TASK_UPDATED, view(find(task).orElseThrow(), false, false));
        return true;
    }

    /**
     * What is left to do once a task's closing is committed: the follow-ups its worker set for itself
     * are cancelled. If that fails they still fire, and find the task closed.
     */
    public void afterClose(Task task) {
        try {
            schedules.cancelLine(task.appId(), task.userId(), task.line());
        } catch (RuntimeException e) {
            log.warn("the follow-ups of task {} could not be cancelled: {}", task.taskId(), e.toString());
        }
    }

    /**
     * Closes the tasks that have waited too long for a confirmation, whoever's they are, so that
     * whoever shows them to the user learns that they can no longer be confirmed. One pass of the
     * sweep; takes each user's lock itself.
     *
     * @return how many it closed
     */
    public int closeUnconfirmed(int limit) {
        List<Task> due = jdbc.sql(SELECT + " WHERE status = 'proposed' AND expires_at <= :now ORDER BY expires_at, task_id LIMIT :limit")
                .param("now", Db.ts(clock.now())).param("limit", limit)
                .query(Tasks::task)
                .list();
        int closed = 0;
        for (Task candidate : due) {
            boolean done = Boolean.TRUE.equals(tx.execute(status -> {
                users.lock(candidate.appId(), candidate.userId());
                // It may have been confirmed, or closed, since it was found.
                return find(candidate).filter(Task::proposed).filter(task -> !clock.now().isBefore(task.expiresAt()))
                        .map(task -> closeIn(task, BY_SERVICE, NOT_CONFIRMED_IN_TIME)).orElse(false);
            }));
            if (done) {
                closed++;
            }
        }
        return closed;
    }

    /** Closes the tasks of a main agent's line that nobody has touched, or confirmed, for too long, to make room. */
    private void closeStale(Threads.Thread hub) {
        // A task whose worker has set itself a follow-up is waiting for something, however long ago it was touched.
        List<Task> stale = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND hub_key = :hubKey AND status = 'idle' AND updated_at < :before
                   AND NOT EXISTS (SELECT 1 FROM schedule s
                                    WHERE s.app_id = delegated_task.app_id AND s.user_id = delegated_task.user_id
                                      AND s.merge_key = 'work:' || delegated_task.task_id AND s.state = 'active')
                """)
                .param("appId", hub.appId()).param("userId", hub.userId()).param("hubKey", hub.mergeKey())
                .param("before", Db.ts(clock.now().minus(config.getIdleTtl())))
                .query(Tasks::task)
                .list();
        for (Task task : stale) {
            closeIn(task, BY_SERVICE, "nobody had touched it for " + config.getIdleTtl().toDays() + " days");
        }
        List<Task> unconfirmed = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND hub_key = :hubKey AND status = 'proposed' AND expires_at <= :now
                """)
                .param("appId", hub.appId()).param("userId", hub.userId()).param("hubKey", hub.mergeKey())
                .param("now", Db.ts(clock.now()))
                .query(Tasks::task)
                .list();
        for (Task task : unconfirmed) {
            closeIn(task, BY_SERVICE, NOT_CONFIRMED_IN_TIME);
        }
    }

    // ================================================================ for people

    /**
     * The user's tasks, newest first.
     *
     * @param conversationId only the tasks of this conversation's main agent; {@code null} for all
     * @param which {@code open} for the tasks that are not closed, {@code proposed} for those that
     *     wait for a confirmation, {@code all} for every task
     * @param forApp whether the reader is the application, which also gets the beginning of each task's latest report
     */
    public TaskPage list(String appId, String userId, String conversationId, String which, int limit, boolean forApp) {
        Names.userId(userId);
        if (conversationId != null) {
            Names.localId(conversationId, "conversation_id");
        }
        List<TaskView> tasks = jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId
                   AND (CAST(:conversationId AS text) IS NULL OR conversation_id = :conversationId)
                   AND (:which = 'all' OR (:which = 'open' AND status <> 'closed') OR (:which = 'proposed' AND status = 'proposed'))
                 ORDER BY created_at DESC, task_id DESC
                 LIMIT :limit
                """)
                .param("appId", appId).param("userId", userId).param("conversationId", conversationId)
                .param("which", which).param("limit", limit)
                .query(Tasks::task)
                .list().stream().map(task -> view(task, forApp, false)).toList();
        return new TaskPage(tasks);
    }

    /**
     * One task. For the application, with everything the main agent and the worker told each other;
     * for the user's own client without it, because the user hears of a worker only what the main
     * agent tells them.
     */
    public TaskView get(String appId, String userId, String taskId, boolean forApp) {
        Names.userId(userId);
        return view(find(appId, userId, taskId).orElseThrow(() -> AgentException.notFound("no task " + taskId + " for this user")),
                forApp, forApp);
    }

    /**
     * Confirms a task that waits for it, for the user or for the application, so that it starts.
     * Confirming a task that has started already changes nothing. Takes the user's lock itself.
     *
     * @param by {@code user} or {@code app}
     */
    public TaskView start(String appId, String userId, String taskId, String by) {
        Names.userId(userId);
        Start outcome = tx.execute(status -> {
            if (users.find(appId, userId).isEmpty()) {
                throw AgentException.notFound("no task " + taskId + " for this user");
            }
            users.lock(appId, userId);
            Task found = find(appId, userId, taskId)
                    .orElseThrow(() -> AgentException.notFound("no task " + taskId + " for this user"));
            return startIn(found, by);
        });
        // The refusals come after the transaction, which has closed a task that waited too long.
        if (outcome == Start.EXPIRED) {
            throw AgentException.conflict("task " + taskId + " waited too long for a confirmation and was closed");
        }
        if (outcome == Start.CLOSED) {
            throw AgentException.conflict("task " + taskId + " is closed");
        }
        return view(find(appId, userId, taskId).orElseThrow(() -> AgentException.notFound("no task " + taskId + " for this user")),
                BY_APP.equals(by), false);
    }

    /**
     * Closes a task for somebody other than the main agent, which is then told. Closing a task that
     * waits for a confirmation declines it. Closing a task that is closed already changes nothing.
     * Takes the user's lock itself.
     *
     * @param by {@code user} or {@code app}
     */
    public TaskView close(String appId, String userId, String taskId, String by, String reason) {
        Names.userId(userId);
        if (reason != null && (reason.isBlank() || reason.length() > MAX_REASON)) {
            throw AgentException.invalid("reason has at most " + MAX_REASON + " characters");
        }
        Task task = tx.execute(status -> {
            if (users.find(appId, userId).isEmpty()) {
                throw AgentException.notFound("no task " + taskId + " for this user");
            }
            users.lock(appId, userId);
            Task found = find(appId, userId, taskId)
                    .orElseThrow(() -> AgentException.notFound("no task " + taskId + " for this user"));
            return closeIn(found, by, reason) ? found : null;
        });
        if (task != null) {
            afterClose(task);
        }
        return view(find(appId, userId, taskId).orElseThrow(() -> AgentException.notFound("no task " + taskId + " for this user")),
                BY_APP.equals(by), false);
    }

    public Optional<Task> find(String appId, String userId, String taskId) {
        return jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId")
                .param("appId", appId).param("userId", userId).param("taskId", taskId)
                .query(Tasks::task)
                .optional();
    }

    /**
     * A task as people see it.
     *
     * @param withReport whether to show the beginning of the worker's latest report: for the application only, like the notes
     * @param withNotes whether to show everything the main agent and the worker told each other
     */
    public TaskView view(Task task, boolean withReport, boolean withNotes) {
        String report = !withReport ? null
                : lastForMain(task).filter(note -> REPORT.equals(note.kind())).map(note -> excerpt(note.text())).orElse(null);
        return new TaskView(task.taskId(), task.conversationId(), task.role(), task.agentKey(), task.title(), task.summary(),
                task.status(), task.authorizedBy(), task.proposed() ? task.expiresAt() : null, task.closedBy(), task.closedReason(),
                report, task.createdAt(), task.updatedAt(), task.closedAt(),
                withNotes ? notes(task).stream().map(Note::view).toList() : null, task.createdBy(), task.subject());
    }

    /** The beginning of a text, for lists. */
    public static String excerpt(String text) {
        return text.length() <= EXCERPT ? text : text.substring(0, EXCERPT) + "…";
    }

    // ================================================================ notes and states

    private record Written(Note note, boolean created) {
    }

    /** Writes a note of a task once: a second write under the same key finds the first. */
    private Written write(Task task, String direction, String kind, String text, String noteKey, String runId) {
        return write(task, direction, kind, text, noteKey, runId, null);
    }

    /** @param readyAt for a note about something that is held back: when it may be given at the earliest */
    private Written write(Task task, String direction, String kind, String text, String noteKey, String runId, Instant readyAt) {
        Optional<Note> earlier = jdbc.sql(NOTES + " WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND note_key = :noteKey")
                .param("appId", task.appId()).param("userId", task.userId()).param("taskId", task.taskId())
                .param("noteKey", noteKey)
                .query(Tasks::note)
                .optional();
        if (earlier.isPresent()) {
            return new Written(earlier.get(), false);
        }
        Instant now = clock.now();
        int seq = jdbc.sql("""
                UPDATE delegated_task SET next_note_seq = next_note_seq + 1, updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId
                RETURNING next_note_seq - 1
                """)
                .param("now", Db.ts(now)).param("appId", task.appId()).param("userId", task.userId())
                .param("taskId", task.taskId())
                .query(Integer.class).single();
        jdbc.sql("""
                INSERT INTO task_note (app_id, user_id, task_id, seq, direction, kind, text, note_key, run_id, created_at,
                                       ready_at)
                VALUES (:appId, :userId, :taskId, :seq, :direction, :kind, :text, :noteKey, :runId, :now, :readyAt)
                """)
                .param("appId", task.appId()).param("userId", task.userId()).param("taskId", task.taskId()).param("seq", seq)
                .param("direction", direction).param("kind", kind).param("text", text).param("noteKey", noteKey)
                .param("runId", runId).param("now", Db.ts(now)).param("readyAt", Db.ts(readyAt))
                .update();
        return new Written(new Note(task.taskId(), seq, direction, kind, text, runId, now, null), true);
    }

    private void markRead(Task task, List<Integer> seqs) {
        for (int seq : seqs) {
            jdbc.sql("""
                    UPDATE task_note SET read_at = :now
                     WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND seq = :seq AND read_at IS NULL
                    """)
                    .param("now", Db.ts(clock.now())).param("appId", task.appId()).param("userId", task.userId())
                    .param("taskId", task.taskId()).param("seq", seq)
                    .update();
        }
    }

    /** After a turn of the worker: the task waits, for the main agent or, when the main agent has sent more meanwhile, for the worker. */
    private void rest(Task task) {
        boolean more = jdbc.sql("""
                SELECT count(*) FROM task_note
                 WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId AND direction = 'to_worker' AND read_at IS NULL
                """)
                .param("appId", task.appId()).param("userId", task.userId()).param("taskId", task.taskId())
                .query(Long.class).single() > 0;
        moveTo(task, more ? QUEUED : IDLE, " AND status NOT IN ('closed', '" + (more ? QUEUED : IDLE) + "')");
    }

    /** Moves the task to {@code status} if {@code condition} holds for it, and records the update if it did. */
    private void moveTo(Task task, String status, String condition) {
        boolean moved = jdbc.sql("UPDATE delegated_task SET status = :status, updated_at = :now"
                        + " WHERE app_id = :appId AND user_id = :userId AND task_id = :taskId" + condition)
                .param("status", status).param("now", Db.ts(clock.now())).param("appId", task.appId())
                .param("userId", task.userId()).param("taskId", task.taskId())
                .update() == 1;
        if (moved) {
            outbound.record(task.appId(), task.userId(), Update.TASK_UPDATED, view(find(task).orElseThrow(), false, false));
        }
    }

    private Optional<Task> find(Task task) {
        return find(task.appId(), task.userId(), task.taskId());
    }

    private long count(Threads.Thread hub, String condition) {
        return jdbc.sql("SELECT count(*) FROM delegated_task WHERE app_id = :appId AND user_id = :userId AND hub_key = :hubKey"
                        + condition)
                .param("appId", hub.appId()).param("userId", hub.userId()).param("hubKey", hub.mergeKey())
                .query(Long.class).single();
    }

    // ================================================================ waking the other side

    private void wakeWorker(Task task, Note note) {
        wakeWorker(task, note, null);
    }

    /** @param readyAt when the worker is to be woken at the earliest; {@code null} for at once */
    private void wakeWorker(Task task, Note note, Instant readyAt) {
        relay.enqueue(task.appId(), task.userId(), WriteEventRequest.builder(EventSource.AGENT_SELF, MESSAGE_EVENT)
                .sourceEventId("task:" + task.taskId() + ":" + note.seq())
                .mergeKey(task.line())
                // A lane of its own by default: a worker's long turn must not hold up what wakes the main agent.
                .lane(lane)
                .interrupt(false)
                .occurredAt(note.createdAt())
                .readyAt(readyAt)
                .payload(named(task, note))
                .build());
    }

    private void wakeMain(Task task, Note note) {
        wakeMain(task, note, null);
    }

    /** @param readyAt when the main agent is to be woken at the earliest; {@code null} for at once */
    private void wakeMain(Task task, Note note, Instant readyAt) {
        relay.enqueue(task.appId(), task.userId(), WriteEventRequest.builder(EventSource.AGENT_SELF, REPORT_EVENT)
                .sourceEventId("task:" + task.taskId() + ":" + note.seq())
                .mergeKey(task.hubKey())
                // Not the lane of the user's messages: a report never makes the user wait for a reply.
                .lane(Lane.BACKGROUND)
                .interrupt(false)
                .occurredAt(note.createdAt())
                .readyAt(readyAt)
                .payload(named(task, note))
                .build());
    }

    private static ObjectNode named(Task task, Note note) {
        ObjectNode payload = InboxJson.mapper().createObjectNode();
        payload.put("task_id", task.taskId()).put("seq", note.seq());
        return payload;
    }

    // ================================================================ rows

    private static final String SELECT = """
            SELECT app_id, user_id, task_id, hub_key, conversation_id, role, agent_key, title, summary, status, authorized_by,
                   expires_at, closed_by, closed_reason, created_at, updated_at, closed_at, created_by, subject
              FROM delegated_task
            """;

    private static final String NOTES = """
            SELECT task_id, seq, direction, kind, text, run_id, created_at, read_at FROM task_note
            """;

    private static Task task(ResultSet rs, int rowNumber) throws SQLException {
        return new Task(rs.getString("app_id"), rs.getString("user_id"), rs.getString("task_id"), rs.getString("hub_key"),
                rs.getString("conversation_id"), rs.getString("role"), rs.getString("agent_key"), rs.getString("title"),
                rs.getString("summary"), rs.getString("status"), rs.getString("authorized_by"), Db.instant(rs, "expires_at"),
                rs.getString("closed_by"), rs.getString("closed_reason"), Db.instant(rs, "created_at"),
                Db.instant(rs, "updated_at"), Db.instant(rs, "closed_at"), rs.getString("created_by"), rs.getString("subject"));
    }

    private static Note note(ResultSet rs, int rowNumber) throws SQLException {
        return new Note(rs.getString("task_id"), rs.getInt("seq"), rs.getString("direction"), rs.getString("kind"),
                rs.getString("text"), rs.getString("run_id"), Db.instant(rs, "created_at"), Db.instant(rs, "read_at"));
    }
}
