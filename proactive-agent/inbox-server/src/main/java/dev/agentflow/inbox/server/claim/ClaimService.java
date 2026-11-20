package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.RunStatus;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.core.IdGenerator;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.db.Locks;
import dev.agentflow.inbox.server.metrics.InboxMetrics;
import dev.agentflow.inbox.server.time.InboxClock;
import dev.agentflow.inbox.server.wake.WakeBus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns ready events into a run: the only way events leave the inbox.
 *
 * <p>One claim attempt on one inbox is one short transaction. It locks the inbox row first, which
 * serialises it against every other claim and every change of delivery state in that inbox; then it
 * reads the open runs and the unclaimed events, lets the {@link ClaimPlanner} decide, and applies the
 * decision with guarded statements. There is no model call anywhere on this path.
 */
@Service
public class ClaimService {

    /** Result of an attempt: a run, or why there is none and when to look again. */
    public record Attempt(ClaimedRun run, ClaimPlan.Reason reason, Instant retryAt) {
        static Attempt nothing(ClaimPlan.Reason reason, Instant retryAt) {
            return new Attempt(null, reason, retryAt);
        }
    }

    private record InboxRow(Integer capInteractive, Integer capBackground, Integer capBatch, Integer maxActiveRuns,
            String settings) {
    }

    private record Granted(Attempt attempt, Lane lane, Instant eligibleAt, Instant headReceivedAt, int batchSize) {
        static Granted nothing(ClaimPlan.Reason reason, Instant retryAt) {
            return new Granted(Attempt.nothing(reason, retryAt), null, null, null, 0);
        }
    }

    private record RunGrant(int epoch, int attempt, String mergeKey, String traceId, String queuedReason,
            String resumeReason, int firstSeq) {
    }

    private record Candidate(String tenantId, String inboxId) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final InboxClock clock;
    private final InboxProperties properties;
    private final InboxLimitsResolver limitsResolver;
    private final IdGenerator ids;
    private final WakeBus wakeBus;
    private final InboxMetrics metrics;
    private final JsonMapper json;
    private final ClaimPlanner planner = new ClaimPlanner();

    public ClaimService(JdbcClient jdbc, TransactionTemplate transactions, InboxClock clock, InboxProperties properties,
            InboxLimitsResolver limitsResolver, IdGenerator ids, WakeBus wakeBus, InboxMetrics metrics, JsonMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.properties = properties;
        this.limitsResolver = limitsResolver;
        this.ids = ids;
        this.wakeBus = wakeBus;
        this.metrics = metrics;
        this.json = json;
    }

