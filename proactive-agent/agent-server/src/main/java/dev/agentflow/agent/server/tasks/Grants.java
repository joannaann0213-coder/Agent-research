package dev.agentflow.agent.server.tasks;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.GrantPage;
import dev.agentflow.agent.api.GrantView;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.agent.server.tools.PlatformTools;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The standing authorisations of users: a user allows their main agent to start the tasks of one
 * role on its own initiative, without asking each time. Without one, such a task waits for the
 * user's confirmation.
 *
 * <p>An authorisation is the user's to give and to take back; the application can do both for them.
 * No agent can: there is no tool for it. It names a role, and what a role can do is what its
 * worker's agent can do, so an authorisation is as wide as the application made the role, and the
 * application decides which roles can be authorised at all.
 */
@Service
public class Grants {

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Users users;
    private final AgentProfiles profiles;
    private final AgentClock clock;

    public Grants(JdbcClient jdbc, TransactionTemplate tx, Users users, AgentProfiles profiles, AgentClock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.profiles = profiles;
        this.clock = clock;
    }

    /** The user's authorisations, by main agent and role. */
    public GrantPage list(String appId, String userId) {
        Names.userId(userId);
        return new GrantPage(jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId ORDER BY agent_key, role")
                .param("appId", appId).param("userId", userId)
                .query(Grants::view)
                .list());
    }

    /**
     * Allows the main agent {@code agentKey} to start the tasks of {@code role} for this user without
     * asking. Giving it again changes nothing, and keeps who gave it first.
     *
     * @param by {@code user} or {@code app}
     */
    public GrantView grant(String appId, String userId, String agentKey, String role, String by) {
        Names.userId(userId);
        WorkerConfig worker = roleOf(appId, agentKey, role);
        if (WorkerConfig.ALWAYS_ASK.equals(worker.startRule())) {
            throw AgentException.invalid("every task of the role " + role + " is confirmed by the user, one by one: the "
                    + "application allows no standing authorisation for it");
        }
        return tx.execute(status -> {
            users.ensure(appId, userId);
            jdbc.sql("""
                    INSERT INTO task_grant (app_id, user_id, agent_key, role, granted_by, created_at)
                    VALUES (:appId, :userId, :agentKey, :role, :by, :now)
                    ON CONFLICT (app_id, user_id, agent_key, role) DO NOTHING
                    """)
                    .param("appId", appId).param("userId", userId).param("agentKey", agentKey).param("role", role)
                    .param("by", by).param("now", Db.ts(clock.now()))
                    .update();
            return jdbc.sql(SELECT + " WHERE app_id = :appId AND user_id = :userId AND agent_key = :agentKey AND role = :role")
                    .param("appId", appId).param("userId", userId).param("agentKey", agentKey).param("role", role)
                    .query(Grants::view)
                    .single();
        });
    }

    /** Takes an authorisation back. Tasks that have started go on; taking back what is not there changes nothing. */
    public void revoke(String appId, String userId, String agentKey, String role) {
        Names.userId(userId);
        jdbc.sql("DELETE FROM task_grant WHERE app_id = :appId AND user_id = :userId AND agent_key = :agentKey AND role = :role")
                .param("appId", appId).param("userId", userId).param("agentKey", agentKey).param("role", role)
                .update();
    }

    /** Whether the user has allowed the main agent to start the tasks of the role unasked. */
    public boolean granted(String appId, String userId, String agentKey, String role) {
        return jdbc.sql("""
                SELECT count(*) FROM task_grant
                 WHERE app_id = :appId AND user_id = :userId AND agent_key = :agentKey AND role = :role
                """)
                .param("appId", appId).param("userId", userId).param("agentKey", agentKey).param("role", role)
                .query(Long.class).single() == 1;
    }

    /** The role as the main agent's list has it now. */
    private WorkerConfig roleOf(String appId, String agentKey, String role) {
        if (agentKey == null || !Names.AGENT_KEY.matcher(agentKey).matches() || role == null
                || !AgentProfiles.ROLE.matcher(role).matches()) {
            throw AgentException.notFound("no role " + role + " of a main agent " + agentKey + " in this application");
        }
        return profiles.configIfThere(appId, agentKey).map(AgentProfileRequest::coordinator).filter(PlatformTools::delegates)
                .flatMap(coordinator -> coordinator.workers().stream().filter(worker -> worker.role().equals(role)).findFirst())
                .orElseThrow(() -> AgentException.notFound("no role " + role + " of a main agent " + agentKey
                        + " in this application"));
    }

    private static final String SELECT = "SELECT agent_key, role, granted_by, created_at FROM task_grant";

    private static GrantView view(ResultSet rs, int rowNumber) throws SQLException {
        return new GrantView(rs.getString("agent_key"), rs.getString("role"), rs.getString("granted_by"),
                Db.instant(rs, "created_at"));
    }
}
