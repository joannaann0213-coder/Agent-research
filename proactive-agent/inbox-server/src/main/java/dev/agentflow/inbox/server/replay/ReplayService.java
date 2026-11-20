package dev.agentflow.inbox.server.replay;

import dev.agentflow.inbox.api.DeadLetterRequest;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.ReplayRequest;
import dev.agentflow.inbox.api.ReplayResponse;
import dev.agentflow.inbox.server.admin.AuditLog;
import dev.agentflow.inbox.server.api.InboxException;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.db.Db;
import dev.agentflow.inbox.server.metrics.InboxMetrics;
import dev.agentflow.inbox.server.time.InboxClock;
import dev.agentflow.inbox.server.wake.WakeBus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Operator tooling: put finished events back in the queue, or take a waiting one out of it.
 *
 * <p>A replayed event keeps its identity and its place in the sequence; it is detached from its old
 * run and will be claimed into a new one. Replaying a {@code failed} event is the ordinary remedy and
 * needs nothing special. Replaying a {@code done} or {@code dead} event repeats work that already had
 * an outcome, so it needs {@code force}. Every replay records who did it and why.
 */
@Service
public class ReplayService {

    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);
    private static final int MAX_EVENTS_PER_REQUEST = 10_000;

    private record Target(String eventId, String inboxId) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final InboxClock clock;
    private final AuditLog audit;
    private final InboxMetrics metrics;
    private final WakeBus wakeBus;
    private final JsonMapper json;

    public ReplayService(JdbcClient jdbc, TransactionTemplate transactions, InboxClock clock, AuditLog audit,
            InboxMetrics metrics, WakeBus wakeBus, JsonMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.audit = audit;
        this.metrics = metrics;
        this.wakeBus = wakeBus;
        this.json = json;
    }

    public ReplayResponse replay(AuthenticatedClient client, String tenantId, ReplayRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw InboxException.invalid("reason is required: a replay must say why");
        }
        boolean force = Boolean.TRUE.equals(request.force());
        boolean dryRun = Boolean.TRUE.equals(request.dryRun());
        String operator = operatorName(client, request.operator());
        List<Target> targets = select(tenantId, request);

        // One transaction per inbox, each under that inbox's lock.
        Map<String, List<String>> byInbox = new LinkedHashMap<>();
        for (Target target : targets) {
            byInbox.computeIfAbsent(target.inboxId(), k -> new ArrayList<>()).add(target.eventId());
        }
        List<String> replayed = new ArrayList<>();
        List<ReplayResponse.Skipped> skipped = new ArrayList<>();
        Instant now = clock.now();
        for (Map.Entry<String, List<String>> inbox : byInbox.entrySet()) {
            transactions.executeWithoutResult(tx -> replayInbox(tenantId, inbox.getKey(), inbox.getValue(), force,
                    dryRun, request.reason(), operator, now, replayed, skipped));
        }

        if (!dryRun) {
            ObjectNode target = json.createObjectNode();
            if (request.eventId() != null) {
                target.put("event_id", request.eventId());
            }
            if (request.inboxId() != null) {
                target.put("inbox_id", request.inboxId());
            }
            if (request.source() != null) {
                target.put("source", request.source().wire());
            }
            if (request.from() != null) {
                target.put("from", request.from().toString());
            }
            if (request.to() != null) {
                target.put("to", request.to().toString());
            }
            ObjectNode params = json.createObjectNode().put("force", force).put("reason", request.reason());
            ObjectNode result = json.createObjectNode().put("replayed", replayed.size()).put("skipped", skipped.size());
            audit.record(tenantId, client.clientId(), operator, "replay", target, params, result, now);
            if (!replayed.isEmpty()) {
                metrics.replayed(tenantId, force, replayed.size());
                publishWake();
            }
        }
        return new ReplayResponse(List.copyOf(replayed), List.copyOf(skipped), dryRun);
    }

    private void replayInbox(String tenantId, String inboxId, List<String> eventIds, boolean force, boolean dryRun,
            String reason, String operator, Instant now, List<String> replayed, List<ReplayResponse.Skipped> skipped) {
        jdbc.sql("SELECT 1 FROM inbox WHERE tenant_id = :tenant AND inbox_id = :inbox FOR UPDATE")
                .param("tenant", tenantId).param("inbox", inboxId)
                .query(Integer.class).single();
        String idList = String.join(",", eventIds);
        record Current(String eventId, String status) {
        }
        List<Current> current = jdbc.sql("""
                SELECT event_id, status FROM event
                 WHERE tenant_id = :tenant AND inbox_id = :inbox AND event_id = ANY(string_to_array(:ids, ','))
                 ORDER BY seq
                """)
                .param("tenant", tenantId).param("inbox", inboxId).param("ids", idList)
                .query((rs, n) -> new Current(rs.getString("event_id"), rs.getString("status")))
                .list();

        Map<String, List<String>> replayable = new LinkedHashMap<>(); // by current status
        for (Current event : current) {
            switch (event.status()) {
                case "failed" -> replayable.computeIfAbsent("failed", k -> new ArrayList<>()).add(event.eventId());
                case "done", "dead" -> {
                    if (force) {
                        replayable.computeIfAbsent(event.status(), k -> new ArrayList<>()).add(event.eventId());
                    } else {
                        skipped.add(new ReplayResponse.Skipped(event.eventId(), event.status(), ReplayResponse.NEEDS_FORCE));
                    }
                }
                default -> skipped.add(new ReplayResponse.Skipped(event.eventId(), event.status(), ReplayResponse.IN_FLIGHT));
            }
        }
        for (Map.Entry<String, List<String>> group : replayable.entrySet()) {
            if (!dryRun) {
                jdbc.sql("""
                        WITH moved AS (
                            UPDATE event
                               SET status = 'pending', run_id = NULL, run_seq = NULL, delivery_mode = NULL, attempt = 0,
                                   finished_at = NULL, last_error = NULL
                             WHERE tenant_id = :tenant AND inbox_id = :inbox AND status = :from
                               AND event_id = ANY(string_to_array(:ids, ','))
                            RETURNING event_id, tenant_id, inbox_id
                        )
                        INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason,
                                                      actor, attempt, detail, at)
                        SELECT event_id, tenant_id, inbox_id, :from, 'pending', 'replay', :actor, 0,
                               jsonb_build_object('force', CAST(:force AS boolean), 'reason', CAST(:reason AS text)), :now
                          FROM moved
                        """)
                        .param("tenant", tenantId).param("inbox", inboxId).param("from", group.getKey())
                        .param("ids", String.join(",", group.getValue())).param("actor", "operator:" + operator)
                        .param("force", force).param("reason", reason).param("now", Db.ts(now))
                        .update();
            }
            replayed.addAll(group.getValue());
        }
    }

    private List<Target> select(String tenantId, ReplayRequest request) {
        boolean byEvent = request.eventId() != null;
        boolean byInbox = request.inboxId() != null;
        boolean bySource = request.source() != null;
        if ((byEvent ? 1 : 0) + (byInbox ? 1 : 0) + (bySource ? 1 : 0) != 1) {
            throw InboxException.invalid("give exactly one selector: event_id, or inbox_id with from/to, or source with from/to");
        }
        if (byEvent) {
            List<Target> found = jdbc.sql("SELECT event_id, inbox_id FROM event WHERE event_id = :id AND tenant_id = :tenant")
                    .param("id", request.eventId()).param("tenant", tenantId)
                    .query((rs, n) -> new Target(rs.getString("event_id"), rs.getString("inbox_id")))
                    .list();
            if (found.isEmpty()) {
                throw InboxException.notFound("no such event in this tenant");
            }
            return found;
        }
        if (request.from() == null || request.to() == null || !request.from().isBefore(request.to())) {
            throw InboxException.invalid("from and to are required for a range replay, and from must be before to");
        }
        List<Target> found = jdbc.sql("""
                SELECT event_id, inbox_id FROM event
                 WHERE tenant_id = :tenant
                   AND (CAST(:inbox AS text) IS NULL OR inbox_id = :inbox)
                   AND (CAST(:source AS text) IS NULL OR source = :source)
                   AND received_at >= :from AND received_at < :to
                 ORDER BY inbox_id, seq
                 LIMIT :limit
                """)
                .param("tenant", tenantId).param("inbox", request.inboxId())
                .param("source", request.source() == null ? null : request.source().wire())
                .param("from", Db.ts(request.from())).param("to", Db.ts(request.to()))
                .param("limit", MAX_EVENTS_PER_REQUEST + 1)
                .query((rs, n) -> new Target(rs.getString("event_id"), rs.getString("inbox_id")))
                .list();
        if (found.size() > MAX_EVENTS_PER_REQUEST) {
            throw InboxException.invalid("the range matches more than " + MAX_EVENTS_PER_REQUEST
                    + " events; narrow it and replay in several steps");
        }
        return found;
    }

    /** Takes an event that is still waiting out of the queue. It can be replayed later, with force. */
    public void deadLetter(AuthenticatedClient client, String tenantId, String eventId, DeadLetterRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw InboxException.invalid("reason is required");
        }
        String operator = operatorName(client, request.operator());
        List<String> inboxes = jdbc.sql("SELECT inbox_id FROM event WHERE event_id = :id AND tenant_id = :tenant")
                .param("id", eventId).param("tenant", tenantId).query(String.class).list();
        if (inboxes.isEmpty()) {
            throw InboxException.notFound("no such event in this tenant");
        }
        Instant now = clock.now();
        transactions.executeWithoutResult(tx -> {
            jdbc.sql("SELECT 1 FROM inbox WHERE tenant_id = :tenant AND inbox_id = :inbox FOR UPDATE")
                    .param("tenant", tenantId).param("inbox", inboxes.getFirst())
                    .query(Integer.class).single();
            int moved = jdbc.sql("""
                    WITH moved AS (
                        UPDATE event SET status = 'dead', finished_at = :now, last_error = :reason
                         WHERE event_id = :id AND status = 'pending' AND run_id IS NULL
                        RETURNING event_id, tenant_id, inbox_id, attempt
                    ), history AS (
                        INSERT INTO event_transition (event_id, tenant_id, inbox_id, from_status, to_status, reason,
                                                      actor, attempt, detail, at)
                        SELECT event_id, tenant_id, inbox_id, 'pending', 'dead', 'manual_dead', :actor, attempt,
                               jsonb_build_object('reason', CAST(:reason AS text)), :now
                          FROM moved
                    )
                    SELECT count(*) FROM moved
                    """)
                    .param("now", Db.ts(now)).param("reason", request.reason()).param("id", eventId)
                    .param("actor", "operator:" + operator)
                    .query(Integer.class).single();
            if (moved == 0) {
                throw InboxException.conflict(ProblemCodes.CONFLICT,
                        "only an event that is waiting unclaimed can be dead-lettered by hand");
            }
            audit.record(tenantId, client.clientId(), operator, "manual_dead",
                    json.createObjectNode().put("event_id", eventId),
                    json.createObjectNode().put("reason", request.reason()), null, now);
        });
    }

    private static String operatorName(AuthenticatedClient client, String operator) {
        return operator == null || operator.isBlank() ? client.clientId() : operator.strip();
    }

    private void publishWake() {
        try {
            wakeBus.publish();
        } catch (RuntimeException e) {
            log.warn("wake hint could not be published", e);
        }
    }
}