    /**
     * Claims one run, waiting up to {@code request.waitMs} for work. While waiting, no database
     * connection is held; the wait ends on a wake hint, on the poll interval, or when a quiet window
     * or backoff is due to end.
     *
     * @param tenantScope when set, only this tenant's inboxes are considered
     */
    public Attempt claim(String tenantScope, ClaimRequest request) {
        if (request.workerId() == null || request.workerId().isBlank()) {
            throw InboxException.invalid("worker_id is required");
        }
        if (request.inboxId() != null && tenantScope == null) {
            throw InboxException.invalid("tenant_id is required when inbox_id is given");
        }
        long waitMs = request.waitMs() == null ? 0 : Math.max(0, request.waitMs());
        waitMs = Math.min(waitMs, properties.getClaim().getMaxWait().toMillis());
        long deadline = System.nanoTime() + waitMs * 1_000_000L;

        while (true) {
            long generation = wakeBus.generation();
            Attempt attempt = request.inboxId() != null
                    ? claimInbox(tenantScope, request.inboxId(), request, false)
                    : discover(tenantScope, request);
            if (attempt.run() != null) {
                return attempt;
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return attempt;
            }
            Duration pause = Duration.ofNanos(Math.min(remaining, properties.getClaim().getPollInterval().toNanos()));
            if (attempt.retryAt() != null) {
                Duration untilDue = Duration.between(clock.now(), attempt.retryAt());
                if (untilDue.compareTo(Duration.ofMillis(1)) < 0) {
                    untilDue = Duration.ofMillis(1);
                }
                if (untilDue.compareTo(pause) < 0) {
                    pause = untilDue;
                }
            }
            try {
                wakeBus.await(generation, pause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return attempt;
            }
        }
    }

    /** Looks for an inbox with claimable work and claims from the first one that yields a run. */
    private Attempt discover(String tenantScope, ClaimRequest request) {
        int pageSize = properties.getClaim().getDiscoveryCandidates();
        ClaimPlan.Reason reason = ClaimPlan.Reason.EMPTY;
        Instant retryAt = null;
        for (int page = 0; page < 8; page++) {
            List<Candidate> candidates = candidates(tenantScope, pageSize, page * pageSize);
            for (Candidate candidate : candidates) {
                Attempt attempt = claimInbox(candidate.tenantId(), candidate.inboxId(), request, true);
                if (attempt.run() != null) {
                    return attempt;
                }
                if (attempt.retryAt() != null && (retryAt == null || attempt.retryAt().isBefore(retryAt))) {
                    retryAt = attempt.retryAt();
                    reason = attempt.reason();
                } else if (retryAt == null && attempt.reason() != ClaimPlan.Reason.EMPTY) {
                    reason = attempt.reason();
                }
            }
            if (candidates.size() < pageSize) {
                break;
            }
        }
        return Attempt.nothing(reason, retryAt);
    }

    /**
     * Inboxes that have unclaimed events or a queued run that is due, oldest work first. This scans
     * the unclaimed events, which is fine at this stage; sharding (roadmap M2) replaces it.
     */
    private List<Candidate> candidates(String tenantScope, int limit, int offset) {
        return jdbc.sql("""
                SELECT tenant_id, inbox_id
                  FROM (
                        SELECT e.tenant_id, e.inbox_id, min(e.received_at) AS oldest
                          FROM event e
                         WHERE e.status = 'pending' AND e.run_id IS NULL
                           AND (CAST(:tenant AS text) IS NULL OR e.tenant_id = :tenant)
                         GROUP BY e.tenant_id, e.inbox_id
                        UNION ALL
                        SELECT r.tenant_id, r.inbox_id, r.next_attempt_at
                          FROM run r
                         WHERE r.status = 'queued' AND r.next_attempt_at <= :now
                           AND (CAST(:tenant AS text) IS NULL OR r.tenant_id = :tenant)
                       ) work
                 GROUP BY tenant_id, inbox_id
                 ORDER BY min(oldest), tenant_id, inbox_id
                 LIMIT :limit OFFSET :offset
                """)
                .param("tenant", tenantScope)
                .param("now", Db.ts(clock.now()))
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, n) -> new Candidate(rs.getString("tenant_id"), rs.getString("inbox_id")))
                .list();
    }

    /** One claim attempt on one inbox, in one transaction. */
    Attempt claimInbox(String tenantId, String inboxId, ClaimRequest request, boolean skipLocked) {
        Granted granted = transactions.execute(tx -> {
            InboxRow inbox;
            try {
                if (!skipLocked) {
                    // A writer holds this row for one statement; wait briefly rather than answer "busy".
                    jdbc.sql("SET LOCAL lock_timeout = '" + properties.getClaim().getLockTimeout().toMillis() + "ms'")
                            .update();
                }
                inbox = lockInbox(tenantId, inboxId, skipLocked);
            } catch (DataAccessException e) {
                if (Locks.isLockTimeout(e)) {
                    tx.setRollbackOnly();
                    return Granted.nothing(ClaimPlan.Reason.BUSY, null);
                }
                throw e;
            }
            if (inbox == null) {
                // With SKIP LOCKED "no row" means someone else is working on this inbox right now.
                return Granted.nothing(skipLocked ? ClaimPlan.Reason.BUSY : ClaimPlan.Reason.EMPTY, null);
            }
            // Read the clock only now: time spent waiting for the lock must not count as "not yet due".
            Instant now = clock.now();
            InboxLimits limits = limitsResolver.resolve(inbox.capInteractive(), inbox.capBackground(),
                    inbox.capBatch(), inbox.maxActiveRuns(), inbox.settings());
            List<OpenRun> runs = openRuns(tenantId, inboxId);
            List<FreshEvent> fresh = freshEvents(tenantId, inboxId);

            ClaimPlan plan = planner.plan(now, limits, runs, fresh, request.lanes());
            return switch (plan) {
                case ClaimPlan.Start start -> start(tenantId, inboxId, start, request, now);
                case ClaimPlan.Reissue reissue -> reissue(tenantId, inboxId, reissue, request, now);
                case ClaimPlan.Resume resume -> resume(tenantId, inboxId, resume, request, now);
                case ClaimPlan.Nothing nothing -> Granted.nothing(nothing.reason(), nothing.retryAt());
            };
        });
        if (granted.attempt().run() != null && granted.eligibleAt() != null) {
            Instant claimedAt = clock.now();
            metrics.claimLatency(tenantId, granted.lane(), Duration.between(granted.eligibleAt(), claimedAt),
                    Duration.between(granted.headReceivedAt(), claimedAt));
            metrics.batchSize(tenantId, granted.lane(), granted.batchSize());
        }
        return granted.attempt();
    }

    private InboxRow lockInbox(String tenantId, String inboxId, boolean skipLocked) {
        List<InboxRow> rows = jdbc.sql("""
                SELECT cap_interactive, cap_background, cap_batch, max_active_runs, settings
                  FROM inbox
                 WHERE tenant_id = :tenant AND inbox_id = :inbox
                   FOR UPDATE""" + (skipLocked ? " SKIP LOCKED" : ""))
                .param("tenant", tenantId).param("inbox", inboxId)
                .query((rs, n) -> new InboxRow(Db.intOrNull(rs, "cap_interactive"), Db.intOrNull(rs, "cap_background"),
                        Db.intOrNull(rs, "cap_batch"), Db.intOrNull(rs, "max_active_runs"), rs.getString("settings")))
                .list();
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private List<OpenRun> openRuns(String tenantId, String inboxId) {
        return jdbc.sql("""
                SELECT run_id, lane, merge_key, status, head_seq, slot_no, next_attempt_at, resume_key
                  FROM run
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND status IN ('active', 'queued', 'suspended')
                """)
                .param("tenant", tenantId).param("inbox", inboxId)
                .query((rs, n) -> new OpenRun(rs.getString("run_id"), Lane.fromWire(rs.getString("lane")),
                        rs.getString("merge_key"), RunStatus.fromWire(rs.getString("status")), rs.getLong("head_seq"),
                        Db.intOrNull(rs, "slot_no"), Db.instant(rs, "next_attempt_at"), rs.getString("resume_key")))
                .list();
    }

    private List<FreshEvent> freshEvents(String tenantId, String inboxId) {
        return jdbc.sql("""
                SELECT event_id, seq, lane, merge_key, resume_key, interrupt, payload_bytes, received_at
                  FROM event
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND status = 'pending' AND run_id IS NULL
                 ORDER BY seq
                 LIMIT :limit
                """)
                .param("tenant", tenantId).param("inbox", inboxId)
                .param("limit", properties.getClaim().getPlanningWindow())
                .query((rs, n) -> new FreshEvent(rs.getString("event_id"), rs.getLong("seq"),
                        Lane.fromWire(rs.getString("lane")), rs.getString("merge_key"), rs.getString("resume_key"),
                        rs.getBoolean("interrupt"), rs.getInt("payload_bytes"), Db.instant(rs, "received_at")))
                .list();
    }

    private Granted start(String tenantId, String inboxId, ClaimPlan.Start plan, ClaimRequest request, Instant now) {
        String runId = ids.newId();
        FreshEvent head = plan.events().getFirst();
        boolean acceptsSteer = Boolean.TRUE.equals(request.acceptsSteer());
        Instant leaseExpires = now.plus(properties.getLease().getDuration());
        Instant leaseDeadline = now.plus(properties.getLease().getMaxDuration());

        String traceId = jdbc.sql("""
                INSERT INTO run (run_id, tenant_id, inbox_id, lane, merge_key, head_seq, status, slot_no, epoch, attempt,
                                 max_attempts, lease_owner, lease_started_at, lease_expires_at, lease_deadline_at,
                                 accepts_steer, next_run_seq, trace_id, eligible_at, created_at, updated_at)
                SELECT :run, :tenant, :inbox, :lane, CAST(:mergeKey AS text), :headSeq, 'active', :slot, 1, 1,
                       :maxAttempts, :owner, :now, :leaseExpires, :leaseDeadline,
                       :acceptsSteer, :nextRunSeq, e.trace_id, :eligibleAt, :now, :now
                  FROM event e
                 WHERE e.event_id = :headId
                RETURNING trace_id
                """)
                .param("run", runId).param("tenant", tenantId).param("inbox", inboxId)
                .param("lane", plan.lane().wire()).param("mergeKey", plan.mergeKey()).param("headSeq", head.seq())
                .param("slot", plan.slotNo()).param("maxAttempts", properties.getRetry().getMaxAttempts())
                .param("owner", request.workerId()).param("now", Db.ts(now))
                .param("leaseExpires", Db.ts(leaseExpires)).param("leaseDeadline", Db.ts(leaseDeadline))
                .param("acceptsSteer", acceptsSteer).param("nextRunSeq", plan.events().size() + 1)
                .param("eligibleAt", Db.ts(plan.eligibleAt())).param("headId", head.eventId())
                .query(String.class).single();

        attach(runId, plan.events(), 1, "batch", 1, now);
        recordClaimed(runId, 1, "claim", request.workerId(), now);

        ClaimedRun run = new ClaimedRun(runId, 1, 1, ClaimedRun.KIND_START, null, tenantId, inboxId, plan.lane(),
                plan.mergeKey(), traceId, acceptsSteer, leaseExpires, leaseDeadline, claimedEvents(runId, 1));
        return new Granted(new Attempt(run, null, null), plan.lane(), plan.eligibleAt(), head.receivedAt(),
                plan.events().size());
    }

    private Granted reissue(String tenantId, String inboxId, ClaimPlan.Reissue plan, ClaimRequest request, Instant now) {
        boolean acceptsSteer = Boolean.TRUE.equals(request.acceptsSteer());
        Instant leaseExpires = now.plus(properties.getLease().getDuration());
        Instant leaseDeadline = now.plus(properties.getLease().getMaxDuration());

        RunGrant grant = jdbc.sql("""
                UPDATE run
                   SET status = 'active', epoch = epoch + 1, slot_no = :slot, lease_owner = :owner,
                       lease_started_at = :now, lease_expires_at = :leaseExpires, lease_deadline_at = :leaseDeadline,
                       next_attempt_at = NULL, accepts_steer = :acceptsSteer, updated_at = :now
                 WHERE run_id = :run AND status = 'queued'
                RETURNING epoch, attempt, merge_key, trace_id, queued_reason, resume_reason
                """)
                .param("run", plan.runId()).param("slot", plan.slotNo()).param("owner", request.workerId())
                .param("now", Db.ts(now)).param("leaseExpires", Db.ts(leaseExpires))
                .param("leaseDeadline", Db.ts(leaseDeadline)).param("acceptsSteer", acceptsSteer)
                .query((rs, n) -> new RunGrant(rs.getInt("epoch"), rs.getInt("attempt"), rs.getString("merge_key"),
                        rs.getString("trace_id"), rs.getString("queued_reason"), rs.getString("resume_reason"), 1))
                .single();

        // The same events as before: this is what keeps run_id + sequence a stable dedupe key downstream.
        jdbc.sql("""
                WITH moved AS (
                    UPDATE event SET status = 'claimed', attempt = :attempt
                     WHERE run_id = :run AND status = 'pending'
                    RETURNING event_id, tenant_id, inbox_id, attempt
                )
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'pending', 'claimed', 'redelivery', :actor, :run, attempt, :now
                  FROM moved
                """)
                .param("run", plan.runId()).param("attempt", grant.attempt())
                .param("actor", "worker:" + request.workerId()).param("now", Db.ts(now))
                .update();

        // A run whose suspend timed out and asked to be told continues as a resume, not as a retry.
        boolean timedOutSuspend = "suspend_timeout".equals(grant.queuedReason());
        ClaimedRun run = new ClaimedRun(plan.runId(), grant.epoch(), grant.attempt(),
                timedOutSuspend ? ClaimedRun.KIND_RESUME : ClaimedRun.KIND_REDELIVERY, grant.resumeReason(), tenantId,
                inboxId, plan.lane(), grant.mergeKey(), grant.traceId(), acceptsSteer, leaseExpires, leaseDeadline,
                claimedEvents(plan.runId(), 1));
        return new Granted(new Attempt(run, null, null), plan.lane(), null, null, run.events().size());
    }

    private Granted resume(String tenantId, String inboxId, ClaimPlan.Resume plan, ClaimRequest request, Instant now) {
        boolean acceptsSteer = Boolean.TRUE.equals(request.acceptsSteer());
        Instant leaseExpires = now.plus(properties.getLease().getDuration());
        Instant leaseDeadline = now.plus(properties.getLease().getMaxDuration());
        int added = plan.events().size();

        RunGrant grant = jdbc.sql("""
                UPDATE run
                   SET status = 'active', epoch = epoch + 1, slot_no = :slot, lease_owner = :owner,
                       lease_started_at = :now, lease_expires_at = :leaseExpires, lease_deadline_at = :leaseDeadline,
                       resume_key = NULL, suspend_deadline_at = NULL, resume_reason = 'event',
                       accepts_steer = :acceptsSteer, next_run_seq = next_run_seq + :added, updated_at = :now
                 WHERE run_id = :run AND status = 'suspended'
                RETURNING epoch, attempt, merge_key, trace_id, queued_reason, resume_reason, next_run_seq - :added AS first_seq
                """)
                .param("run", plan.runId()).param("slot", plan.slotNo()).param("owner", request.workerId())
                .param("now", Db.ts(now)).param("leaseExpires", Db.ts(leaseExpires))
                .param("leaseDeadline", Db.ts(leaseDeadline)).param("acceptsSteer", acceptsSteer).param("added", added)
                .query((rs, n) -> new RunGrant(rs.getInt("epoch"), rs.getInt("attempt"), rs.getString("merge_key"),
                        rs.getString("trace_id"), rs.getString("queued_reason"), rs.getString("resume_reason"),
                        rs.getInt("first_seq")))
                .single();

        attach(plan.runId(), plan.events(), grant.firstSeq(), "resume", grant.attempt(), now);
        recordClaimed(plan.runId(), grant.firstSeq(), "resume", request.workerId(), now);

        ClaimedRun run = new ClaimedRun(plan.runId(), grant.epoch(), grant.attempt(), ClaimedRun.KIND_RESUME, "event",
                tenantId, inboxId, plan.lane(), grant.mergeKey(), grant.traceId(), acceptsSteer, leaseExpires,
                leaseDeadline, claimedEvents(plan.runId(), grant.firstSeq()));
        return new Granted(new Attempt(run, null, null), plan.lane(), null, null, added);
    }

    /** Binds unclaimed events to a run, numbering them from {@code firstSeq} in the order given. */
    void attach(String runId, List<FreshEvent> events, int firstSeq, String mode, int attempt, Instant now) {
        String idList = events.stream().map(FreshEvent::eventId).collect(Collectors.joining(","));
        int updated = jdbc.sql("""
                UPDATE event e
                   SET status = 'claimed', run_id = :run, run_seq = CAST(:firstSeq + t.ord - 1 AS int),
                       delivery_mode = :mode, attempt = :attempt,
                       first_claimed_at = COALESCE(e.first_claimed_at, :now)
                  FROM unnest(string_to_array(:ids, ',')) WITH ORDINALITY AS t(id, ord)
                 WHERE e.event_id = t.id AND e.status = 'pending' AND e.run_id IS NULL
                """)
                .param("run", runId).param("firstSeq", firstSeq).param("mode", mode).param("attempt", attempt)
                .param("now", Db.ts(now)).param("ids", idList)
                .update();
        if (updated != events.size()) {
            // Cannot happen while the inbox lock is held; if it does, roll back rather than lose an event.
            throw new IllegalStateException("claim planned " + events.size() + " events but bound " + updated);
        }
    }

    void recordClaimed(String runId, int fromRunSeq, String reason, String workerId, Instant now) {
        jdbc.sql("""
                INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason, actor,
                                              run_id, attempt, at)
                SELECT event_id, tenant_id, inbox_id, 'pending', 'claimed', :reason, :actor, run_id, attempt, :now
                  FROM event
                 WHERE run_id = :run AND status = 'claimed' AND run_seq >= :fromSeq
                 ORDER BY run_seq
                """)
                .param("run", runId).param("fromSeq", fromRunSeq).param("reason", reason)
                .param("actor", "worker:" + workerId).param("now", Db.ts(now))
                .update();
    }

    /** The run's claimed events from {@code fromRunSeq} on, in run order, with their payloads. */
    public List<ClaimedEvent> claimedEvents(String runId, int fromRunSeq) {
        return jdbc.sql("""
                SELECT event_id, seq, run_seq, delivery_mode, source, type, source_event_id, occurred_at, received_at,
                       merge_key, resume_key, interrupt, payload, trace_id
                  FROM event
                 WHERE run_id = :run AND status = 'claimed' AND run_seq >= :fromSeq
                 ORDER BY run_seq
                """)
                .param("run", runId).param("fromSeq", fromRunSeq)
                .query((rs, n) -> new ClaimedEvent(rs.getString("event_id"), rs.getLong("seq"), rs.getInt("run_seq"),
                        rs.getString("delivery_mode"), EventSource.fromWire(rs.getString("source")),
                        rs.getString("type"), rs.getString("source_event_id"), Db.instant(rs, "occurred_at"),
                        Db.instant(rs, "received_at"), rs.getString("merge_key"), rs.getString("resume_key"),
                        rs.getBoolean("interrupt"), json.readTree(rs.getString("payload")), rs.getString("trace_id")))
                .list();
    }
}
