package dev.agentflow.agent.server.app;

import dev.agentflow.agent.api.AppView;
import dev.agentflow.agent.api.CreateAppRequest;
import dev.agentflow.agent.api.UpdateAppRequest;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.time.AgentClock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** The applications that use the service. */
@Service
public class Apps {

    private static final Set<String> STATUSES = Set.of("active", "suspended");

    private final JdbcClient jdbc;
    private final AgentClock clock;

    public Apps(JdbcClient jdbc, AgentClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public AppView create(CreateAppRequest request) {
        String appId = Names.appId(request.appId());
        String name = name(request.name());
        Instant now = clock.now();
        try {
            jdbc.sql("""
                    INSERT INTO app (app_id, name, status, created_at, updated_at)
                    VALUES (:appId, :name, 'active', :now, :now)
                    """)
                    .param("appId", appId).param("name", name).param("now", Db.ts(now))
                    .update();
        } catch (DuplicateKeyException exists) {
            throw AgentException.conflict("an application with the id " + appId + " exists already");
        }
        return new AppView(appId, name, "active", now);
    }

    public List<AppView> list() {
        return jdbc.sql("SELECT app_id, name, status, created_at FROM app ORDER BY app_id")
                .query((rs, row) -> new AppView(rs.getString("app_id"), rs.getString("name"), rs.getString("status"),
                        Db.instant(rs, "created_at")))
                .list();
    }

    public Optional<AppView> find(String appId) {
        return jdbc.sql("SELECT app_id, name, status, created_at FROM app WHERE app_id = :appId")
                .param("appId", appId)
                .query((rs, row) -> new AppView(rs.getString("app_id"), rs.getString("name"), rs.getString("status"),
                        Db.instant(rs, "created_at")))
                .optional();
    }

    public AppView get(String appId) {
        return find(appId).orElseThrow(() -> AgentException.notFound("no such application: " + appId));
    }

    /** Changes the name, the status, or both. A suspended application's keys and tokens are refused. */
    public AppView update(String appId, UpdateAppRequest request) {
        if (request.name() == null && request.status() == null) {
            throw AgentException.invalid("nothing to change: give name, status or both");
        }
        if (request.status() != null && !STATUSES.contains(request.status())) {
            throw AgentException.invalid("status must be active or suspended");
        }
        int changed = jdbc.sql("""
                UPDATE app SET name = COALESCE(:name, name), status = COALESCE(:status, status), updated_at = :now
                 WHERE app_id = :appId
                """)
                .param("name", request.name() == null ? null : name(request.name())).param("status", request.status())
                .param("now", Db.ts(clock.now())).param("appId", appId)
                .update();
        if (changed == 0) {
            throw AgentException.notFound("no such application: " + appId);
        }
        return get(appId);
    }

    private static String name(String name) {
        if (name == null || name.isBlank() || name.length() > 200) {
            throw AgentException.invalid("name is required and has at most 200 characters");
        }
        return name;
    }
}
