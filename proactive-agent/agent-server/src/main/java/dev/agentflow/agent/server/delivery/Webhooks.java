package dev.agentflow.agent.server.delivery;

import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.WebhookRequest;
import dev.agentflow.agent.api.WebhookSignature;
import dev.agentflow.agent.api.WebhookView;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.time.AgentClock;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * An application's webhook: where the updates of all its users are delivered, signed, by a POST
 * each. A delivery that fails is tried again for hours; when one fails for good, the service stops
 * delivering until the application turns the webhook on again.
 */
@Service
public class Webhooks {

    public static final List<String> TYPES = List.of(Update.MESSAGE_CREATED, Update.ACTION_CREATED, Update.ACTION_RESOLVED,
            Update.RUN_FAILED, Update.TASK_CREATED, Update.TASK_UPDATED, Update.PREFERENCES_UPDATED);

    public static final String ACTIVE = "active";
    public static final String DISABLED = "disabled";

    private static final SecureRandom RANDOM = new SecureRandom();

    record Hook(String appId, String url, List<String> events, String secret, String state, String disabledReason,
            Instant createdAt, Instant updatedAt) {

        WebhookView view(boolean withSecret) {
            return new WebhookView(url, events, state, disabledReason, withSecret ? secret : null, createdAt, updatedAt);
        }
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final AddressPolicy policy;
    private final AgentClock clock;

    public Webhooks(JdbcClient jdbc, TransactionTemplate tx, AddressPolicy policy, AgentClock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.policy = policy;
        this.clock = clock;
    }

    /** Sets the webhook up, or changes it. The signing secret is shown when it is made, and only then. */
    public WebhookView put(String appId, WebhookRequest request) {
        if (request == null) {
            throw AgentException.invalid("a request body is required");
        }
        String problem = policy.problemWith(request.url());
        if (problem != null) {
            throw AgentException.invalid(problem);
        }
        List<String> events = null;
        if (request.events() != null) {
            if (request.events().isEmpty() || new HashSet<>(request.events()).size() != request.events().size()
                    || !TYPES.containsAll(request.events())) {
                throw AgentException.invalid("events lists each of " + String.join(", ", TYPES) + " at most once, or is left out for all");
            }
            events = List.copyOf(request.events());
        }
        List<String> chosen = events;
        return tx.execute(status -> {
            Instant now = clock.now();
            Optional<Hook> existing = find(appId, true);
            boolean newSecret = existing.isEmpty() || Boolean.TRUE.equals(request.rotateSecret());
            String secret = newSecret ? newSecret() : existing.get().secret();
            String state = Boolean.FALSE.equals(request.enabled()) ? DISABLED
                    : Boolean.TRUE.equals(request.enabled()) || existing.isEmpty() ? ACTIVE : existing.get().state();
            String reason = DISABLED.equals(state)
                    ? Boolean.FALSE.equals(request.enabled()) ? "turned off by the application" : existing.get().disabledReason()
                    : null;
            jdbc.sql("""
                    INSERT INTO webhook (app_id, url, events, secret, state, disabled_reason, created_at, updated_at)
                    VALUES (:appId, :url, CAST(:events AS jsonb), :secret, :state, :reason, :now, :now)
                    ON CONFLICT (app_id) DO UPDATE SET url = EXCLUDED.url, events = EXCLUDED.events, secret = EXCLUDED.secret,
                           state = EXCLUDED.state, disabled_reason = EXCLUDED.disabled_reason, updated_at = EXCLUDED.updated_at
                    """)
                    .param("appId", appId).param("url", request.url())
                    .param("events", chosen == null ? null : Json.STORED.writeValueAsString(chosen)).param("secret", secret)
                    .param("state", state).param("reason", reason).param("now", Db.ts(now))
                    .update();
            if (DISABLED.equals(state)) {
                skipPending(appId);
            }
            return find(appId, false).orElseThrow().view(newSecret);
        });
    }

    public Optional<WebhookView> get(String appId) {
        return find(appId, false).map(hook -> hook.view(false));
    }

    /** @return whether there was one */
    public boolean delete(String appId) {
        return Boolean.TRUE.equals(tx.execute(status -> {
            skipPending(appId);
            return jdbc.sql("DELETE FROM webhook WHERE app_id = :appId").param("appId", appId).update() == 1;
        }));
    }

    /** Queues the delivery of an update, if the application's webhook wants it. In the caller's transaction. */
    void enqueue(String appId, String userId, String eventId, String type) {
        jdbc.sql("""
                INSERT INTO webhook_delivery (app_id, user_id, event_id, status, next_attempt_at, created_at)
                SELECT :appId, :userId, :eventId, 'pending', :now, :now FROM webhook
                 WHERE app_id = :appId AND state = 'active' AND (events IS NULL OR events @> jsonb_build_array(CAST(:type AS text)))
                """)
                .param("appId", appId).param("userId", userId).param("eventId", eventId).param("type", type)
                .param("now", Db.ts(clock.now()))
                .update();
    }

    /** Stops deliveries: the receiver kept failing. The application turns the webhook on again with a PUT. */
    void disable(String appId, String reason) {
        tx.executeWithoutResult(status -> {
            jdbc.sql("UPDATE webhook SET state = 'disabled', disabled_reason = :reason, updated_at = :now WHERE app_id = :appId")
                    .param("reason", reason).param("now", Db.ts(clock.now())).param("appId", appId)
                    .update();
            skipPending(appId);
        });
    }

    private void skipPending(String appId) {
        jdbc.sql("UPDATE webhook_delivery SET status = 'skipped', finished_at = :now WHERE app_id = :appId AND status = 'pending'")
                .param("now", Db.ts(clock.now())).param("appId", appId)
                .update();
    }

    private Optional<Hook> find(String appId, boolean forUpdate) {
        return jdbc.sql("""
                SELECT app_id, url, events::text AS events, secret, state, disabled_reason, created_at, updated_at
                  FROM webhook WHERE app_id = :appId
                """ + (forUpdate ? " FOR UPDATE" : ""))
                .param("appId", appId)
                .query(Webhooks::row)
                .optional();
    }

    private static String newSecret() {
        byte[] secret = new byte[24];
        RANDOM.nextBytes(secret);
        return WebhookSignature.SECRET_PREFIX + Base64.getEncoder().encodeToString(secret);
    }

    private static Hook row(ResultSet rs, int rowNumber) throws SQLException {
        String events = rs.getString("events");
        List<String> list = null;
        if (events != null) {
            list = new ArrayList<>();
            for (JsonNode type : Json.STORED.readTree(events)) {
                list.add(type.asString());
            }
        }
        return new Hook(rs.getString("app_id"), rs.getString("url"), list, rs.getString("secret"), rs.getString("state"),
                rs.getString("disabled_reason"), Db.instant(rs, "created_at"), Db.instant(rs, "updated_at"));
    }
}
