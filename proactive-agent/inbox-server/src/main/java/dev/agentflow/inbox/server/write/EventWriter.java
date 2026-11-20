package dev.agentflow.inbox.server.write;

import dev.agentflow.inbox.api.CanonicalJson;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.EventStatus;
import dev.agentflow.inbox.api.Hashing;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.core.IdGenerator;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.metrics.InboxMetrics;
import dev.agentflow.inbox.server.time.InboxClock;
import dev.agentflow.inbox.server.wake.WakeBus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * The single write entry of the inbox (invariant I1).
 *
 * <p>The write path does four things and nothing else: authenticate (done by the filter), validate,
 * dedupe, append. It never starts a run and never calls a model, so its latency does not depend on
 * the health of anything downstream. On the happy path it is one SQL statement.
 */
@Service
public class EventWriter {

    private static final Logger log = LoggerFactory.getLogger(EventWriter.class);

    private static final Pattern INBOX_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:@\\-]{0,127}");
    private static final int MAX_KEY_LENGTH = 256;
    private static final String FINGERPRINT_PREFIX = "fp1:";
    private static final String TIMER_KEY_PREFIX = "timer:";
    private static final String FINGERPRINT_WARNING =
            "no source_event_id was sent, so the event was fingerprinted; send an explicit idempotency key";

    /**
     * Append one event in a single round trip.
     *
     * <ol>
     * <li>{@code existing} / {@code deferred}: is this idempotency key already known? If so nothing is
     *     written and the inbox row is not even locked.</li>
     * <li>{@code seq}: upsert the inbox row. This takes its row lock and holds it until commit, so the
     *     next writer can only obtain the next {@code seq} after this one is visible. Sequence order is
     *     therefore commit order, and a reader never sees seq 6 without seq 5.</li>
     * <li>{@code ins}: insert the event; the unique index is the dedupe (invariant I2).</li>
     * <li>{@code tr}: record the first transition (invariant I7).</li>
     * </ol>
     */
    static final String APPEND_SQL = """
            WITH existing AS (
                SELECT event_id, payload_hash, status
                  FROM event
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND source_event_id = :sid
            ), deferred AS (
                SELECT event_id, payload_hash
                  FROM timer
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND source_event_id = :sid
                   AND state = 'scheduled' AND CAST(:checkDeferred AS boolean)
            ), seq AS (
                INSERT INTO inbox AS i (tenant_id, inbox_id, next_seq, created_at, updated_at)
                SELECT :tenant, :inbox, 2, :now, :now
                 WHERE NOT EXISTS (SELECT 1 FROM existing) AND NOT EXISTS (SELECT 1 FROM deferred)
                ON CONFLICT (tenant_id, inbox_id)
                DO UPDATE SET next_seq = i.next_seq + 1, updated_at = EXCLUDED.updated_at
                RETURNING next_seq - 1 AS seq
            ), ins AS (
                INSERT INTO event (event_id, tenant_id, inbox_id, seq, client_id, source, source_event_id, dedupe_kind,
                                   type, payload, payload_hash, payload_bytes, occurred_at, received_at, ready_at,
                                   merge_key, lane, resume_key, interrupt, trace_id, timer_id,
                                   status, attempt, max_attempts)
                SELECT :eventId, :tenant, :inbox, seq.seq, :clientId, :source, :sid, :dedupeKind,
                       :type, CAST(:payload AS jsonb), :payloadHash, :payloadBytes,
                       CAST(:occurredAt AS timestamptz), :now, :readyAt,
                       CAST(:mergeKey AS text), :lane, CAST(:resumeKey AS text), :interrupt, :traceId,
                       CAST(:timerId AS text), 'pending', 0, :maxAttempts
                  FROM seq
                ON CONFLICT (tenant_id, inbox_id, source_event_id) DO NOTHING
                RETURNING event_id, seq
            ), tr AS (
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor, at)
                SELECT event_id, :tenant, :inbox, NULL, 'pending', 'accepted', :actor, :now FROM ins
            )
            SELECT (SELECT event_id FROM ins)          AS inserted_id,
                   (SELECT seq FROM ins)               AS inserted_seq,
                   (SELECT event_id FROM existing)     AS existing_id,
                   (SELECT payload_hash FROM existing) AS existing_hash,
                   (SELECT status FROM existing)       AS existing_status,
                   (SELECT event_id FROM deferred)     AS deferred_id,
                   (SELECT payload_hash FROM deferred) AS deferred_hash
            """;

    /** A future {@code ready_at}: the event waits in the timer table and never enters the ready queue (I6). */
    static final String DEFER_SQL = """
            WITH existing AS (
                SELECT event_id, payload_hash, status
                  FROM event
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND source_event_id = :sid
            ), deferred AS (
                SELECT event_id, payload_hash, state
                  FROM timer
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND source_event_id = :sid
            ), ib AS (
                INSERT INTO inbox (tenant_id, inbox_id, next_seq, created_at, updated_at)
                SELECT :tenant, :inbox, 1, :now, :now
                 WHERE NOT EXISTS (SELECT 1 FROM existing) AND NOT EXISTS (SELECT 1 FROM deferred)
                ON CONFLICT (tenant_id, inbox_id) DO NOTHING
            ), ins AS (
                INSERT INTO timer (timer_id, fire_at, tenant_id, inbox_id, kind, state, event_id, event_source,
                                   event_type, source_event_id, dedupe_kind, merge_key, lane, resume_key, interrupt,
                                   payload, payload_hash, occurred_at, trace_id, client_id, created_at, updated_at)
                SELECT :timerId, :fireAt, :tenant, :inbox, 'once', 'scheduled', :eventId, :source,
                       :type, :sid, :dedupeKind, CAST(:mergeKey AS text), :lane, CAST(:resumeKey AS text), :interrupt,
                       CAST(:payload AS jsonb), :payloadHash, CAST(:occurredAt AS timestamptz), :traceId, :clientId,
                       :now, :now
                 WHERE NOT EXISTS (SELECT 1 FROM existing) AND NOT EXISTS (SELECT 1 FROM deferred)
                ON CONFLICT (tenant_id, inbox_id, source_event_id) WHERE source_event_id IS NOT NULL DO NOTHING
                RETURNING event_id
            )
            SELECT (SELECT event_id FROM ins)          AS inserted_id,
                   (SELECT event_id FROM existing)     AS existing_id,
                   (SELECT payload_hash FROM existing) AS existing_hash,
                   (SELECT status FROM existing)       AS existing_status,
                   (SELECT event_id FROM deferred)     AS deferred_id,
                   (SELECT payload_hash FROM deferred) AS deferred_hash,
                   (SELECT state FROM deferred)        AS deferred_state
            """;

    private static final String FIND_BY_KEY_SQL = """
            SELECT event_id, payload_hash, status
              FROM event
             WHERE tenant_id = :tenant AND inbox_id = :inbox AND source_event_id = :sid
            """;

    private static final String FIND_TIMER_BY_KEY_SQL = """
            SELECT event_id, payload_hash, state
              FROM timer
             WHERE tenant_id = :tenant AND inbox_id = :inbox AND source_event_id = :sid
            """;

    private final JdbcClient jdbc;
    private final InboxClock clock;
    private final InboxProperties properties;
    private final IdGenerator ids;
    private final InterruptDetector interruptDetector;
    private final WakeBus wakeBus;
    private final InboxMetrics metrics;

    public EventWriter(JdbcClient jdbc, InboxClock clock, InboxProperties properties, IdGenerator ids,
            InterruptDetector interruptDetector, WakeBus wakeBus, InboxMetrics metrics) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.properties = properties;
        this.ids = ids;
        this.interruptDetector = interruptDetector;
        this.wakeBus = wakeBus;
        this.metrics = metrics;
    }

    /** Handles {@code POST /v1/inbox/{inbox_id}/events}. */
    public WriteEventResponse write(AuthenticatedClient client, String tenantId, String inboxId, WriteEventRequest request) {
        long started = System.nanoTime();
        String result = "error";
        Lane lane = null;
        try {
            validate(inboxId, request);
            lane = request.lane() != null ? request.lane() : Lane.defaultFor(request.source());

            String payloadJson = CanonicalJson.of(request.payload());
            int payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8).length;
            if (payloadBytes > properties.getPayload().getMaxBytes()) {
                result = "rejected";
                throw InboxException.tooLarge("payload is " + payloadBytes + " bytes; the limit is "
                        + properties.getPayload().getMaxBytes());
            }
            byte[] payloadHash = Hashing.sha256(request.type() + "\n" + payloadJson);

            Instant now = clock.now();
            boolean fingerprinted = request.sourceEventId() == null;
            String sourceEventId = fingerprinted
                    ? fingerprint(tenantId, client.clientId(), request, payloadHash, now)
                    : request.sourceEventId();
            boolean interrupt = request.interrupt() != null
                    ? request.interrupt()
                    : interruptDetector.isInterrupt(request.source(), request.payload());
            String traceId = request.traceId() != null ? request.traceId() : ids.newId();
            boolean defer = request.readyAt() != null && request.readyAt().isAfter(now);

            AppendCommand command = new AppendCommand(tenantId, inboxId, ids.newId(), client.clientId(),
                    request.source(), sourceEventId, fingerprinted ? "fingerprint" : "provided", request.type(),
                    payloadJson, payloadHash, payloadBytes, request.occurredAt(), defer ? request.readyAt() : now,
                    request.mergeKey(), lane, request.resumeKey(), interrupt, traceId, null, client.actor(),
                    properties.getRetry().getMaxAttempts(), true);

            AppendOutcome outcome = defer ? defer(command, now) : append(command, now);

            if (outcome.inserted()) {
                result = "accepted";
                if (!defer) {
                    publishWake();
                }
                return new WriteEventResponse(outcome.insertedId(), false,
                        defer ? EventStatus.SCHEDULED : EventStatus.PENDING.wire(),
                        fingerprinted ? Boolean.TRUE : null, fingerprinted ? FINGERPRINT_WARNING : null);
            }
            if (!outcome.samePayload(payloadHash)) {
                result = "conflict";
                throw InboxException.conflict(ProblemCodes.IDEMPOTENCY_CONFLICT,
                        "source_event_id '" + sourceEventId + "' was already used for a different event "
                                + outcome.existingId() + "; an idempotency key must never be reused");
            }
            result = "deduped";
            return new WriteEventResponse(outcome.existingId(), true, outcome.existingStatus(),
                    fingerprinted ? Boolean.TRUE : null, fingerprinted ? FINGERPRINT_WARNING : null);
        } catch (InboxException e) {
            if ("error".equals(result)) {
                result = "rejected";
            }
            throw e;
        } finally {
            metrics.write(result, tenantId, lane, System.nanoTime() - started);
        }
    }

    /**
     * Runs the append statement. Joins the caller's transaction when there is one (the timer scheduler
     * fires a timer and appends its event atomically); otherwise the statement is its own transaction.
     */
    public AppendOutcome append(AppendCommand c, Instant now) {
        Row row = jdbc.sql(APPEND_SQL)
                .param("tenant", c.tenantId())
                .param("inbox", c.inboxId())
                .param("sid", c.sourceEventId())
                .param("checkDeferred", c.checkDeferred())
                .param("now", Db.ts(now))
                .param("eventId", c.eventId())
                .param("clientId", c.clientId())
                .param("source", c.source().wire())
                .param("dedupeKind", c.dedupeKind())
                .param("type", c.type())
                .param("payload", c.payloadJson())
                .param("payloadHash", c.payloadHash())
                .param("payloadBytes", c.payloadBytes())
                .param("occurredAt", Db.ts(c.occurredAt()))
                .param("readyAt", Db.ts(c.readyAt()))
                .param("mergeKey", c.mergeKey())
                .param("lane", c.lane().wire())
                .param("resumeKey", c.resumeKey())
                .param("interrupt", c.interrupt())
                .param("traceId", c.traceId())
                .param("timerId", c.timerId())
                .param("maxAttempts", c.maxAttempts())
                .param("actor", c.actor())
                .query((rs, n) -> new Row(rs.getString("inserted_id"), Db.longOrNull(rs, "inserted_seq"),
                        rs.getString("existing_id"), rs.getBytes("existing_hash"), rs.getString("existing_status"),
                        rs.getString("deferred_id"), rs.getBytes("deferred_hash"), null))
                .single();
        return resolve(row, c);
    }

    private AppendOutcome defer(AppendCommand c, Instant now) {
        Row row = jdbc.sql(DEFER_SQL)
                .param("tenant", c.tenantId())
                .param("inbox", c.inboxId())
                .param("sid", c.sourceEventId())
                .param("now", Db.ts(now))
                .param("timerId", ids.newId())
                .param("fireAt", Db.ts(c.readyAt()))
                .param("eventId", c.eventId())
                .param("clientId", c.clientId())
                .param("source", c.source().wire())
                .param("dedupeKind", c.dedupeKind())
                .param("type", c.type())
                .param("payload", c.payloadJson())
                .param("payloadHash", c.payloadHash())
                .param("occurredAt", Db.ts(c.occurredAt()))
                .param("mergeKey", c.mergeKey())
                .param("lane", c.lane().wire())
                .param("resumeKey", c.resumeKey())
                .param("interrupt", c.interrupt())
                .param("traceId", c.traceId())
                .query((rs, n) -> new Row(rs.getString("inserted_id"), null,
                        rs.getString("existing_id"), rs.getBytes("existing_hash"), rs.getString("existing_status"),
                        rs.getString("deferred_id"), rs.getBytes("deferred_hash"), rs.getString("deferred_state")))
                .single();
        return resolve(row, c);
    }

    private AppendOutcome resolve(Row row, AppendCommand c) {
        if (row.insertedId != null) {
            return new AppendOutcome(row.insertedId, row.seq, null, null, null);
        }
        if (row.existingId != null) {
            return new AppendOutcome(null, null, row.existingId, row.existingHash, row.existingStatus);
        }
        if (row.deferredId != null) {
            return new AppendOutcome(null, null, row.deferredId, row.deferredHash, deferredStatus(row.deferredState));
        }
        // Lost a race: another writer inserted this key after our snapshot was taken, so ON CONFLICT
        // skipped our insert. One extra read finds the winner. This is the only second round trip.
        List<AppendOutcome> event = jdbc.sql(FIND_BY_KEY_SQL)
                .param("tenant", c.tenantId()).param("inbox", c.inboxId()).param("sid", c.sourceEventId())
                .query((rs, n) -> new AppendOutcome(null, null, rs.getString("event_id"),
                        rs.getBytes("payload_hash"), rs.getString("status")))
                .list();
        if (!event.isEmpty()) {
            return event.getFirst();
        }
        List<AppendOutcome> timer = jdbc.sql(FIND_TIMER_BY_KEY_SQL)
                .param("tenant", c.tenantId()).param("inbox", c.inboxId()).param("sid", c.sourceEventId())
                .query((rs, n) -> new AppendOutcome(null, null, rs.getString("event_id"),
                        rs.getBytes("payload_hash"), deferredStatus(rs.getString("state"))))
                .list();
        if (!timer.isEmpty()) {
            return timer.getFirst();
        }
        throw InboxException.unavailable("the write could not be confirmed; retry with the same idempotency key");
    }

    private static String deferredStatus(String timerState) {
        return timerState == null || "scheduled".equals(timerState) ? EventStatus.SCHEDULED : timerState;
    }

    /** A wake hint is an optimisation. Failing to send one must never fail the write. */
    private void publishWake() {
        try {
            wakeBus.publish();
        } catch (RuntimeException e) {
            log.warn("wake hint could not be published; claimers will find the event by polling", e);
        }
    }

    private void validate(String inboxId, WriteEventRequest request) {
        if (inboxId == null || !INBOX_ID.matcher(inboxId).matches()) {
            throw InboxException.invalid("inbox_id must match " + INBOX_ID.pattern());
        }
        if (request == null) {
            throw InboxException.invalid("a request body is required");
        }
        if (request.source() == null) {
            throw InboxException.invalid("source is required");
        }
        if (request.type() == null || request.type().isBlank() || request.type().length() > MAX_KEY_LENGTH) {
            throw InboxException.invalid("type is required and may be at most " + MAX_KEY_LENGTH + " characters");
        }
        if (request.payload() == null || request.payload().isNull() || request.payload().isMissingNode()) {
            throw InboxException.invalid("payload is required");
        }
        checkKey("source_event_id", request.sourceEventId());
        checkKey("merge_key", request.mergeKey());
        checkKey("resume_key", request.resumeKey());
        checkKey("trace_id", request.traceId());
        String sid = request.sourceEventId();
        if (sid != null && (sid.startsWith(FINGERPRINT_PREFIX) || sid.startsWith(TIMER_KEY_PREFIX))) {
            throw InboxException.invalid("source_event_id may not start with the reserved prefix '"
                    + FINGERPRINT_PREFIX + "' or '" + TIMER_KEY_PREFIX + "'");
        }
    }

    private static void checkKey(String name, String value) {
        if (value != null && (value.isBlank() || value.length() > MAX_KEY_LENGTH)) {
            throw InboxException.invalid(name + " may not be blank or longer than " + MAX_KEY_LENGTH + " characters");
        }
    }

    /**
     * Idempotency key for an event that came without one. Identical retries collapse; events with
     * different payloads never do, so nothing is lost silently. The discriminator is the producer's
     * {@code occurred_at} when present, otherwise a time bucket of the arrival time (a retry that
     * straddles a bucket edge becomes a second event, which is why an explicit key is recommended).
     */
    private String fingerprint(String tenantId, String clientId, WriteEventRequest request, byte[] payloadHash, Instant now) {
        long bucketSeconds = Math.max(1, properties.getFingerprint().getBucket().toSeconds());
        String discriminator = request.occurredAt() != null
                ? "at:" + request.occurredAt()
                : "bucket:" + (now.getEpochSecond() / bucketSeconds);
        String material = String.join("\u001f", tenantId, clientId, request.source().wire(), request.type(),
                request.mergeKey() == null ? "" : request.mergeKey(), Hashing.hex(payloadHash), discriminator);
        return FINGERPRINT_PREFIX + Hashing.sha256Hex(material.getBytes(StandardCharsets.UTF_8));
    }

    public static String timerEventKey(String timerId, Instant fireAt) {
        return TIMER_KEY_PREFIX + timerId + ":" + fireAt.getEpochSecond();
    }

    private record Row(String insertedId, Long seq, String existingId, byte[] existingHash, String existingStatus,
            String deferredId, byte[] deferredHash, String deferredState) {
    }
}
