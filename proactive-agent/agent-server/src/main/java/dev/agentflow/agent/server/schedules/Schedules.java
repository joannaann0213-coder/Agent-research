package dev.agentflow.agent.server.schedules;

import dev.agentflow.agent.api.CreateScheduleRequest;
import dev.agentflow.agent.api.ScheduleView;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.chat.Lines;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.relay.InboxRelay;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.CanonicalJson;
import dev.agentflow.inbox.api.CreateTimerRequest;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Hashing;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.TimerView;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.ObjectNode;

/**
 * What the agent is to do at a given time: a schedule the application set up, once or on a cron
 * rule in the user's time zone, or a follow-up the agent set itself. The Event Inbox keeps the time
 * as a timer; when it is due, the timer puts an event on the schedule's line, and the agent works on
 * it in the background. What to do stays here.
 *
 * <p>In a conversation that a main agent serves, the line is the main agent's, also for what the
 * application schedules for another agent: when that is due, the main agent's line hands it to the
 * other agent as a task, so that the main agent knows of it.
 */
@Service
public class Schedules {

    private static final Logger log = LoggerFactory.getLogger(Schedules.class);

    /** The type of the events that the Inbox's timers produce. */
    public static final String TIMER_EVENT = "timer.due";

    /**
     * The type of the Inbox event with which a schedule that fell due on a subject's line is passed
     * on to the main agent that serves its conversation by now. Its payload names the schedule.
     */
    public static final String FORWARDED_EVENT = "agent.schedule_due.v1";

    public static final String BY_APP = "app";
    public static final String BY_AGENT = "agent";

    public static final String ACTIVE = "active";
    public static final String DONE = "done";
    public static final String CANCELLED = "cancelled";

    public static final int MAX_NAME = 200;
    public static final int MAX_INSTRUCTIONS = 4000;
    public static final Duration MAX_AHEAD = Duration.ofDays(366);

    /**
     * @param agentKey the agent that does it. On a main agent's line that is the main agent itself, or
     *     another agent, to which the main agent's line hands it as a task each time it is due
     * @param subject for a schedule that a main agent's line hands to another agent: the subject its
     *     task is kept under; {@code null} for every other schedule
     */
    public record Schedule(String appId, String userId, String scheduleId, String createdBy, String kind, String name,
            String instructions, String note, int chain, String mergeKey, String agentKey, String conversationId,
            Instant fireAt, String cron, String timeZone, String fingerprint, String timerId, String state,
            Instant lastFiredAt, Instant createdAt, String subject) {

        public boolean followup() {
            return BY_AGENT.equals(createdBy);
        }

        /** Whether it is the application's, for a worker of the main agent on whose line it falls due. */
        public boolean forWorker() {
            return subject != null;
        }

        /** The same schedule, with the subject that its work is kept under when a main agent hands it to a worker. */
        public Schedule withSubject(String ofLine) {
            return new Schedule(appId, userId, scheduleId, createdBy, kind, name, instructions, note, chain, mergeKey, agentKey,
                    conversationId, fireAt, cron, timeZone, fingerprint, timerId, state, lastFiredAt, createdAt, ofLine);
        }

        /** The subject of the line it falls due on, when that is a subject's line; {@code null} otherwise. */
        public String subjectOfLine() {
            return mergeKey.startsWith(Threads.TASK_PREFIX) ? mergeKey.substring(Threads.TASK_PREFIX.length()) : null;
        }

        public ScheduleView view() {
            return new ScheduleView(scheduleId, createdBy, kind, name, instructions, note, fireAt, cron, timeZone,
                    subject != null ? subject : subjectOfLine(), agentKey, conversationId, state, lastFiredAt, createdAt);
        }
    }

    /** @param created whether this call made it, rather than found it */
    public record Created(ScheduleView schedule, boolean created) {
    }

