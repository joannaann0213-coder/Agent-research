package dev.agentflow.inbox.server.query;

import dev.agentflow.inbox.api.EventView;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.db.Db;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Read side: a run and the events bound to it. */
@Service
public class RunQueries {

    private final JdbcClient jdbc;
    private final EventQueries events;

    public RunQueries(JdbcClient jdbc, EventQueries events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    public RunView view(String tenantScope, String runId) {
        List<EventView> bound = jdbc.sql("SELECT " + EventQueries.EVENT_COLUMNS + """
                  FROM event
                 WHERE run_id = :run
                 ORDER BY run_seq
                """)
                .param("run", runId)
                .query((rs, n) -> events.mapEvent(rs))
                .list();
        List<RunView> runs = jdbc.sql("""
                SELECT run_id, tenant_id, inbox_id, lane, merge_key, status, slot_no, epoch, attempt, max_attempts,
                       lease_owner, lease_expires_at, lease_deadline_at, next_attempt_at, queued_reason, accepts_steer,
                       resume_key, suspend_deadline_at, on_timeout, resume_reason, trace_id, end_reason, last_error,
                       created_at, finished_at
                  FROM run
                 WHERE run_id = :run AND (CAST(:tenant AS text) IS NULL OR tenant_id = :tenant)
                """)
                .param("run", runId).param("tenant", tenantScope)
                .query((rs, n) -> new RunView(rs.getString("run_id"), rs.getString("tenant_id"),
                        rs.getString("inbox_id"), Lane.fromWire(rs.getString("lane")), rs.getString("merge_key"),
                        rs.getString("status"), Db.intOrNull(rs, "slot_no"), rs.getInt("epoch"), rs.getInt("attempt"),
                        rs.getInt("max_attempts"), rs.getString("lease_owner"), Db.instant(rs, "lease_expires_at"),
                        Db.instant(rs, "lease_deadline_at"), Db.instant(rs, "next_attempt_at"),
                        rs.getString("queued_reason"), rs.getBoolean("accepts_steer"), rs.getString("resume_key"),
                        Db.instant(rs, "suspend_deadline_at"), rs.getString("on_timeout"),
                        rs.getString("resume_reason"), rs.getString("trace_id"), rs.getString("end_reason"),
                        rs.getString("last_error"), Db.instant(rs, "created_at"), Db.instant(rs, "finished_at"), bound))
                .list();
        if (runs.isEmpty()) {
            throw InboxException.notFound("no such run");
        }
        return runs.getFirst();
    }
}
