package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.RunStatus;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.metrics.InboxMetrics;
import dev.agentflow.inbox.server.time.InboxClock;
import dev.agentflow.inbox.server.wake.WakeBus;
import java.time.Duration;
import java.time.Instant;
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

    public RunLifecycleService(JdbcClient jdbc, TransactionTemplate transactions, InboxClock clock,
            InboxProperties properties, WakeBus wakeBus, InboxMetrics metrics) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.properties = properties;
        this.wakeBus = wakeBus;
        this.metrics = metrics;
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
        RunSignals.Signals signals = RunSignals.none();
        return new HeartbeatResponse(run.leaseExpiresAt(), run.leaseExpiresAt().equals(target),
                signals.steerPending(), signals.interruptRequested(), signals.interruptEventId());
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
            finishEvents(runId, cursor, finalStatus, failed ? "fail" : "ack", actor, now, request.error());
            releaseEvents(runId, cursor, actor, now);
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
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = 'pending'
                     WHERE run_id = :run AND status = 'claimed'
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'claimed', 'pending', :reason, :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("run", run.runId()).param("reason", reason).param("actor", actor).param("now", Db.ts(now))
                .update();
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

    /** Claimed events up to the cursor reach a final state. */
    void finishEvents(String runId, int cursor, String toStatus, String reason, String actor, Instant now, String error) {
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = :to, finished_at = :now, last_error = CAST(:error AS text)
                     WHERE run_id = :run AND status = 'claimed' AND run_seq <= :cursor
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'claimed', :to, :reason, :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("to", toStatus).param("now", Db.ts(now)).param("error", error).param("run", runId)
                .param("cursor", cursor).param("reason", reason).param("actor", actor)
                .update();
    }

    /** Claimed events beyond the cursor were never consumed: they leave the run and wait again. */
    void releaseEvents(String runId, int cursor, String actor, Instant now) {
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = 'pending', run_id = NULL, run_seq = NULL, delivery_mode = NULL
                     WHERE run_id = :run AND status = 'claimed' AND run_seq > :cursor
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'claimed', 'pending', 'released', :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("run", runId).param("cursor", cursor).param("actor", actor).param("now", Db.ts(now))
                .update();
    }

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