    /** A follow-up the agent asks for. */
    public record Followup(String scheduleId, String mergeKey, String agentKey, String conversationId, Instant fireAt,
            String note, int chain) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Users users;
    private final Lines lines;
    private final InboxClient inbox;
    private final InboxRelay relay;
    private final AgentClock clock;
    private final Ids ids;
    /** The Inbox lane of the follow-ups that workers set themselves. */
    private final Lane workLane;

    public Schedules(JdbcClient jdbc, TransactionTemplate tx, Users users, Lines lines, InboxClient inbox, InboxRelay relay,
            AgentClock clock, Ids ids, AgentProperties properties) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.lines = lines;
        this.inbox = inbox;
        this.relay = relay;
        this.clock = clock;
        this.ids = ids;
        this.workLane = Lane.fromWire(properties.getTasks().getLane());
    }

    // ================================================================ the application's schedules

    /**
     * Sets a schedule up: stored here first, then given to the Inbox as a timer. When the Inbox
     * refuses the timer, nothing is kept.
     */
    public Created create(String appId, String userId, CreateScheduleRequest request) {
        Names.userId(userId);
        if (request == null) {
            throw AgentException.invalid("a request body is required");
        }
        String name = text(request.name(), "name", MAX_NAME);
        String instructions = text(request.instructions(), "instructions", MAX_INSTRUCTIONS);
        boolean once = request.at() != null;
        if (once == (request.cron() != null)) {
            throw AgentException.invalid("a schedule has either at, to run once, or cron, to repeat; exactly one of them");
        }
        Instant now = clock.now();
        if (once && (!request.at().isAfter(now) || request.at().isAfter(now.plus(MAX_AHEAD)))) {
            throw AgentException.invalid("at must be in the future, at most " + MAX_AHEAD.toDays() + " days ahead");
        }
        if (!once && (request.cron().isBlank() || request.cron().length() > 100)) {
            throw AgentException.invalid("cron is a rule of five fields, for example 0 8 * * 1-5");
        }
        if (once && request.timeZone() != null) {
            throw AgentException.invalid("time_zone goes with cron; at is a moment and needs none");
        }
        String zone = null;
        if (!once) {
            try {
                zone = request.timeZone() == null ? "UTC" : ZoneId.of(request.timeZone()).getId();
            } catch (DateTimeException e) {
                throw AgentException.invalid("time_zone is not a known time zone, such as Asia/Shanghai: " + request.timeZone());
            }
        }
        String scheduleId = request.scheduleId() == null ? ids.next("sch") : Names.localId(request.scheduleId(), "schedule_id");
        String subject = request.subject() == null ? "schedule-" + scheduleId : Names.localId(request.subject(), "subject");
        String agentKey = request.agent() == null ? null : Names.agentKey(request.agent());
        String conversationId = request.conversationId() == null ? Messages.DEFAULT_CONVERSATION
                : Names.localId(request.conversationId(), "conversation_id");

        ObjectNode asked = Json.STORED.createObjectNode();
        asked.put("name", name).put("instructions", instructions).put("subject", subject).put("agent", agentKey)
                .put("conversation_id", conversationId).put("at", once ? request.at().toString() : null)
                .put("cron", request.cron()).put("time_zone", zone);
        String fingerprint = Hashing.sha256Hex(CanonicalJson.of(asked).getBytes(StandardCharsets.UTF_8));
        String zoneId = zone;

        Optional<Schedule> earlier = tx.execute(status -> {
            users.ensure(appId, userId);
            users.lock(appId, userId);
            Optional<Schedule> existing = find(appId, userId, scheduleId);
            if (existing.isPresent()) {
                if (!existing.get().fingerprint().equals(fingerprint)) {
                    throw AgentException.conflict("schedule_id " + scheduleId + " was already used for another schedule of this user");
                }
                return existing;
            }
            // The subject's own line; or, where a main agent serves the conversation, the main agent's line. There
            // it falls due like everything that concerns the user, also when it is for another agent: the line then
            // hands it to that agent, as a task of the main agent.
            Lines.Route route = lines.route(appId, userId, subject, agentKey, conversationId,
                    "; a subject keeps the agent it started with");
            Threads.Thread line = switch (route) {
                case Lines.Direct direct -> direct.line();
                case Lines.Hub hub -> hub.line();
            };
            String worker = route instanceof Lines.Hub hub ? hub.worker() : null;
            insert(appId, userId, scheduleId, BY_APP, once ? CreateTimerRequest.ONCE : CreateTimerRequest.CRON, name,
                    instructions, null, 0, line.mergeKey(), worker == null ? line.agentKey() : worker, conversationId,
                    once ? request.at() : null, once ? null : request.cron().trim(), zoneId, fingerprint,
                    worker == null ? null : subject);
            return Optional.empty();
        });
        if (earlier.isPresent()) {
            Schedule schedule = earlier.get();
            if (schedule.timerId() == null && ACTIVE.equals(schedule.state())) {
                // An earlier attempt stored it and did not get to the Inbox. This one does.
                return new Created(withTimer(schedule).view(), false);
            }
            return new Created(schedule.view(), false);
        }
        return new Created(withTimer(find(appId, userId, scheduleId).orElseThrow()).view(), true);
    }

    public List<ScheduleView> active(String appId, String userId) {
        Names.userId(userId);
        return jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId AND state = 'active' ORDER BY created_at, schedule_id LIMIT 200")
                .param("appId", appId).param("userId", userId)
                .query(Schedules::row)
                .list().stream().map(Schedule::view).toList();
    }

    /** Cancels a schedule. It does not run again; a firing already under way finds it cancelled and does nothing. */
    public void cancel(String appId, String userId, String scheduleId) {
        Names.userId(userId);
        Schedule schedule = find(appId, userId, scheduleId)
                .orElseThrow(() -> AgentException.notFound("no schedule " + scheduleId + " for this user"));
        boolean cancelled = jdbc.sql("""
                UPDATE schedule SET state = 'cancelled', ended_at = :now
                 WHERE app_id = :appId AND user_id = :userId AND schedule_id = :scheduleId AND state = 'active'
                """)
                .param("now", Db.ts(clock.now())).param("appId", appId).param("userId", userId).param("scheduleId", scheduleId)
                .update() == 1;
        if (cancelled && schedule.timerId() != null) {
            try {
                ApiResult<Void> result = inbox.cancelTimer(appId, userId, schedule.timerId());
                if (!result.ok() && result.status() != 404) {
                    log.warn("the Inbox did not cancel timer {} of schedule {}: {} {}", schedule.timerId(), scheduleId,
                            result.status(), result.code());
                }
            } catch (RuntimeException e) {
                // The schedule is cancelled here, which is what counts: a firing that still comes is ignored.
                log.warn("the Inbox could not be told to cancel timer {} of schedule {}: {}", schedule.timerId(), scheduleId,
                        e.toString());
            }
        }
    }

    /**
     * Passes a schedule that fell due on its subject's line on to the main agent that serves its
     * conversation by now, as an event of the main agent's line: the main agent works on it there,
     * like on anything else that concerns the user. A firing is passed on once, however often this
     * is called for it. In the caller's transaction.
     *
     * @param hub the main agent's line
     */
    public void forward(Threads.Thread hub, Schedule schedule, Instant dueAt, boolean late) {
        ObjectNode payload = InboxJson.mapper().createObjectNode();
        payload.put("schedule_id", schedule.scheduleId()).put("due_at", dueAt.toString()).put("late", late);
        relay.enqueue(schedule.appId(), schedule.userId(), WriteEventRequest.builder(EventSource.AGENT_SELF, FORWARDED_EVENT)
                .sourceEventId("due:" + schedule.scheduleId() + ":" + dueAt)
                .mergeKey(hub.mergeKey())
                .lane(Lane.BACKGROUND)
                .interrupt(false)
                .occurredAt(clock.now())
                .payload(payload)
                .build());
    }

    /**
     * What the application has scheduled for workers of the main agent of a line, and that is still
     * to come: oldest first.
     */
    public List<Schedule> forWorkers(String appId, String userId, String hubKey) {
        return jdbc.sql(SELECT + """
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :hubKey AND state = 'active' AND subject IS NOT NULL
                 ORDER BY created_at, schedule_id LIMIT 50
                """)
                .param("appId", appId).param("userId", userId).param("hubKey", hubKey)
                .query(Schedules::row)
                .list();
    }

    /**
     * Cancels every schedule that waits on a line: the follow-ups an agent set itself there. Used
     * when the line is over, as when a delegated task is closed.
     */
    public void cancelLine(String appId, String userId, String mergeKey) {
        List<String> waiting = jdbc.sql("""
                SELECT schedule_id FROM schedule
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND state = 'active'
                """)
                .param("appId", appId).param("userId", userId).param("mergeKey", mergeKey)
                .query(String.class)
                .list();
        for (String scheduleId : waiting) {
            cancel(appId, userId, scheduleId);
        }
    }

    // ================================================================ the agent's follow-ups

    /** How many follow-ups wait on a line. */
    public long pendingFollowups(String appId, String userId, String mergeKey) {
        return jdbc.sql("""
                SELECT count(*) FROM schedule
                 WHERE app_id = :appId AND user_id = :userId AND merge_key = :mergeKey AND created_by = 'agent' AND state = 'active'
                """)
                .param("appId", appId).param("userId", userId).param("mergeKey", mergeKey)
                .query(Long.class).single();
    }

    /**
     * Stores a follow-up, in the caller's transaction. Its id is derived from the call that asked for
     * it, so asking a second time changes nothing.
     *
     * @return whether it was stored by this call
     */
    public boolean storeFollowup(String appId, String userId, Followup followup) {
        if (find(appId, userId, followup.scheduleId()).isPresent()) {
            return false;
        }
        insert(appId, userId, followup.scheduleId(), BY_AGENT, CreateTimerRequest.ONCE, null, null, followup.note(),
                followup.chain(), followup.mergeKey(), followup.agentKey(), followup.conversationId(), followup.fireAt(), null,
                null, followup.scheduleId(), null);
        return true;
    }

    /**
     * Gives a stored schedule its timer, if it has none yet. When the Inbox refuses, the schedule is
     * removed and the refusal is passed on.
     */
    public Schedule withTimer(Schedule schedule) {
        if (schedule.timerId() != null) {
            return schedule;
        }
        ObjectNode payload = InboxJson.mapper().createObjectNode();
        payload.put("schedule_id", schedule.scheduleId());
        // A worker's follow-up runs where the worker runs; everything else in the background.
        Lane lane = schedule.mergeKey().startsWith(Threads.WORK_PREFIX) ? workLane : Lane.BACKGROUND;
        CreateTimerRequest request = new CreateTimerRequest(schedule.kind(), schedule.fireAt(), schedule.cron(),
                schedule.timeZone(), schedule.scheduleId(), schedule.mergeKey(), lane, payload);
        ApiResult<TimerView> created;
        try {
            created = inbox.createTimer(schedule.appId(), schedule.userId(), request);
        } catch (RuntimeException e) {
            forget(schedule);
            throw AgentException.unavailable("the scheduler is not available right now; nothing was set up, try again");
        }
        if (!created.ok()) {
            forget(schedule);
            if (created.status() == 400) {
                throw AgentException.invalid("the schedule was refused: " + refusal(created));
            }
            throw AgentException.unavailable("the scheduler is not available right now; nothing was set up, try again");
        }
        jdbc.sql("UPDATE schedule SET timer_id = :timerId WHERE app_id = :appId AND user_id = :userId AND schedule_id = :scheduleId")
                .param("timerId", created.body().timerId()).param("appId", schedule.appId()).param("userId", schedule.userId())
                .param("scheduleId", schedule.scheduleId())
                .update();
        return find(schedule.appId(), schedule.userId(), schedule.scheduleId()).orElseThrow();
    }

    // ================================================================ for the turn

    public Optional<Schedule> find(String appId, String userId, String scheduleId) {
        return jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId AND schedule_id = :scheduleId")
                .param("appId", appId).param("userId", userId).param("scheduleId", scheduleId)
                .query(Schedules::row)
                .optional();
    }

    /** Notes that the schedules were due and handled: one that runs once is done. In the caller's transaction. */
    public void fired(String appId, String userId, List<String> scheduleIds) {
        for (String scheduleId : scheduleIds) {
            jdbc.sql("""
                    UPDATE schedule SET last_fired_at = :now,
                           state = CASE WHEN kind = 'once' AND state = 'active' THEN 'done' ELSE state END,
                           ended_at = CASE WHEN kind = 'once' AND state = 'active' THEN :now ELSE ended_at END
                     WHERE app_id = :appId AND user_id = :userId AND schedule_id = :scheduleId
                    """)
                    .param("now", Db.ts(clock.now())).param("appId", appId).param("userId", userId).param("scheduleId", scheduleId)
                    .update();
        }
    }

    // ================================================================ rows

    private void insert(String appId, String userId, String scheduleId, String createdBy, String kind, String name,
            String instructions, String note, int chain, String mergeKey, String agentKey, String conversationId,
            Instant fireAt, String cron, String timeZone, String fingerprint, String subject) {
        jdbc.sql("""
                INSERT INTO schedule (app_id, user_id, schedule_id, created_by, kind, name, instructions, note, chain, merge_key,
                                      agent_key, conversation_id, fire_at, cron, time_zone, fingerprint, state, created_at,
                                      subject)
                VALUES (:appId, :userId, :scheduleId, :createdBy, :kind, :name, :instructions, :note, :chain, :mergeKey,
                        :agentKey, :conversationId, :fireAt, :cron, :timeZone, :fingerprint, 'active', :now, :subject)
                """)
                .param("appId", appId).param("userId", userId).param("scheduleId", scheduleId).param("createdBy", createdBy)
                .param("kind", kind).param("name", name).param("instructions", instructions).param("note", note)
                .param("chain", chain).param("mergeKey", mergeKey).param("agentKey", agentKey)
                .param("conversationId", conversationId).param("fireAt", Db.ts(fireAt)).param("cron", cron)
                .param("timeZone", timeZone).param("fingerprint", fingerprint).param("now", Db.ts(clock.now()))
                .param("subject", subject)
                .update();
    }

    private void forget(Schedule schedule) {
        jdbc.sql("DELETE FROM schedule WHERE app_id = :appId AND user_id = :userId AND schedule_id = :scheduleId AND timer_id IS NULL")
                .param("appId", schedule.appId()).param("userId", schedule.userId()).param("scheduleId", schedule.scheduleId())
                .update();
    }

    private static String refusal(ApiResult<?> result) {
        return result.problem() != null && result.problem().detail() != null ? result.problem().detail()
                : result.status() + " " + result.code();
    }

    private static String text(String value, String what, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw AgentException.invalid(what + " is required and has at most " + max + " characters");
        }
        return value;
    }

    private static final String SELECT = """
            SELECT app_id, user_id, schedule_id, created_by, kind, name, instructions, note, chain, merge_key, agent_key,
                   conversation_id, fire_at, cron, time_zone, fingerprint, timer_id, state, last_fired_at, created_at, subject
              FROM schedule
            """;

    private static Schedule row(ResultSet rs, int rowNumber) throws SQLException {
        return new Schedule(rs.getString("app_id"), rs.getString("user_id"), rs.getString("schedule_id"),
                rs.getString("created_by"), rs.getString("kind"), rs.getString("name"), rs.getString("instructions"),
                rs.getString("note"), rs.getInt("chain"), rs.getString("merge_key"), rs.getString("agent_key"),
                rs.getString("conversation_id"), Db.instant(rs, "fire_at"), rs.getString("cron"), rs.getString("time_zone"),
                rs.getString("fingerprint"), rs.getString("timer_id"), rs.getString("state"), Db.instant(rs, "last_fired_at"),
                Db.instant(rs, "created_at"), rs.getString("subject"));
    }
}
