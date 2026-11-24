package dev.agentflow.agent.server.app;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.PreferencesView;
import dev.agentflow.agent.api.Proactivity;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.delivery.Outbound;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.time.AgentClock;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What a user wants of their agents. For now one thing: how much they hear from their main agent
 * without having asked ({@link Proactivity}). The user sets it, in their own client or by telling
 * the main agent; the application can set it for them. Where the user has no setting, the main
 * agent's own default applies, and where that has none, the service's.
 */
@Service
public class Preferences {

    public static final String BY_USER = "user";
    public static final String BY_APP = "app";
    public static final String BY_AGENT = "agent";

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Users users;
    private final AgentProfiles profiles;
    private final Outbound outbound;
    private final AgentClock clock;
    private final String serviceDefault;
    private final AgentProperties.Notify notify;

    public Preferences(JdbcClient jdbc, TransactionTemplate tx, Users users, AgentProfiles profiles, Outbound outbound,
            AgentClock clock, AgentProperties properties) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.profiles = profiles;
        this.outbound = outbound;
        this.clock = clock;
        String configured = properties.getTasks().getProactivity();
        if (!Proactivity.LEVELS.contains(configured)) {
            throw new IllegalStateException("agent.tasks.proactivity is one of " + String.join(", ", Proactivity.LEVELS)
                    + ", not " + configured);
        }
        this.serviceDefault = configured;
        this.notify = properties.getNotify();
    }

    /**
     * How many notifications, and questions asked unasked, the user gets within 24 hours at most: fewer
     * for a user who wants their agent quiet.
     */
    public int notificationsPerDay(String appId, String userId, String agentKey) {
        return Proactivity.QUIET.equals(proactivity(appId, userId, agentKey))
                ? Math.min(notify.getPerUserPerDay(), notify.getQuietPerUserPerDay()) : notify.getPerUserPerDay();
    }

    /** The user's own settings. A user nobody mentioned yet has none. */
    public PreferencesView get(String appId, String userId) {
        Names.userId(userId);
        return jdbc.sql("SELECT proactivity, proactivity_set_by, proactivity_updated_at FROM app_user WHERE app_id = :appId AND user_id = :userId")
                .param("appId", appId).param("userId", userId)
                .query((rs, row) -> new PreferencesView(rs.getString("proactivity"), rs.getString("proactivity_set_by"),
                        Db.instant(rs, "proactivity_updated_at")))
                .optional().orElse(new PreferencesView(null, null, null));
    }

    /**
     * Sets how forward the user wants their main agent, or takes the user's own setting away. Takes
     * the user's lock itself.
     *
     * @param proactivity a level, or {@code null} to go back to the agent's default
     * @param by {@code user} or {@code app}
     */
    public PreferencesView set(String appId, String userId, String proactivity, String by) {
        Names.userId(userId);
        check(proactivity);
        return tx.execute(status -> {
            users.ensure(appId, userId);
            users.lock(appId, userId);
            return setIn(appId, userId, proactivity, by);
        });
    }

    /**
     * The same in the caller's transaction, under the user's lock. Setting what is set already
     * changes nothing and records nothing.
     */
    public PreferencesView setIn(String appId, String userId, String proactivity, String by) {
        check(proactivity);
        PreferencesView before = get(appId, userId);
        if (java.util.Objects.equals(before.proactivity(), proactivity)) {
            return before;
        }
        jdbc.sql("""
                UPDATE app_user SET proactivity = :level, proactivity_set_by = :by, proactivity_updated_at = :now
                 WHERE app_id = :appId AND user_id = :userId
                """)
                .param("level", proactivity).param("by", by).param("now", Db.ts(clock.now()))
                .param("appId", appId).param("userId", userId)
                .update();
        PreferencesView now = get(appId, userId);
        outbound.record(appId, userId, Update.PREFERENCES_UPDATED, now);
        return now;
    }

    /**
     * How forward a main agent is with a user: the user's own setting, else the agent's default,
     * else the service's.
     */
    public String proactivity(String appId, String userId, String agentKey) {
        Optional<String> own = jdbc.sql("SELECT proactivity FROM app_user WHERE app_id = :appId AND user_id = :userId")
                .param("appId", appId).param("userId", userId)
                .query((rs, row) -> Optional.ofNullable(rs.getString("proactivity")))
                .optional().flatMap(found -> found);
        return own.orElseGet(() -> profiles.configIfThere(appId, agentKey).map(AgentProfileRequest::coordinator)
                .map(CoordinatorConfig::proactivity).orElse(serviceDefault));
    }

    private static void check(String proactivity) {
        if (proactivity != null && !Proactivity.LEVELS.contains(proactivity)) {
            throw AgentException.invalid("proactivity is one of " + String.join(", ", Proactivity.LEVELS)
                    + "; leave it out to go back to the agent's default");
        }
    }
}
