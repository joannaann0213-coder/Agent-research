package dev.agentflow.inbox.server.admin;

import dev.agentflow.inbox.api.AuditEntry;
import dev.agentflow.inbox.server.core.IdGenerator;
import dev.agentflow.inbox.server.db.Db;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Append-only record of operator actions. */
@Component
public class AuditLog {

    private final JdbcClient jdbc;
    private final IdGenerator ids;
    private final JsonMapper json;

    public AuditLog(JdbcClient jdbc, IdGenerator ids, JsonMapper json) {
        this.jdbc = jdbc;
        this.ids = ids;
        this.json = json;
    }

    public void record(String tenantId, String actorClientId, String operator, String action, JsonNode target,
            JsonNode params, JsonNode result, Instant now) {
        jdbc.sql("""
                INSERT INTO admin_audit (audit_id, tenant_id, actor_client_id, operator, action, target, params, result, at)
                VALUES (:id, CAST(:tenant AS text), :client, CAST(:operator AS text), :action, CAST(:target AS jsonb),
                        CAST(:params AS jsonb), CAST(:result AS jsonb), :now)
                """)
                .param("id", ids.newId()).param("tenant", tenantId).param("client", actorClientId)
                .param("operator", operator).param("action", action).param("target", target.toString())
                .param("params", params == null ? null : params.toString())
                .param("result", result == null ? null : result.toString()).param("now", Db.ts(now))
                .update();
    }

    /** Newest first. */
    public List<AuditEntry> list(String tenantId, int limit) {
        return jdbc.sql("""
                SELECT audit_id, tenant_id, actor_client_id, operator, action, target, params, result, at
                  FROM admin_audit
                 WHERE CAST(:tenant AS text) IS NULL OR tenant_id = :tenant
                 ORDER BY at DESC, audit_id DESC
                 LIMIT :limit
                """)
                .param("tenant", tenantId).param("limit", limit)
                .query((rs, n) -> new AuditEntry(rs.getString("audit_id"), rs.getString("tenant_id"),
                        rs.getString("actor_client_id"), rs.getString("operator"), rs.getString("action"),
                        read(rs.getString("target")), read(rs.getString("params")), read(rs.getString("result")),
                        Db.instant(rs, "at")))
                .list();
    }

    private JsonNode read(String text) {
        return text == null ? null : json.readTree(text);
    }
}
