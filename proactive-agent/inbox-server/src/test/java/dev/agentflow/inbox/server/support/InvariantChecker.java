package dev.agentflow.inbox.server.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Structural invariants of the tables. Checked after every database test, whatever the test was about,
 * so that a bug in one code path cannot leave rows in a shape another path does not expect.
 */
public final class InvariantChecker {

    private static Long deadlocksAtStart;

    private InvariantChecker() {
    }

    public static void assertAll(JdbcClient jdbc) {
        assertThat(count(jdbc, """
                SELECT count(*) FROM event e LEFT JOIN run r ON r.run_id = e.run_id
                 WHERE e.status = 'claimed' AND (r.run_id IS NULL OR r.status NOT IN ('active', 'suspended'))
                """)).as("a claimed event belongs to an active or suspended run").isZero();

        assertThat(count(jdbc, """
                SELECT count(*) FROM event e LEFT JOIN run r ON r.run_id = e.run_id
                 WHERE e.status = 'pending' AND e.run_id IS NOT NULL AND (r.run_id IS NULL OR r.status <> 'queued')
                """)).as("a pending event that is bound to a run belongs to a queued run").isZero();

        assertThat(count(jdbc, """
                SELECT count(*) FROM event
                 WHERE (status IN ('done', 'failed', 'dead')) <> (finished_at IS NOT NULL)
                """)).as("exactly the finished events carry finished_at").isZero();

        assertThat(count(jdbc, """
                SELECT count(*) FROM (
                    SELECT from_status, lag(to_status) OVER (PARTITION BY event_id ORDER BY transition_id) AS previous,
                           row_number() OVER (PARTITION BY event_id ORDER BY transition_id) AS n
                      FROM event_transition) t
                 WHERE (n = 1 AND from_status IS NOT NULL) OR (n > 1 AND from_status IS DISTINCT FROM previous)
                """)).as("every event's history is an unbroken chain starting from nothing").isZero();

        assertThat(count(jdbc, """
                SELECT count(*) FROM event e
                 WHERE e.status IS DISTINCT FROM (SELECT t.to_status FROM event_transition t
                                                   WHERE t.event_id = e.event_id ORDER BY t.transition_id DESC LIMIT 1)
                """)).as("the last recorded transition is the event's current state").isZero();

        assertThat(count(jdbc, """
                SELECT count(*) FROM (SELECT tenant_id, inbox_id, merge_key FROM run
                                       WHERE status = 'active' AND merge_key IS NOT NULL
                                       GROUP BY tenant_id, inbox_id, merge_key HAVING count(*) > 1) d
                """)).as("at most one active run per merge key in an inbox").isZero();

        long deadlocks = count(jdbc, "SELECT deadlocks FROM pg_stat_database WHERE datname = current_database()");
        if (deadlocksAtStart == null) {
            deadlocksAtStart = deadlocks;
        }
        assertThat(deadlocks).as("the lock order never deadlocks").isEqualTo(deadlocksAtStart);
    }

    private static long count(JdbcClient jdbc, String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }
}
