package dev.agentflow.inbox.server.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Publishes how much is waiting. Backlog is reported per tenant and lane as the median, the 99th
 * percentile and the maximum across inboxes: a mean would hide the one inbox that is drowning.
 */
@Component
public class DepthSampler {

    private final JdbcClient jdbc;
    private final MeterRegistry registry;
    private final Map<String, AtomicLong> values = new ConcurrentHashMap<>();

    public DepthSampler(JdbcClient jdbc, MeterRegistry registry) {
        this.jdbc = jdbc;
        this.registry = registry;
    }

    /** One sample. Series that have nothing waiting any more drop to zero. */
    public void sample() {
        values.values().forEach(value -> value.set(0));

        jdbc.sql("""
                SELECT tenant_id, lane,
                       max(depth)                                          AS max_depth,
                       percentile_disc(0.99) WITHIN GROUP (ORDER BY depth) AS p99_depth,
                       percentile_disc(0.5)  WITHIN GROUP (ORDER BY depth) AS p50_depth,
                       sum(depth)                                          AS total_depth
                  FROM (SELECT tenant_id, inbox_id, lane, count(*) AS depth
                          FROM event
                         WHERE status = 'pending' AND run_id IS NULL
                         GROUP BY tenant_id, inbox_id, lane) per_inbox
                 GROUP BY tenant_id, lane
                """)
                .query((rs, n) -> {
                    String tenant = rs.getString("tenant_id");
                    String lane = rs.getString("lane");
                    set("inbox.pending.depth", "Unclaimed events per inbox", tenant, lane, "max", rs.getLong("max_depth"));
                    set("inbox.pending.depth", "Unclaimed events per inbox", tenant, lane, "p99", rs.getLong("p99_depth"));
                    set("inbox.pending.depth", "Unclaimed events per inbox", tenant, lane, "p50", rs.getLong("p50_depth"));
                    set("inbox.pending.depth", "Unclaimed events per inbox", tenant, lane, "total", rs.getLong("total_depth"));
                    return 0;
                })
                .list();

        jdbc.sql("SELECT tenant_id, lane, count(*) AS dead FROM event WHERE status = 'dead' GROUP BY tenant_id, lane")
                .query((rs, n) -> {
                    set("inbox.dead.letter.depth", "Events past their retry limit", rs.getString("tenant_id"),
                            rs.getString("lane"), "total", rs.getLong("dead"));
                    return 0;
                })
                .list();
    }

    private void set(String name, String description, String tenant, String lane, String stat, long value) {
        String key = name + '|' + tenant + '|' + lane + '|' + stat;
        values.computeIfAbsent(key, k -> {
            AtomicLong holder = new AtomicLong();
            Gauge.builder(name, holder, AtomicLong::get)
                    .description(description)
                    .tag("tenant", tenant).tag("lane", lane).tag("stat", stat)
                    .register(registry);
            return holder;
        }).set(value);
    }
}
