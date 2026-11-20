package dev.agentflow.inbox.server.timer;

import dev.agentflow.inbox.api.CanonicalJson;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Hashing;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.core.IdGenerator;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.time.InboxClock;
import dev.agentflow.inbox.server.wake.WakeBus;
import dev.agentflow.inbox.server.write.AppendCommand;
import dev.agentflow.inbox.server.write.AppendOutcome;
import dev.agentflow.inbox.server.write.EventWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Turns due timers into events. A timer never executes anything: it appends an event through the
 * normal write statement, so the event gets dedupe, single flight, batching and replay like any other.
 *
 * <p>Firing is one transaction per timer: lock the timer row (skipping rows another scheduler holds),
 * append the event, mark the row fired, and for a cron insert the next occurrence. A crash rolls all
 * of it back, so a timer neither fires twice nor gets lost, and the loop can run on every instance.
 */
@Component
public class TimerScheduler {

    private static final Logger log = LoggerFactory.getLogger(TimerScheduler.class);
    private static final String ACTOR = "system:timer-scheduler";
    /** Upper bound on occurrences walked when catching up after an outage. */
    private static final int MAX_CATCH_UP_STEPS = 200_000;

    private record Due(String timerId, Instant fireAt) {
    }

    private record TimerRow(String timerId, Instant fireAt, String tenantId, String inboxId, String kind,
            String cronExpr, String timeZone, String eventId, String eventSource, String eventType,
            String sourceEventId, String dedupeKind, String mergeKey, Lane lane, String resumeKey, boolean interrupt,
            String payload, byte[] payloadHash, Instant occurredAt, String traceId, String clientId) {

        boolean deferredEvent() {
            return sourceEventId != null;
        }
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final InboxClock clock;
    private final InboxProperties properties;
    private final EventWriter writer;
    private final IdGenerator ids;
    private final WakeBus wakeBus;
    private final JsonMapper json;

    public TimerScheduler(JdbcClient jdbc, TransactionTemplate transactions, InboxClock clock,
            InboxProperties properties, EventWriter writer, IdGenerator ids, WakeBus wakeBus, JsonMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.properties = properties;
        this.writer = writer;
        this.ids = ids;
        this.wakeBus = wakeBus;
        this.json = json;
    }

    /** One pass. Returns how many timers fired. */
    public int runOnce() {
        List<Due> due = jdbc.sql("""
                SELECT timer_id, fire_at
                  FROM timer
                 WHERE state = 'scheduled' AND fire_at <= :now
                 ORDER BY fire_at
                 LIMIT :limit
                """)
                .param("now", Db.ts(clock.now()))
                .param("limit", properties.getScheduling().getBatchSize())
                .query((rs, n) -> new Due(rs.getString("timer_id"), Db.instant(rs, "fire_at")))
                .list();
        int fired = 0;
        for (Due timer : due) {
            if (Boolean.TRUE.equals(transactions.execute(tx -> fire(timer)))) {
                fired++;
            }
        }
        if (fired > 0) {
            try {
                wakeBus.publish();
            } catch (RuntimeException e) {
                log.warn("wake hint could not be published", e);
            }
        }
        return fired;
    }

    private boolean fire(Due due) {
        List<TimerRow> rows = jdbc.sql("""
                SELECT timer_id, fire_at, tenant_id, inbox_id, kind, cron_expr, time_zone, event_id, event_source,
                       event_type, source_event_id, dedupe_kind, merge_key, lane, resume_key, interrupt, payload,
                       payload_hash, occurred_at, trace_id, client_id
                  FROM timer
                 WHERE timer_id = :timerId AND fire_at = :fireAt AND state = 'scheduled'
                   FOR UPDATE SKIP LOCKED
                """)
                .param("timerId", due.timerId()).param("fireAt", Db.ts(due.fireAt()))
                .query((rs, n) -> new TimerRow(rs.getString("timer_id"), Db.instant(rs, "fire_at"),
                        rs.getString("tenant_id"), rs.getString("inbox_id"), rs.getString("kind"),
                        rs.getString("cron_expr"), rs.getString("time_zone"), rs.getString("event_id"),
                        rs.getString("event_source"), rs.getString("event_type"), rs.getString("source_event_id"),
                        rs.getString("dedupe_kind"), rs.getString("merge_key"), Lane.fromWire(rs.getString("lane")),
                        rs.getString("resume_key"), rs.getBoolean("interrupt"), rs.getString("payload"),
                        rs.getBytes("payload_hash"), Db.instant(rs, "occurred_at"), rs.getString("trace_id"),
                        rs.getString("client_id")))
                .list();
        if (rows.isEmpty()) {
            return false; // another scheduler has it, or it was cancelled
        }
        TimerRow timer = rows.getFirst();
        Instant now = clock.now();

        if (!"cron".equals(timer.kind())) {
            String eventId = append(timer, timer.fireAt(), timer.lane(), false, 0, now);
            markFired(timer.timerId(), timer.fireAt(), eventId, now);
            return true;
        }

        // Cron: which occurrence is the latest one that is due, and how many were skipped before it?
        CronSchedule schedule = CronSchedule.parse(timer.cronExpr(), timer.timeZone());
        Instant latest = timer.fireAt();
        int missed = 0;
        Instant next = schedule.next(latest);
        while (!next.isAfter(now) && missed < MAX_CATCH_UP_STEPS) {
            latest = next;
            missed++;
            next = schedule.next(latest);
        }
        if (missed == 0) {
            String eventId = append(timer, timer.fireAt(), timer.lane(), false, 0, now);
            markFired(timer.timerId(), timer.fireAt(), eventId, now);
        } else {
            // After an outage only the most recent missed occurrence fires, once, on the batch lane, so
            // a restart sends one daily report instead of six and does not flood the queue.
            jdbc.sql("""
                    UPDATE timer SET state = 'missed', missed_count = :missed, updated_at = :now
                     WHERE timer_id = :timerId AND fire_at = :fireAt
                    """)
                    .param("missed", missed).param("now", Db.ts(now)).param("timerId", timer.timerId())
                    .param("fireAt", Db.ts(timer.fireAt()))
                    .update();
            String eventId = append(timer, latest, Lane.BATCH, true, missed, now);
            insertOccurrence(timer, latest, "fired", true, missed, eventId, now);
        }
        insertOccurrence(timer, next, "scheduled", false, 0, null, now);
        return true;
    }

    /** Appends the event of one occurrence and returns its id (the existing id when it was a duplicate). */
    private String append(TimerRow timer, Instant occurrence, Lane lane, boolean catchUp, int missed, Instant now) {
        String payloadJson;
        byte[] payloadHash;
        String sourceEventId;
        String dedupeKind;
        if (timer.deferredEvent()) {
            // A deferred event fires as itself: the type, key and payload the producer sent.
            payloadJson = CanonicalJson.of(json.readTree(timer.payload()));
            payloadHash = timer.payloadHash();
            sourceEventId = timer.sourceEventId();
            dedupeKind = timer.dedupeKind();
        } else {
            ObjectNode body = json.createObjectNode();
            body.put("timer_id", timer.timerId());
            body.put("fire_at", occurrence.toString());
            body.put("kind", timer.kind());
            body.put("catch_up", catchUp);
            body.put("missed_count", missed);
            JsonNode userPayload = json.readTree(timer.payload());
            body.set("payload", userPayload);
            payloadJson = CanonicalJson.of(body);
            payloadHash = Hashing.sha256(timer.eventType() + "\n" + payloadJson);
            sourceEventId = EventWriter.timerEventKey(timer.timerId(), occurrence);
            dedupeKind = "provided";
        }
        AppendCommand command = new AppendCommand(timer.tenantId(), timer.inboxId(),
                timer.eventId() != null ? timer.eventId() : ids.newId(), timer.clientId(),
                EventSource.fromWire(timer.eventSource()), sourceEventId, dedupeKind, timer.eventType(), payloadJson,
                payloadHash, payloadJson.getBytes(StandardCharsets.UTF_8).length, timer.occurredAt(), occurrence,
                timer.mergeKey(), lane, timer.resumeKey(), timer.interrupt(),
                timer.traceId() != null ? timer.traceId() : ids.newId(), timer.timerId(), ACTOR,
                properties.getRetry().getMaxAttempts(), false);
        AppendOutcome outcome = writer.append(command, now);
        return outcome.inserted() ? outcome.insertedId() : outcome.existingId();
    }

    private void markFired(String timerId, Instant fireAt, String eventId, Instant now) {
        jdbc.sql("""
                UPDATE timer SET state = 'fired', fired_event_id = :eventId, fired_at = :now, updated_at = :now
                 WHERE timer_id = :timerId AND fire_at = :fireAt
                """)
                .param("eventId", eventId).param("now", Db.ts(now)).param("timerId", timerId)
                .param("fireAt", Db.ts(fireAt))
                .update();
    }

    /** Adds another occurrence of a cron series, copying its definition from the row being fired. */
    private void insertOccurrence(TimerRow timer, Instant fireAt, String state, boolean catchUp, int missed,
            String firedEventId, Instant now) {
        jdbc.sql("""
                INSERT INTO timer (timer_id, fire_at, tenant_id, inbox_id, kind, cron_expr, time_zone, timer_key, state,
                                   event_source, event_type, merge_key, lane, payload, client_id, fired_event_id,
                                   fired_at, catch_up, missed_count, created_at, updated_at)
                SELECT timer_id, :newFireAt, tenant_id, inbox_id, kind, cron_expr, time_zone, timer_key, :state,
                       event_source, event_type, merge_key, lane, payload, client_id, CAST(:firedEventId AS text),
                       CAST(:firedAt AS timestamptz), :catchUp, :missed, :now, :now
                  FROM timer
                 WHERE timer_id = :timerId AND fire_at = :fireAt
                ON CONFLICT (timer_id, fire_at) DO NOTHING
                """)
                .param("newFireAt", Db.ts(fireAt)).param("state", state).param("firedEventId", firedEventId)
                .param("firedAt", firedEventId == null ? null : Db.ts(now)).param("catchUp", catchUp)
                .param("missed", missed).param("now", Db.ts(now)).param("timerId", timer.timerId())
                .param("fireAt", Db.ts(timer.fireAt()))
                .update();
    }
}
