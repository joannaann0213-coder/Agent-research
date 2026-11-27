package dev.agentflow.agent.server.stats;

import dev.agentflow.agent.api.LevelStats;
import dev.agentflow.agent.api.MessageStats;
import dev.agentflow.agent.api.ReplyStats;
import dev.agentflow.agent.api.StatsView;
import dev.agentflow.agent.api.TaskStats;
import dev.agentflow.agent.api.TurnStats;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.time.AgentClock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * The numbers by which the settings of an agent are tuned: what its turns were, how they ended and
 * what they used, how long users waited, what was said and what became of the tasks. They are read
 * from the service's own records, for one user or for all users of an application, over a span of
 * time. Nothing is kept for this beyond what a turn records about itself.
 */
@Service
public class Stats {

    /** The span of time that is looked at when the caller names none. */
    static final Duration DEFAULT_SPAN = Duration.ofHours(24);
    /** The longest span that can be asked for. */
    static final Duration LONGEST_SPAN = Duration.ofDays(92);

    private final JdbcClient jdbc;
    private final AgentClock clock;

    public Stats(JdbcClient jdbc, AgentClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * @param userId one user, or {@code null} for all users of the application
     * @param since from when, as an instant in ISO 8601; {@code null} for 24 hours before {@code until}
     * @param until to when; {@code null} for now
     */
    public StatsView of(String appId, String userId, String since, String until) {
        if (userId != null) {
            Names.userId(userId);
        }
        Instant to = until == null ? clock.now() : instant("until", until);
        Instant from = since == null ? to.minus(DEFAULT_SPAN) : instant("since", since);
        if (!from.isBefore(to)) {
            throw AgentException.invalid("since has to be before until");
        }
        if (Duration.between(from, to).compareTo(LONGEST_SPAN) > 0) {
            throw AgentException.invalid("the span from since to until is at most " + LONGEST_SPAN.toDays() + " days");
        }
        return new StatsView(from, to, userId == null ? users(appId, from, to) : null,
                userId == null ? null : ownSetting(appId, userId), turns(appId, userId, from, to), replies(appId, userId, from, to),
                messages(appId, userId, from, to), tasks(appId, userId, from, to), userId == null ? levels(appId, from, to) : null);
    }

    private static Instant instant(String name, String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw AgentException.invalid(name + " is an instant in ISO 8601, for example 2026-11-20T00:00:00Z");
        }
    }

    private static final String IN_SPAN = """
             WHERE app_id = :appId AND (CAST(:userId AS text) IS NULL OR user_id = :userId)
               AND created_at >= :since AND created_at < :until
            """;

    private JdbcClient.StatementSpec inSpan(String sql, String appId, String userId, Instant since, Instant until) {
        return jdbc.sql(sql).param("appId", appId).param("userId", userId).param("since", Db.ts(since)).param("until", Db.ts(until));
    }

    private long users(String appId, Instant since, Instant until) {
        return inSpan("SELECT count(DISTINCT user_id) FROM turn" + IN_SPAN, appId, null, since, until).query(Long.class).single();
    }

    private String ownSetting(String appId, String userId) {
        return jdbc.sql("SELECT proactivity FROM app_user WHERE app_id = :appId AND user_id = :userId")
                .param("appId", appId).param("userId", userId)
                .query((rs, row) -> rs.getString("proactivity")).optional().orElse(null);
    }

    /** A turn that failed has no outcome of its own: it is shown as failed. One that is still under way is left out. */
    private List<TurnStats> turns(String appId, String userId, Instant since, Instant until) {
        return inSpan("""
                SELECT COALESCE(kind, 'none') AS kind, COALESCE(outcome, state) AS outcome, count(*) AS turns,
                       COALESCE(sum(yields), 0) AS yields,
                       COALESCE(sum(EXTRACT(EPOCH FROM (finished_at - created_at))), 0) AS seconds,
                       COALESCE(sum(model_requests), 0) AS model_requests, COALESCE(sum(input_tokens), 0) AS input_tokens,
                       COALESCE(sum(output_tokens), 0) AS output_tokens, COALESCE(sum(cache_read_tokens), 0) AS cache_read_tokens,
                       COALESCE(sum(cache_creation_tokens), 0) AS cache_creation_tokens
                  FROM turn
                """ + IN_SPAN + " AND state <> 'open' GROUP BY 1, 2 ORDER BY 1, 2", appId, userId, since, until)
                .query((rs, row) -> new TurnStats(rs.getString("kind"), rs.getString("outcome"), rs.getLong("turns"),
                        rs.getLong("yields"), rs.getDouble("seconds"), rs.getLong("model_requests"), rs.getLong("input_tokens"),
                        rs.getLong("output_tokens"), rs.getLong("cache_read_tokens"), rs.getLong("cache_creation_tokens")))
                .list();
    }

