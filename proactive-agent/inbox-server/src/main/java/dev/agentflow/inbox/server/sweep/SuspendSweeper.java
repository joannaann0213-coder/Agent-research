package dev.agentflow.inbox.server.sweep;

import dev.agentflow.inbox.server.claim.RunLifecycleService;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.time.InboxClock;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Applies the deadline of suspended runs. Without it a run that waits for an approval nobody gives
 * would wait forever. The scan takes no locks; each timeout re-checks the run under the inbox lock.
 */
@Component
public class SuspendSweeper {

    private record Overdue(String runId, int epoch) {
    }

    private final JdbcClient jdbc;
    private final InboxClock clock;
    private final InboxProperties properties;
    private final RunLifecycleService lifecycle;

    public SuspendSweeper(JdbcClient jdbc, InboxClock clock, InboxProperties properties, RunLifecycleService lifecycle) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.properties = properties;
        this.lifecycle = lifecycle;
    }

    /** One pass. Returns how many suspended runs were timed out. */
    public int runOnce() {
        List<Overdue> overdue = jdbc.sql("""
                SELECT run_id, epoch
                  FROM run
                 WHERE status = 'suspended' AND suspend_deadline_at <= :now
                 ORDER BY suspend_deadline_at
                 LIMIT :limit
                """)
                .param("now", Db.ts(clock.now()))
                .param("limit", properties.getScheduling().getBatchSize())
                .query((rs, n) -> new Overdue(rs.getString("run_id"), rs.getInt("epoch")))
                .list();
        int timedOut = 0;
        for (Overdue run : overdue) {
            if (lifecycle.timeOutSuspended(run.runId(), run.epoch())) {
                timedOut++;
            }
        }
        return timedOut;
    }
}
