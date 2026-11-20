package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.RunStatus;
import dev.agentflow.inbox.api.SteerRequest;
import dev.agentflow.inbox.api.SteerResponse;
import dev.agentflow.inbox.api.SuspendRequest;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.metrics.InboxMetrics;
import dev.agentflow.inbox.server.time.InboxClock;
import dev.agentflow.inbox.server.wake.WakeBus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Everything that happens to a run after it was claimed.
 *
 * <p>Every operation except heartbeat takes the inbox row lock first and then the run row, the same
 * order as a claim, so they serialise cleanly: whichever of "ack" and "lease expired" gets the lock
 * first wins and the other finds the run no longer in the state it guards on. Callers prove ownership
 * with the run's {@code epoch}; a worker that lost its lease gets 409 and must stop.
 */
@Service
public class RunLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(RunLifecycleService.class);

    /** The run row as seen under its lock. */
    public record LockedRun(String runId, String tenantId, String inboxId, Lane lane, String mergeKey,
            RunStatus status, int epoch, int attempt, int maxAttempts, int nextRunSeq, boolean acceptsSteer,
            String leaseOwner, Instant leaseExpiresAt, Instant leaseDeadlineAt, String resumeKey,
            Instant suspendDeadlineAt, String onTimeout, Instant nextAttemptAt) {

        /** Name recorded in the event history for what this run's worker did. */
        public String workerActor() {
            return "worker:" + leaseOwner;
        }
    }

    private record RunRef(String tenantId, String inboxId) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final InboxClock clock;
    private final InboxProperties properties;
    private final WakeBus wakeBus;
    private final InboxMetrics metrics;
    private final RunEventStore events;
    private final RunSignals signals;

    public RunLifecycleService(JdbcClient jdbc, TransactionTemplate transactions, InboxClock clock,
            InboxProperties properties, WakeBus wakeBus, InboxMetrics metrics, RunEventStore events,
            RunSignals signals) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.properties = properties;
        this.wakeBus = wakeBus;
        this.metrics = metrics;
        this.events = events;
        this.signals = signals;
    }

    // ---- heartbeat ------------------------------------------------------------------------------

    /**
     * Renews the lease. This is a single guarded update of the run row and takes no other lock, so it
     * is cheap enough to call often. The lease is extended by the lease duration but never beyond the
     * deadline fixed when the lease was granted, so a run that loops forever cannot hold a slot forever.
     */
    public HeartbeatResponse heartbeat(String tenantScope, String runId, HeartbeatRequest request) {
        Instant now = clock.now();
        Instant target = now.plus(properties.getLease().getDuration());
        record Renewed(String tenantId, String inboxId, Lane lane, String mergeKey, Instant leaseExpiresAt,
                boolean acceptsSteer) {
        }
        List<Renewed> renewed = jdbc.sql("""
                UPDATE run
                   SET lease_expires_at = LEAST(:target, lease_deadline_at),
                       accepts_steer = COALESCE(CAST(:acceptsSteer AS boolean), accepts_steer),
                       updated_at = :now
                 WHERE run_id = :run AND status = 'active' AND epoch = :epoch
                   AND (CAST(:tenant AS text) IS NULL OR tenant_id = :tenant)
                RETURNING tenant_id, inbox_id, lane, merge_key, lease_expires_at, accepts_steer
                """)
                .param("target", Db.ts(target)).param("acceptsSteer", request.acceptsSteer())
                .param("now", Db.ts(now)).param("run", runId).param("epoch", request.epoch())
                .param("tenant", tenantScope)
                .query((rs, n) -> new Renewed(rs.getString("tenant_id"), rs.getString("inbox_id"),
                        Lane.fromWire(rs.getString("lane")), rs.getString("merge_key"),
                        Db.instant(rs, "lease_expires_at"), rs.getBoolean("accepts_steer")))
                .list();
        if (renewed.isEmpty()) {
            findRun(tenantScope, runId); // 404 when the run does not exist
            throw leaseLost(runId);
        }
        Renewed run = renewed.getFirst();
        RunSignals.Signals waiting = signals.read(run.tenantId(), run.inboxId(), runId, run.lane(), run.mergeKey());
        return new HeartbeatResponse(run.leaseExpiresAt(), run.leaseExpiresAt().equals(target),
                waiting.steerPending(), waiting.interruptRequested(), waiting.interruptEventId());
    }

    // ---- ack ------------------------------------------------------------------------------------

    /**
     * Ends a run. Events up to the cursor reach the outcome; events that joined the run later and were
     * never read by the worker go back to the queue, so a steer that raced with the end of the run
     * cannot lose an event.
     */
    public RunOutcome ack(String tenantScope, String runId, AckRequest request) {
        boolean failed = AckRequest.FAILED.equals(request.outcome());
        if (!failed && !AckRequest.DONE.equals(request.outcome())) {
            throw InboxException.invalid("outcome must be 'done' or 'failed'");
        }
        String finalStatus = failed ? "failed" : "done";
        RunOutcome outcome = withRun(tenantScope, runId, run -> {
            if (run.status() != RunStatus.ACTIVE || run.epoch() != request.epoch()) {
                if (run.epoch() == request.epoch() && run.status().wire().equals(finalStatus)) {
                    return outcome(run, null); // the same ack again: nothing to do
                }
                throw leaseLost(runId);
            }
            Instant now = clock.now();
            String actor = run.workerActor();
            int cursor = request.lastRunSeq() == null ? run.nextRunSeq() - 1 : request.lastRunSeq();
            events.finish(runId, cursor, finalStatus, failed ? "fail" : "ack", actor, request.error(), now);
            events.release(runId, cursor, actor, now);
            jdbc.sql("""
                    UPDATE run
                       SET status = :status, slot_no = NULL, lease_expires_at = NULL, lease_deadline_at = NULL,
                           finished_at = :now, end_reason = :reason, last_error = CAST(:error AS text), updated_at = :now
                     WHERE run_id = :run
                    """)
                    .param("status", finalStatus).param("now", Db.ts(now)).param("reason", failed ? "failed" : "acked")
                    .param("error", request.error()).param("run", runId)
                    .update();
            return new RunOutcome(runId, finalStatus, run.epoch(), run.attempt(), null);
        });
        publishWake();
        return outcome;
    }

    // ---- nack -----------------------------------------------------------------------------------

    /** "Could not process, try again": the same run is issued again after a backoff. */
    public RunOutcome nack(String tenantScope, String runId, NackRequest request) {
        RunOutcome outcome = withRun(tenantScope, runId, run -> {
            if (run.status() != RunStatus.ACTIVE || run.epoch() != request.epoch()) {
                if (run.epoch() == request.epoch()
                        && (run.status() == RunStatus.QUEUED || run.status() == RunStatus.DEAD)) {
                    return outcome(run, run.nextAttemptAt()); // the same nack again
                }
                throw leaseLost(runId);
            }
            Instant now = clock.now();
            Duration delay = request.retryAfterMs() != null
                    ? bounded(Duration.ofMillis(request.retryAfterMs()))
                    : backoff(run.attempt());
            return requeueOrKill(run, "nack", run.workerActor(), request.reason(), now.plus(delay), now);
        });
        publishWake();
        return outcome;
    }

    // ---- steer ----------------------------------------------------------------------------------

    /**
     * Pulls same-key events that arrived after the run started into the run, and returns every event
     * of the run beyond the worker's cursor. Only the lease holder can do this, and only when the run
     * declared that it accepts steering; otherwise the events simply wait for the run to end.
     */
    public SteerResponse steer(String tenantScope, String runId, SteerRequest request) {
        return withRun(tenantScope, runId, run -> {
            if (run.status() != RunStatus.ACTIVE || run.epoch() != request.epoch()) {
                throw leaseLost(runId);
            }
            int after = request.afterRunSeq() == null ? run.nextRunSeq() - 1 : request.afterRunSeq();
            if (run.acceptsSteer() && run.mergeKey() != null) {
                record Waiting(String eventId, int payloadBytes) {
                }
                List<Waiting> waiting = jdbc.sql("SELECT e.event_id, e.payload_bytes" + RunSignals.STEERABLE
                        + " ORDER BY e.seq LIMIT :limit")
                        .param("tenant", run.tenantId()).param("inbox", run.inboxId()).param("key", run.mergeKey())
                        .param("limit", properties.getBatch().getMaxEvents())
                        .query((rs, n) -> new Waiting(rs.getString("event_id"), rs.getInt("payload_bytes")))
                        .list();
                List<String> taken = new ArrayList<>();
                long bytes = 0;
                for (Waiting event : waiting) {
                    if (!taken.isEmpty() && bytes + event.payloadBytes() > properties.getBatch().getMaxBytes()) {
                        break;
                    }
                    taken.add(event.eventId());
                    bytes += event.payloadBytes();
                }
                if (!taken.isEmpty()) {
                    Instant now = clock.now();
                    events.attach(runId, taken, run.nextRunSeq(), "steer", run.attempt(), "steer", run.workerActor(), now);
                    jdbc.sql("UPDATE run SET next_run_seq = next_run_seq + :added, updated_at = :now WHERE run_id = :run")
                            .param("added", taken.size()).param("now", Db.ts(now)).param("run", runId)
                            .update();
                }
            }
            List<ClaimedEvent> beyondCursor = events.claimedEvents(runId, after);
            return new SteerResponse(beyondCursor);
        });
    }

    // ---- suspend --------------------------------------------------------------------------------

    /**
     * The run stops to wait for an answer. It gives up its slot and its lease, so the inbox is free
     * for other work, including new events of the same key. The events it consumed stay bound to it
     * and stay {@code claimed}: they are not finished until the run is. The mandatory deadline is what
     * keeps this from becoming a state an event can rest in forever.
     */
    public RunOutcome suspend(String tenantScope, String runId, SuspendRequest request) {
        if (request.resumeKey() == null || request.resumeKey().isBlank() || request.resumeKey().length() > 256) {
            throw InboxException.invalid("resume_key is required and may be at most 256 characters");
        }
        if (request.timeoutSeconds() == null || request.timeoutSeconds() <= 0) {
            throw InboxException.invalid("timeout_seconds is required and must be positive: "
                    + "a suspended run without a deadline could wait forever");
        }
        String policy = request.onTimeout() == null ? SuspendRequest.FINISH : request.onTimeout();
        if (!SuspendRequest.FINISH.equals(policy) && !SuspendRequest.RESUME.equals(policy)) {
            throw InboxException.invalid("on_timeout must be 'finish' or 'resume'");
        }
        Duration timeout = Duration.ofSeconds(request.timeoutSeconds());
        if (timeout.compareTo(properties.getSuspend().getMaxTimeout()) > 0) {
            timeout = properties.getSuspend().getMaxTimeout();
        }
        Duration boundedTimeout = timeout;

        RunOutcome outcome = withRun(tenantScope, runId, run -> {
            if (run.status() != RunStatus.ACTIVE || run.epoch() != request.epoch()) {
                if (run.status() == RunStatus.SUSPENDED && run.epoch() == request.epoch()
                        && request.resumeKey().equals(run.resumeKey())) {
                    return outcome(run, null); // the same suspend again
                }
                throw leaseLost(runId);
            }
            int clash = jdbc.sql("""
                    SELECT count(*) FROM run
                     WHERE tenant_id = :tenant AND inbox_id = :inbox AND status = 'suspended' AND resume_key = :key
                    """)
                    .param("tenant", run.tenantId()).param("inbox", run.inboxId()).param("key", request.resumeKey())
                    .query(Integer.class).single();
            if (clash > 0) {
                throw InboxException.conflict(ProblemCodes.RESUME_KEY_IN_USE,
                        "another suspended run of this inbox already waits for resume_key '" + request.resumeKey() + "'");
            }
            Instant now = clock.now();
            Instant deadline = now.plus(boundedTimeout);
            int cursor = request.lastRunSeq() == null ? run.nextRunSeq() - 1 : request.lastRunSeq();
            events.noteSuspended(runId, cursor, request.resumeKey(), deadline, run.workerActor(), now);
            events.release(runId, cursor, run.workerActor(), now);
            jdbc.sql("""
                    UPDATE run
                       SET status = 'suspended', slot_no = NULL, lease_expires_at = NULL, lease_deadline_at = NULL,
                           resume_key = :key, suspend_deadline_at = :deadline, on_timeout = :policy,
                           resume_reason = NULL, updated_at = :now
                     WHERE run_id = :run
                    """)
                    .param("key", request.resumeKey()).param("deadline", Db.ts(deadline)).param("policy", policy)
                    .param("now", Db.ts(now)).param("run", runId)
                    .update();
            return new RunOutcome(runId, "suspended", run.epoch(), run.attempt(), null);
        });
        publishWake();
        return outcome;
    }

    /**
     * A suspended run's deadline passed with no answer. With policy {@code finish} the inbox ends it
     * here: its events are done, with the reason on record. With policy {@code resume} the run is
     * handed back to the runtime, which wanted to be told. An answer that arrived before the deadline
     * but has not been claimed yet still wins.
     *
     * @return true when the timeout was applied
     */
    public boolean timeOutSuspended(String runId, int epoch) {
        Boolean applied = withRun(null, runId, run -> {
            Instant now = clock.now();
            if (run.status() != RunStatus.SUSPENDED || run.epoch() != epoch || run.suspendDeadlineAt().isAfter(now)) {
                return false;
            }
            int answered = jdbc.sql("""
                    SELECT count(*) FROM event
                     WHERE tenant_id = :tenant AND inbox_id = :inbox AND status = 'pending' AND run_id IS NULL
                       AND resume_key = :key AND received_at <= :deadline
                    """)
                    .param("tenant", run.tenantId()).param("inbox", run.inboxId()).param("key", run.resumeKey())
                    .param("deadline", Db.ts(run.suspendDeadlineAt()))
                    .query(Integer.class).single();
            if (answered > 0) {
                return false; // the next claim resumes the run with that answer
            }
            String actor = "system:suspend-sweeper";
            if (SuspendRequest.RESUME.equals(run.onTimeout())) {
                events.requeue(runId, "suspend_timeout", actor, now);
                jdbc.sql("""
                        UPDATE run
                           SET status = 'queued', next_attempt_at = :now, queued_reason = 'suspend_timeout',
                               resume_reason = 'timeout', resume_key = NULL, suspend_deadline_at = NULL, updated_at = :now
                         WHERE run_id = :run
                        """)
                        .param("now", Db.ts(now)).param("run", runId)
                        .update();
            } else {
                events.finish(runId, Integer.MAX_VALUE, "done", "suspend_timeout", actor, null, now);
                jdbc.sql("""
                        UPDATE run
                           SET status = 'done', finished_at = :now, end_reason = 'suspend_timeout', resume_key = NULL,
                               suspend_deadline_at = NULL, updated_at = :now
                         WHERE run_id = :run
                        """)
                        .param("now", Db.ts(now)).param("run", runId)
                        .update();
            }
            return true;
        });
        if (Boolean.TRUE.equals(applied)) {
            publishWake();
        }
        return Boolean.TRUE.equals(applied);
    }

    // ---- lease expiry ---------------------------------------------------------------------------

    /**
     * Takes back a run whose lease ran out: its worker is presumed dead. The run is queued for
     * immediate re-issue with the same id and events, and the attempt is charged.
     *
     * @return true when the run was reclaimed (false when it was acked or renewed in the meantime)
     */
    public boolean reclaimExpired(String runId, int epoch) {
        Boolean reclaimed = withRun(null, runId, run -> {
            Instant now = clock.now();
            if (run.status() != RunStatus.ACTIVE || run.epoch() != epoch || run.leaseExpiresAt().isAfter(now)) {
                return false;
            }
            requeueOrKill(run, "lease_expired", "system:lease-sweeper", "lease expired", now, now);
            metrics.leaseReclaimed(run.tenantId(), run.lane());
            return true;
        });
        if (Boolean.TRUE.equals(reclaimed)) {
            publishWake();
        }
        return Boolean.TRUE.equals(reclaimed);
    }

    /**
     * Shared by nack and lease expiry. Below the attempt limit the run is queued and keeps its events
     * (they return to {@code pending} but stay bound to it). At the limit the run and its events are dead.
     */
    private RunOutcome requeueOrKill(LockedRun run, String reason, String actor, String error, Instant nextAttemptAt,
            Instant now) {
        if (run.attempt() >= run.maxAttempts()) {
            jdbc.sql("""
                    WITH moved AS (
                        UPDATE event SET status = 'dead', finished_at = :now, last_error = CAST(:error AS text)
                         WHERE run_id = :run AND status = 'claimed'
                        RETURNING event_id, tenant_id, inbox_id, attempt
                    )
                    INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                                  run_id, attempt, detail, at)
                    SELECT event_id, tenant_id, inbox_id, 'claimed', 'dead', 'attempts_exhausted', :actor, :run, attempt,
                           jsonb_build_object('last_failure', CAST(:reason AS text)), :now
                      FROM moved
                    """)
                    .param("now", Db.ts(now)).param("error", error).param("run", run.runId())
                    .param("actor", actor).param("reason", reason)
                    .update();
            jdbc.sql("""
                    UPDATE run
                       SET status = 'dead', slot_no = NULL, lease_expires_at = NULL, lease_deadline_at = NULL,
                           finished_at = :now, end_reason = 'attempts_exhausted', last_error = CAST(:error AS text),
                           updated_at = :now
                     WHERE run_id = :run
                    """)
                    .param("now", Db.ts(now)).param("error", error).param("run", run.runId())
                    .update();
            log.warn("run {} is dead after {} attempts ({})", run.runId(), run.attempt(), reason);
            return new RunOutcome(run.runId(), "dead", run.epoch(), run.attempt(), null);
        }
        events.requeue(run.runId(), reason, actor, now);
        jdbc.sql("""
                UPDATE run
                   SET status = 'queued', slot_no = NULL, lease_expires_at = NULL, lease_deadline_at = NULL,
                       attempt = attempt + 1, next_attempt_at = :nextAttemptAt, queued_reason = :reason,
                       last_error = CAST(:error AS text), updated_at = :now
                 WHERE run_id = :run
                """)
                .param("nextAttemptAt", Db.ts(nextAttemptAt)).param("reason", reason).param("error", error)
                .param("now", Db.ts(now)).param("run", run.runId())
                .update();
        return new RunOutcome(run.runId(), "queued", run.epoch(), run.attempt() + 1, nextAttemptAt);
    }

    // ---- shared pieces --------------------------------------------------------------------------

    /**
     * Runs {@code action} with the inbox row and then the run row locked, in that order.
     *
     * @param tenantScope when set, a run of another tenant is reported as not found
     */
    <T> T withRun(String tenantScope, String runId, Function<LockedRun, T> action) {
        RunRef ref = findRun(tenantScope, runId);
        return transactions.execute(tx -> {
            jdbc.sql("SELECT 1 FROM inbox WHERE tenant_id = :tenant AND inbox_id = :inbox FOR UPDATE")
                    .param("tenant", ref.tenantId()).param("inbox", ref.inboxId())
                    .query(Integer.class).single();
            LockedRun run = jdbc.sql("""
                    SELECT run_id, tenant_id, inbox_id, lane, merge_key, status, epoch, attempt, max_attempts,
                           next_run_seq, accepts_steer, lease_owner, lease_expires_at, lease_deadline_at,
                           resume_key, suspend_deadline_at, on_timeout, next_attempt_at
                      FROM run
                     WHERE run_id = :run
                       FOR UPDATE
                    """)
                    .param("run", runId)
                    .query((rs, n) -> new LockedRun(rs.getString("run_id"), rs.getString("tenant_id"),
                            rs.getString("inbox_id"), Lane.fromWire(rs.getString("lane")), rs.getString("merge_key"),
                            RunStatus.fromWire(rs.getString("status")), rs.getInt("epoch"), rs.getInt("attempt"),
                            rs.getInt("max_attempts"), rs.getInt("next_run_seq"), rs.getBoolean("accepts_steer"),
                            rs.getString("lease_owner"), Db.instant(rs, "lease_expires_at"),
                            Db.instant(rs, "lease_deadline_at"),
                            rs.getString("resume_key"), Db.instant(rs, "suspend_deadline_at"),
                            rs.getString("on_timeout"), Db.instant(rs, "next_attempt_at")))
                    .single();
            return action.apply(run);
        });
    }

    private RunRef findRun(String tenantScope, String runId) {
        List<RunRef> refs = jdbc.sql("""
                SELECT tenant_id, inbox_id FROM run
                 WHERE run_id = :run AND (CAST(:tenant AS text) IS NULL OR tenant_id = :tenant)
                """)
                .param("run", runId).param("tenant", tenantScope)
                .query((rs, n) -> new RunRef(rs.getString("tenant_id"), rs.getString("inbox_id")))
                .list();
        if (refs.isEmpty()) {
            throw InboxException.notFound("no such run");
        }
        return refs.getFirst();
    }

    /** Exponential backoff after a failed attempt: base^attempt, capped (2 s, 4 s, 8 s, 16 s, ... 5 min). */
    Duration backoff(int failedAttempt) {
        long base = Math.max(1, properties.getRetry().getBackoffBase().toSeconds());
        long cap = properties.getRetry().getBackoffCap().toSeconds();
        double seconds = Math.pow(base, Math.min(failedAttempt, 30));
        return Duration.ofSeconds((long) Math.min(seconds, cap));
    }

    private Duration bounded(Duration requested) {
        if (requested.isNegative()) {
            return Duration.ZERO;
        }
        Duration cap = properties.getRetry().getBackoffCap();
        return requested.compareTo(cap) > 0 ? cap : requested;
    }

    private static RunOutcome outcome(LockedRun run, Instant nextAttemptAt) {
        return new RunOutcome(run.runId(), run.status().wire(), run.epoch(), run.attempt(), nextAttemptAt);
    }

    static InboxException leaseLost(String runId) {
        return InboxException.conflict(ProblemCodes.LEASE_LOST,
                "run " + runId + " is no longer held under this epoch; stop working on it");
    }

    void publishWake() {
        try {
            wakeBus.publish();
        } catch (RuntimeException e) {
            log.warn("wake hint could not be published", e);
        }
    }
}