    private ReplyStats replies(String appId, String userId, Instant since, Instant until) {
        return inSpan("""
                SELECT count(*) AS replies,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY waited_ms) / 1000.0 AS median,
                       percentile_cont(0.95) WITHIN GROUP (ORDER BY waited_ms) / 1000.0 AS p95
                  FROM turn
                """ + IN_SPAN + " AND waited_ms IS NOT NULL", appId, userId, since, until)
                .query((rs, row) -> {
                    long replies = rs.getLong("replies");
                    return new ReplyStats(replies, replies == 0 ? null : rs.getDouble("median"), replies == 0 ? null : rs.getDouble("p95"));
                })
                .single();
    }

    private MessageStats messages(String appId, String userId, Instant since, Instant until) {
        return inSpan("""
                SELECT count(*) FILTER (WHERE role = 'user') AS from_user,
                       count(*) FILTER (WHERE kind = 'reply') AS replies,
                       count(*) FILTER (WHERE kind = 'notification') AS notifications,
                       count(*) FILTER (WHERE kind = 'question') AS questions,
                       count(*) FILTER (WHERE kind = 'proposal') AS proposals
                  FROM message
                """ + IN_SPAN, appId, userId, since, until)
                .query((rs, row) -> new MessageStats(rs.getLong("from_user"), rs.getLong("replies"), rs.getLong("notifications"),
                        rs.getLong("questions"), rs.getLong("proposals")))
                .single();
    }

    private TaskStats tasks(String appId, String userId, Instant since, Instant until) {
        long failures = inSpan("SELECT count(*) FROM task_note" + IN_SPAN + " AND kind = 'failure'", appId, userId, since, until)
                .query(Long.class).single();
        return inSpan("""
                SELECT count(*) AS made,
                       count(*) FILTER (WHERE created_by = 'agent') AS by_agent,
                       count(*) FILTER (WHERE created_by = 'app') AS by_application,
                       count(*) FILTER (WHERE authorized_by = 'conversation') AS asked_for,
                       count(*) FILTER (WHERE created_by = 'agent' AND authorized_by IN ('user', 'app')) AS confirmed,
                       count(*) FILTER (WHERE authorized_by IN ('grant', 'policy')) AS unasked,
                       count(*) FILTER (WHERE authorized_by IS NULL AND status = 'closed' AND closed_by IN ('user', 'app')) AS declined,
                       count(*) FILTER (WHERE authorized_by IS NULL AND status = 'closed' AND closed_by = 'service') AS expired,
                       count(*) FILTER (WHERE authorized_by IS NULL AND status = 'closed' AND closed_by = 'agent') AS withdrawn,
                       count(*) FILTER (WHERE status = 'proposed') AS waiting
                  FROM delegated_task
                """ + IN_SPAN, appId, userId, since, until)
                .query((rs, row) -> new TaskStats(rs.getLong("made"), rs.getLong("by_agent"), rs.getLong("by_application"),
                        rs.getLong("asked_for"), rs.getLong("confirmed"), rs.getLong("unasked"), rs.getLong("declined"),
                        rs.getLong("expired"), rs.getLong("withdrawn"), rs.getLong("waiting"), failures))
                .single();
    }

    /** The application's users by their own setting, with what each group was sent in the span. */
    private List<LevelStats> levels(String appId, Instant since, Instant until) {
        return jdbc.sql("""
                SELECT COALESCE(u.proactivity, 'default') AS level, count(*) AS users,
                       COALESCE(sum((SELECT count(*) FROM message m
                                      WHERE m.app_id = u.app_id AND m.user_id = u.user_id AND m.kind = 'notification'
                                        AND m.created_at >= :since AND m.created_at < :until)), 0) AS notifications
                  FROM app_user u
                 WHERE u.app_id = :appId
                 GROUP BY 1 ORDER BY 1
                """)
                .param("appId", appId).param("since", Db.ts(since)).param("until", Db.ts(until))
                .query((rs, row) -> new LevelStats(rs.getString("level"), rs.getLong("users"), rs.getLong("notifications")))
                .list();
    }
}
