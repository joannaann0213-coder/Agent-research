package dev.agentflow.inbox.server.sweep;

import dev.agentflow.inbox.server.claim.RunLifecycleService;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.time.InboxClock;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Finds runs whose lease ran out and takes them back. This is one of the two paths that guarantee no
 * event stays stuck in the middle of the state machine (the other is the attempt limit).
 *
 * <p>The scan takes no locks; each reclaim re-checks the run under the inbox lock, so several
 * instances can run this loop at once.
 */
@Component
public class LeaseSweeper {

    private record Expired(String runId, int epoch) {
    }

    private final JdbcClient jdbc;
    private final InboxClock clock;
    private final InboxProperties properties;
    private final RunLifecycleService lifecycle;

    public LeaseSweeper(JdbcClient jdbc, InboxClock clock, InboxProperties properties, RunLifecycleService lifecycle) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.properties = properties;
        this.lifecycle = lifecycle;
    }

    /** One pass. Returns how many runs were reclaimed. */
    public int runOnce() {
        List<Expired> expired = jdbc.sql("""
                SELECT run_id, epoch
                  FROM run
                 WHERE status = 'active' AND lease_expires_at <= :now
                 ORDER BY lease_expires_at
                 LIMIT :limit
                """)
                .param("now", Db.ts(clock.now()))
                .param("limit", properties.getScheduling().getBatchSize())
                .query((rs, n) -> new Expired(rs.getString("run_id"), rs.getInt("epoch")))
                .list();
        int reclaimed = 0;
        for (Expired run : expired) {
            if (lifecycle.reclaimExpired(run.runId(), run.epoch())) {
                reclaimed++;
            }
        }
        return reclaimed;
    }
}
