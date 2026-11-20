package dev.agentflow.inbox.server.timer;

import dev.agentflow.inbox.api.CanonicalJson;
import dev.agentflow.inbox.api.CreateTimerRequest;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.TimerView;
import dev.agentflow.inbox.server.admin.AuditLog;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.auth.Role;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.core.IdGenerator;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.time.InboxClock;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Timers made through the API. Deferred events (a write with a future {@code ready_at}) share the
 * table but are created by the write path.
 */
@Service
public class TimerService {

    /** Type of the event an API timer produces. */
    public static final String TIMER_EVENT_TYPE = "timer.due";

    private static final Pattern INBOX_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:@\\-]{0,127}");

    static final String TIMER_COLUMNS = """
            timer_id, tenant_id, inbox_id, kind, state, fire_at, cron_expr, time_zone, timer_key, event_type,
            merge_key, lane, payload, fired_event_id, fired_at, catch_up, missed_count, created_at
            """;

    /** A timer and whether this call created it. */
    public record Created(TimerView timer, boolean created) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final InboxClock clock;
    private final InboxProperties properties;
    private final IdGenerator ids;
    private final JsonMapper json;
    private final AuditLog audit;

    public TimerService(JdbcClient jdbc, TransactionTemplate transactions, InboxClock clock,
            InboxProperties properties, IdGenerator ids, JsonMapper json, AuditLog audit) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.properties = properties;
        this.ids = ids;
        this.json = json;
        this.audit = audit;
    }

    public Created create(AuthenticatedClient client, String tenantId, String inboxId, CreateTimerRequest request) {
        if (!INBOX_ID.matcher(inboxId).matches()) {
            throw InboxException.invalid("inbox_id must match " + INBOX_ID.pattern());
        }
        Instant now = clock.now();
        boolean cron = CreateTimerRequest.CRON.equals(request.kind());
        if (!cron && !CreateTimerRequest.ONCE.equals(request.kind())) {
            throw InboxException.invalid("kind must be 'once' or 'cron'");
        }
        Instant fireAt;
        String zone = null;
        if (cron) {
            CronSchedule schedule;
            try {
                schedule = CronSchedule.parse(request.cron(), request.timeZone());
            } catch (IllegalArgumentException e) {
                throw InboxException.invalid("invalid cron timer: " + e.getMessage());
            }
            Duration minimum = properties.getTimers().getMinCronPeriod();
            if (schedule.shortestPeriod(now).compareTo(minimum) < 0) {
                throw InboxException.invalid("a cron timer may not fire more often than every " + minimum.toSeconds() + " s");
            }
            zone = schedule.zone().getId();
            fireAt = schedule.next(now);
        } else {
            if (request.fireAt() == null) {
                throw InboxException.invalid("fire_at is required for a 'once' timer");
            }
            if (request.cron() != null) {
                throw InboxException.invalid("a 'once' timer has no cron expression");
            }
            fireAt = request.fireAt();
        }
        JsonNode payload = request.payload() == null ? json.createObjectNode() : request.payload();
        String payloadJson = CanonicalJson.of(payload);
        if (payloadJson.getBytes(StandardCharsets.UTF_8).length > properties.getPayload().getMaxBytes() / 2) {
            throw InboxException.tooLarge("timer payload is too large");
        }
        Lane lane = request.lane() != null ? request.lane() : Lane.BACKGROUND;
        String zoneId = zone;
        String timerId = ids.newId();

        return transactions.execute(tx -> {
            jdbc.sql("""
                    INSERT INTO inbox (tenant_id, inbox_id, next_seq, created_at, updated_at)
                    VALUES (:tenant, :inbox, 1, :now, :now)
                    ON CONFLICT (tenant_id, inbox_id) DO NOTHING
                    """)
                    .param("tenant", tenantId).param("inbox", inboxId).param("now", Db.ts(now))
                    .update();
            int inserted = jdbc.sql("""
                    INSERT INTO timer (timer_id, fire_at, tenant_id, inbox_id, kind, cron_expr, time_zone, timer_key,
                                       state, event_source, event_type, merge_key, lane, payload, client_id,
                                       created_at, updated_at)
                    VALUES (:timerId, :fireAt, :tenant, :inbox, :kind, CAST(:cron AS text), CAST(:zone AS text),
                            CAST(:timerKey AS text), 'scheduled', :source, :type, CAST(:mergeKey AS text), :lane,
                            CAST(:payload AS jsonb), :clientId, :now, :now)
                    ON CONFLICT (tenant_id, inbox_id, timer_key) WHERE timer_key IS NOT NULL AND state = 'scheduled'
                    DO NOTHING
                    """)
                    .param("timerId", timerId).param("fireAt", Db.ts(fireAt)).param("tenant", tenantId)
                    .param("inbox", inboxId).param("kind", request.kind()).param("cron", cron ? request.cron().trim() : null)
                    .param("zone", zoneId).param("timerKey", request.timerKey())
                    .param("source", EventSource.TIMER.wire()).param("type", TIMER_EVENT_TYPE)
                    .param("mergeKey", request.mergeKey()).param("lane", lane.wire()).param("payload", payloadJson)
                    .param("clientId", client.clientId()).param("now", Db.ts(now))
                    .update();
            if (inserted == 1) {
                return new Created(get(tenantId, inboxId, timerId, null), true);
            }
            // The key is already scheduled: creating it again is a no-op that returns the existing timer.
            TimerView existing = jdbc.sql("SELECT " + TIMER_COLUMNS + """
                      FROM timer
                     WHERE tenant_id = :tenant AND inbox_id = :inbox AND timer_key = :timerKey AND state = 'scheduled'
                    """)
                    .param("tenant", tenantId).param("inbox", inboxId).param("timerKey", request.timerKey())
                    .query((rs, n) -> map(rs)).single();
            return new Created(existing, false);
        });
    }

    /**
     * The latest occurrence of a timer.
     *
     * @param clientId when set, only timers created by this client are visible (a producer sees its own)
     */
    public TimerView get(String tenantId, String inboxId, String timerId, String clientId) {
        List<TimerView> timers = jdbc.sql("SELECT " + TIMER_COLUMNS + """
                  FROM timer
                 WHERE timer_id = :timerId AND tenant_id = :tenant AND inbox_id = :inbox
                   AND (CAST(:clientId AS text) IS NULL OR client_id = :clientId)
                 ORDER BY fire_at DESC
                 LIMIT 1
                """)
                .param("timerId", timerId).param("tenant", tenantId).param("inbox", inboxId)
                .param("clientId", clientId)
                .query((rs, n) -> map(rs)).list();
        if (timers.isEmpty()) {
            throw InboxException.notFound("no such timer in this inbox");
        }
        return timers.getFirst();
    }

    /**
     * Cancels the scheduled occurrence. A timer that is firing right now wins the race and fires.
     *
     * <p>A producer cancels the timers it created. An operator may cancel any timer of the tenant, and
     * that is an operator action like any other: it is written to the audit log.
     */
    public void cancel(AuthenticatedClient client, String tenantId, String inboxId, String timerId) {
        boolean operator = client.has(Role.OPERATOR);
        Instant now = clock.now();
        transactions.executeWithoutResult(tx -> {
            int cancelled = jdbc.sql("""
                    UPDATE timer SET state = 'cancelled', updated_at = :now
                     WHERE timer_id = :timerId AND tenant_id = :tenant AND inbox_id = :inbox AND state = 'scheduled'
                       AND (CAST(:clientId AS text) IS NULL OR client_id = :clientId)
                    """)
                    .param("now", Db.ts(now)).param("timerId", timerId).param("tenant", tenantId)
                    .param("inbox", inboxId).param("clientId", operator ? null : client.clientId())
                    .update();
            if (cancelled == 0) {
                throw InboxException.notFound("no scheduled timer with this id in this inbox");
            }
            if (operator) {
                audit.record(tenantId, client.clientId(), client.clientId(), "timer_cancel",
                        json.createObjectNode().put("inbox_id", inboxId).put("timer_id", timerId), null, null, now);
            }
        });
    }

    TimerView map(ResultSet rs) throws SQLException {
        return new TimerView(rs.getString("timer_id"), rs.getString("tenant_id"), rs.getString("inbox_id"),
                rs.getString("kind"), rs.getString("state"), Db.instant(rs, "fire_at"), rs.getString("cron_expr"),
                rs.getString("time_zone"), rs.getString("timer_key"), rs.getString("event_type"),
                rs.getString("merge_key"), Lane.fromWire(rs.getString("lane")), json.readTree(rs.getString("payload")),
                rs.getString("fired_event_id"), Db.instant(rs, "fired_at"), rs.getBoolean("catch_up"),
                rs.getInt("missed_count"), Db.instant(rs, "created_at"));
    }
}
