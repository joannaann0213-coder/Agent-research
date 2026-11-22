package dev.agentflow.agent.server.delivery;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.WebhookSignature;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.time.AgentClock;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Sends what is queued for the webhooks, and tries again what failed, after growing delays. Several
 * instances can run it at once: a delivery is claimed before it is sent.
 */
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    /** How long a claimed delivery is left alone by other instances. Longer than a send can take. */
    private static final Duration CLAIM = Duration.ofMinutes(2);

    private record Due(long deliveryId, String appId, int attempts, Instant nextAttemptAt, String url, String secret, Update update) {
    }

    private final JdbcClient jdbc;
    private final WebhookSender sender;
    private final Webhooks webhooks;
    private final AgentClock clock;
    private final AgentProperties.Webhooks config;

    public WebhookDispatcher(JdbcClient jdbc, WebhookSender sender, Webhooks webhooks, AgentClock clock, AgentProperties properties) {
        this.jdbc = jdbc;
        this.sender = sender;
        this.webhooks = webhooks;
        this.clock = clock;
        this.config = properties.getWebhooks();
    }

    /** One pass over what is due. @return how many deliveries got through */
    public int runOnce() {
        List<Due> due = jdbc.sql("""
                SELECT d.delivery_id, d.app_id, d.attempts, d.next_attempt_at, w.url, w.secret,
                       e.event_id, e.seq, e.user_id, e.type, e.created_at, e.data::text AS data
                  FROM webhook_delivery d
                  JOIN webhook w ON w.app_id = d.app_id AND w.state = 'active'
                  JOIN outbound_event e ON e.event_id = d.event_id
                 WHERE d.status = 'pending' AND d.next_attempt_at <= :now
                 ORDER BY d.next_attempt_at, d.delivery_id
                 LIMIT :limit
                """)
                .param("now", Db.ts(clock.now())).param("limit", config.getBatchSize())
                .query((rs, row) -> new Due(rs.getLong("delivery_id"), rs.getString("app_id"), rs.getInt("attempts"),
                        Db.instant(rs, "next_attempt_at"), rs.getString("url"), rs.getString("secret"), Outbound.row(rs, row)))
                .list();
        int delivered = 0;
        for (Due delivery : due) {
            if (claim(delivery) && send(delivery)) {
                delivered++;
            }
        }
        return delivered;
    }

    private boolean claim(Due delivery) {
        return jdbc.sql("""
                UPDATE webhook_delivery SET next_attempt_at = :claimedUntil
                 WHERE delivery_id = :id AND status = 'pending' AND next_attempt_at = :seen
                """)
                .param("claimedUntil", Db.ts(clock.now().plus(CLAIM))).param("id", delivery.deliveryId())
                .param("seen", Db.ts(delivery.nextAttemptAt()))
                .update() == 1;
    }

    private boolean send(Due delivery) {
        byte[] body = AgentJson.mapper().writeValueAsString(delivery.update()).getBytes(StandardCharsets.UTF_8);
        long timestamp = clock.now().getEpochSecond();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", "agentflow-webhooks/1");
        headers.put(WebhookSignature.ID, delivery.update().id());
        headers.put(WebhookSignature.TIMESTAMP, Long.toString(timestamp));
        headers.put(WebhookSignature.SIGNATURE, WebhookSignature.sign(delivery.secret(), delivery.update().id(), timestamp, body));
        WebhookSender.Result result = sender.post(URI.create(delivery.url()), headers, body);
        int attempts = delivery.attempts() + 1;
        Instant now = clock.now();
        if (result.ok()) {
            jdbc.sql("""
                    UPDATE webhook_delivery SET status = 'delivered', attempts = :attempts, last_status = :status, last_error = NULL,
                           finished_at = :now
                     WHERE delivery_id = :id
                    """)
                    .param("attempts", attempts).param("status", result.status()).param("now", Db.ts(now))
                    .param("id", delivery.deliveryId())
                    .update();
            return true;
        }
        List<Duration> delays = config.getRetryDelays();
        boolean last = attempts > delays.size();
        jdbc.sql("""
                UPDATE webhook_delivery SET status = :status, attempts = :attempts, next_attempt_at = :next, last_status = :lastStatus,
                       last_error = :error, finished_at = :finished
                 WHERE delivery_id = :id
                """)
                .param("status", last ? "failed" : "pending").param("attempts", attempts)
                .param("next", last ? null : Db.ts(now.plus(delays.get(attempts - 1))))
                .param("lastStatus", result.status() == 0 ? null : result.status()).param("error", result.describe())
                .param("finished", last ? Db.ts(now) : null).param("id", delivery.deliveryId())
                .update();
        if (last) {
            String reason = "a delivery failed " + attempts + " times, the last time because " + result.describe();
            log.warn("webhook of app {} is turned off: {}", delivery.appId(), reason);
            webhooks.disable(delivery.appId(), reason);
        }
        return false;
    }
}
