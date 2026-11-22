package dev.agentflow.agent.server.schedules;

import dev.agentflow.agent.api.CreateScheduleRequest;
import dev.agentflow.agent.api.ScheduleView;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.CanonicalJson;
import dev.agentflow.inbox.api.CreateTimerRequest;
import dev.agentflow.inbox.api.Hashing;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.TimerView;
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
 */
@Service
public class Schedules {

    private static final Logger log = LoggerFactory.getLogger(Schedules.class);

    /** The type of the events that the Inbox's timers produce. */
    public static final String TIMER_EVENT = "timer.due";

    public static final String BY_APP = "app";
    public static final String BY_AGENT = "agent";

    public static final String ACTIVE = "active";
    public static final String DONE = "done";
    public static final String CANCELLED = "cancelled";

    public static final int MAX_NAME = 200;
    public static final int MAX_INSTRUCTIONS = 4000;
    public static final Duration MAX_AHEAD = Duration.ofDays(366);

    public record Schedule(String appId, String userId, String scheduleId, String createdBy, String kind, String name,
            String instructions, String note, int chain, String mergeKey, String agentKey, String conversationId,
            Instant fireAt, String cron, String timeZone, String fingerprint, String timerId, String state,
            Instant lastFiredAt, Instant createdAt) {

        public boolean followup() {
            return BY_AGENT.equals(createdBy);
        }

        public ScheduleView view() {
            String subject = mergeKey.startsWith(Threads.TASK_PREFIX) ? mergeKey.substring(Threads.TASK_PREFIX.length()) : null;
            return new ScheduleView(scheduleId, createdBy, kind, name, instructions, note, fireAt, cron, timeZone, subject,
                    agentKey, conversationId, state, lastFiredAt, createdAt);
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
    private final Threads threads;
    private final AgentProfiles profiles;
    private final InboxClient inbox;
    private final AgentClock clock;
    private final Ids ids;

    public Schedules(JdbcClient jdbc, TransactionTemplate tx, Users users, Threads threads, AgentProfiles profiles,
            InboxClient inbox, AgentClock clock, Ids ids) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.threads = threads;
        this.profiles = profiles;
        this.inbox = inbox;
        this.clock = clock;
        this.ids = ids;
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
        String mergeKey = Threads.taskKey(subject);
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
            Optional<Threads.Thread> line = threads.find(appId, userId, mergeKey);
            if (line.isPresent() && agentKey != null && !line.get().agentKey().equals(agentKey)) {
                throw AgentException.conflict("subject " + subject + " is handled by agent " + line.get().agentKey()
                        + "; a subject keeps the agent it started with");
            }
            String agent = line.map(Threads.Thread::agentKey).orElse(agentKey == null ? Messages.DEFAULT_AGENT : agentKey);
            if (line.isEmpty()) {
                if (!profiles.exists(appId, agent)) {
                    throw AgentException.invalid("this application has no agent " + agent + "; create it with PUT /v1/agents/" + agent);
                }
                threads.ensureTask(appId, userId, subject, agent);
            }
            insert(appId, userId, scheduleId, BY_APP, once ? CreateTimerRequest.ONCE : CreateTimerRequest.CRON, name,
                    instructions, null, 0, mergeKey, agent, conversationId, once ? request.at() : null,
                    once ? null : request.cron().trim(), zoneId, fingerprint);
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
                null, followup.scheduleId());
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
        CreateTimerRequest request = new CreateTimerRequest(schedule.kind(), schedule.fireAt(), schedule.cron(),
                schedule.timeZone(), schedule.scheduleId(), schedule.mergeKey(), Lane.BACKGROUND, payload);
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
            Instant fireAt, String cron, String timeZone, String fingerprint) {
        jdbc.sql("""
                INSERT INTO schedule (app_id, user_id, schedule_id, created_by, kind, name, instructions, note, chain, merge_key,
                                      agent_key, conversation_id, fire_at, cron, time_zone, fingerprint, state, created_at)
                VALUES (:appId, :userId, :scheduleId, :createdBy, :kind, :name, :instructions, :note, :chain, :mergeKey,
                        :agentKey, :conversationId, :fireAt, :cron, :timeZone, :fingerprint, 'active', :now)
                """)
                .param("appId", appId).param("userId", userId).param("scheduleId", scheduleId).param("createdBy", createdBy)
                .param("kind", kind).param("name", name).param("instructions", instructions).param("note", note)
                .param("chain", chain).param("mergeKey", mergeKey).param("agentKey", agentKey)
                .param("conversationId", conversationId).param("fireAt", Db.ts(fireAt)).param("cron", cron)
                .param("timeZone", timeZone).param("fingerprint", fingerprint).param("now", Db.ts(clock.now()))
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
                   conversation_id, fire_at, cron, time_zone, fingerprint, timer_id, state, last_fired_at, created_at
              FROM schedule
            """;

    private static Schedule row(ResultSet rs, int rowNumber) throws SQLException {
        return new Schedule(rs.getString("app_id"), rs.getString("user_id"), rs.getString("schedule_id"),
                rs.getString("created_by"), rs.getString("kind"), rs.getString("name"), rs.getString("instructions"),
                rs.getString("note"), rs.getInt("chain"), rs.getString("merge_key"), rs.getString("agent_key"),
                rs.getString("conversation_id"), Db.instant(rs, "fire_at"), rs.getString("cron"), rs.getString("time_zone"),
                rs.getString("fingerprint"), rs.getString("timer_id"), rs.getString("state"), Db.instant(rs, "last_fired_at"),
                Db.instant(rs, "created_at"));
    }
}
