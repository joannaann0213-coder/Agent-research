package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.server.db.Db;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * What a running run should know about events that arrived after it started. This is the mechanical
 * half of the four delivery modes: the inbox reports that something is waiting; whether to take it in,
 * or to stop for it, is the agent's decision.
 *
 * <p>Signals are derived from the tables every time they are asked for. Nothing is stored, so an
 * ignored signal has no side effect: the waiting event is simply claimed after the run ends.
 */
@Component
public class RunSignals {

    public record Signals(int steerPending, boolean interruptRequested, String interruptEventId) {
    }

    private record InterruptCandidate(String eventId, String mergeKey, Lane lane) {
    }

    /** Unclaimed events of the run's merge key that are not reserved as the answer to a suspended run. */
    static final String STEERABLE = """
              FROM event e
             WHERE e.tenant_id = :tenant AND e.inbox_id = :inbox AND e.status = 'pending' AND e.run_id IS NULL
               AND e.merge_key = :key
               AND NOT (e.resume_key IS NOT NULL AND EXISTS (
                       SELECT 1 FROM run s
                        WHERE s.tenant_id = e.tenant_id AND s.inbox_id = e.inbox_id
                          AND s.status = 'suspended' AND s.resume_key = e.resume_key))
            """;

    private final JdbcClient jdbc;
    private final InboxLimitsResolver limitsResolver;

    public RunSignals(JdbcClient jdbc, InboxLimitsResolver limitsResolver) {
        this.jdbc = jdbc;
        this.limitsResolver = limitsResolver;
    }

    public Signals read(String tenantId, String inboxId, String runId, Lane lane, String mergeKey) {
        int steerPending = mergeKey == null ? 0 : jdbc.sql("SELECT count(*)" + STEERABLE)
                .param("tenant", tenantId).param("inbox", inboxId).param("key", mergeKey)
                .query(Integer.class).single();

        List<InterruptCandidate> flagged = jdbc.sql("""
                SELECT e.event_id, e.merge_key, e.lane
                  FROM event e
                 WHERE e.tenant_id = :tenant AND e.inbox_id = :inbox AND e.status = 'pending' AND e.run_id IS NULL
                   AND e.interrupt
                 ORDER BY e.seq
                 LIMIT 20
                """)
                .param("tenant", tenantId).param("inbox", inboxId)
                .query((rs, n) -> new InterruptCandidate(rs.getString("event_id"), rs.getString("merge_key"),
                        Lane.fromWire(rs.getString("lane"))))
                .list();
        for (InterruptCandidate candidate : flagged) {
            // An interrupt for this run's own key is always meant for this run.
            if (mergeKey != null && mergeKey.equals(candidate.mergeKey())) {
                return new Signals(steerPending, true, candidate.eventId());
            }
        }
        for (InterruptCandidate candidate : flagged) {
            // An interrupt for a key nobody is working on, stuck behind a full lane: the youngest run
            // of that lane is the one asked to make room.
            if (candidate.lane() == lane && !keyIsActive(tenantId, inboxId, candidate.mergeKey())
                    && isYoungestOfFullLane(tenantId, inboxId, runId, lane)) {
                return new Signals(steerPending, true, candidate.eventId());
            }
        }
        return new Signals(steerPending, false, null);
    }

    private boolean keyIsActive(String tenantId, String inboxId, String mergeKey) {
        if (mergeKey == null) {
            return false;
        }
        return jdbc.sql("""
                SELECT count(*) FROM run
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND status = 'active' AND merge_key = :key
                """)
                .param("tenant", tenantId).param("inbox", inboxId).param("key", mergeKey)
                .query(Integer.class).single() > 0;
    }

    private boolean isYoungestOfFullLane(String tenantId, String inboxId, String runId, Lane lane) {
        List<String> activeNewestFirst = jdbc.sql("""
                SELECT run_id FROM run
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND lane = :lane AND status = 'active'
                 ORDER BY lease_started_at DESC, run_id DESC
                """)
                .param("tenant", tenantId).param("inbox", inboxId).param("lane", lane.wire())
                .query(String.class).list();
        if (activeNewestFirst.isEmpty() || !activeNewestFirst.getFirst().equals(runId)) {
            return false;
        }
        InboxLimits limits = jdbc.sql("""
                SELECT cap_interactive, cap_background, cap_batch, max_active_runs, settings
                  FROM inbox WHERE tenant_id = :tenant AND inbox_id = :inbox
                """)
                .param("tenant", tenantId).param("inbox", inboxId)
                .query((rs, n) -> limitsResolver.resolve(Db.intOrNull(rs, "cap_interactive"),
                        Db.intOrNull(rs, "cap_background"), Db.intOrNull(rs, "cap_batch"),
                        Db.intOrNull(rs, "max_active_runs"), rs.getString("settings")))
                .single();
        return activeNewestFirst.size() >= limits.cap(lane);
    }
}
