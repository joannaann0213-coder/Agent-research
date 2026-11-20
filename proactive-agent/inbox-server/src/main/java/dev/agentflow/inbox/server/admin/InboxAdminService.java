package dev.agentflow.inbox.server.admin;

import dev.agentflow.inbox.api.InboxConfigRequest;
import dev.agentflow.inbox.api.InboxStatus;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.claim.InboxLimitsResolver;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.time.InboxClock;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Per-inbox configuration and a snapshot of what is waiting in an inbox. */
@Service
public class InboxAdminService {

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final InboxClock clock;
    private final InboxLimitsResolver limitsResolver;
    private final AuditLog audit;
    private final JsonMapper json;

    public InboxAdminService(JdbcClient jdbc, TransactionTemplate transactions, InboxClock clock,
            InboxLimitsResolver limitsResolver, AuditLog audit, JsonMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.limitsResolver = limitsResolver;
        this.audit = audit;
        this.json = json;
    }

    /** Replaces the inbox's overrides; creates the inbox when it does not exist yet. */
    public InboxStatus configure(AuthenticatedClient client, String tenantId, String inboxId, InboxConfigRequest request) {
        checkCap("cap_interactive", request.capInteractive());
        checkCap("cap_background", request.capBackground());
        checkCap("cap_batch", request.capBatch());
        checkCap("max_active_runs", request.maxActiveRuns());
        JsonNode settings = request.settings() == null ? json.createObjectNode() : request.settings();
        if (!settings.isObject()) {
            throw InboxException.invalid("settings must be a JSON object");
        }
        try {
            // Reject settings the claim path could not read, before they are stored.
            limitsResolver.resolve(request.capInteractive(), request.capBackground(), request.capBatch(),
                    request.maxActiveRuns(), settings.toString());
        } catch (RuntimeException e) {
            throw InboxException.invalid("settings are not valid: " + e.getMessage());
        }
        Instant now = clock.now();
        String operator = request.operator() == null || request.operator().isBlank()
                ? client.clientId() : request.operator().strip();
        transactions.executeWithoutResult(tx -> {
            jdbc.sql("""
                    INSERT INTO inbox AS i (tenant_id, inbox_id, next_seq, cap_interactive, cap_background, cap_batch,
                                            max_active_runs, settings, created_at, updated_at)
                    VALUES (:tenant, :inbox, 1, CAST(:capInteractive AS smallint), CAST(:capBackground AS smallint),
                            CAST(:capBatch AS smallint), CAST(:maxActiveRuns AS smallint), CAST(:settings AS jsonb),
                            :now, :now)
                    ON CONFLICT (tenant_id, inbox_id) DO UPDATE
                       SET cap_interactive = EXCLUDED.cap_interactive, cap_background = EXCLUDED.cap_background,
                           cap_batch = EXCLUDED.cap_batch, max_active_runs = EXCLUDED.max_active_runs,
                           settings = EXCLUDED.settings, updated_at = EXCLUDED.updated_at
                    """)
                    .param("tenant", tenantId).param("inbox", inboxId)
                    .param("capInteractive", request.capInteractive()).param("capBackground", request.capBackground())
                    .param("capBatch", request.capBatch()).param("maxActiveRuns", request.maxActiveRuns())
                    .param("settings", settings.toString()).param("now", Db.ts(now))
                    .update();
            ObjectNode params = json.createObjectNode();
            putOrNull(params, "cap_interactive", request.capInteractive());
            putOrNull(params, "cap_background", request.capBackground());
            putOrNull(params, "cap_batch", request.capBatch());
            putOrNull(params, "max_active_runs", request.maxActiveRuns());
            params.set("settings", settings);
            audit.record(tenantId, client.clientId(), operator, "inbox_config",
                    json.createObjectNode().put("inbox_id", inboxId), params, null, now);
        });
        return status(tenantId, inboxId);
    }

    public InboxStatus status(String tenantId, String inboxId) {
        List<InboxStatus> rows = jdbc.sql("""
                SELECT i.tenant_id, i.inbox_id, i.cap_interactive, i.cap_background, i.cap_batch, i.max_active_runs,
                       i.settings,
                       (SELECT count(*) FROM event e WHERE e.tenant_id = i.tenant_id AND e.inbox_id = i.inbox_id
                           AND e.status = 'pending' AND e.run_id IS NULL)     AS pending,
                       (SELECT count(*) FROM event e WHERE e.tenant_id = i.tenant_id AND e.inbox_id = i.inbox_id
                           AND e.status = 'pending' AND e.run_id IS NOT NULL) AS retrying,
                       (SELECT count(*) FROM event e WHERE e.tenant_id = i.tenant_id AND e.inbox_id = i.inbox_id
                           AND e.status = 'claimed')                          AS claimed,
                       (SELECT count(*) FROM event e WHERE e.tenant_id = i.tenant_id AND e.inbox_id = i.inbox_id
                           AND e.status = 'dead')                             AS dead,
                       (SELECT count(*) FROM run r WHERE r.tenant_id = i.tenant_id AND r.inbox_id = i.inbox_id
                           AND r.status = 'active')                           AS active_runs,
                       (SELECT count(*) FROM run r WHERE r.tenant_id = i.tenant_id AND r.inbox_id = i.inbox_id
                           AND r.status = 'queued')                           AS queued_runs,
                       (SELECT count(*) FROM run r WHERE r.tenant_id = i.tenant_id AND r.inbox_id = i.inbox_id
                           AND r.status = 'suspended')                        AS suspended_runs
                  FROM inbox i
                 WHERE i.tenant_id = :tenant AND i.inbox_id = :inbox
                """)
                .param("tenant", tenantId).param("inbox", inboxId)
                .query((rs, n) -> new InboxStatus(rs.getString("tenant_id"), rs.getString("inbox_id"),
                        Db.intOrNull(rs, "cap_interactive"), Db.intOrNull(rs, "cap_background"),
                        Db.intOrNull(rs, "cap_batch"), Db.intOrNull(rs, "max_active_runs"),
                        json.readTree(rs.getString("settings")), rs.getLong("pending"), rs.getLong("retrying"),
                        rs.getLong("claimed"), rs.getLong("dead"), rs.getLong("active_runs"),
                        rs.getLong("queued_runs"), rs.getLong("suspended_runs")))
                .list();
        if (rows.isEmpty()) {
            throw InboxException.notFound("no such inbox in this tenant");
        }
        return rows.getFirst();
    }

    private static void checkCap(String name, Integer value) {
        if (value != null && (value < 1 || value > 100)) {
            throw InboxException.invalid(name + " must be between 1 and 100");
        }
    }

    private static void putOrNull(ObjectNode node, String name, Integer value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value);
        }
    }
}
