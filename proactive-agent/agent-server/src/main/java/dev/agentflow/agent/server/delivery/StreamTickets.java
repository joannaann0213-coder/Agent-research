package dev.agentflow.agent.server.delivery;

import dev.agentflow.agent.api.StreamTicket;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.Hashing;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * One-time tickets that open a user's stream without a header, for a browser's EventSource. A
 * ticket is good once, for a minute, for the one user it was issued for. Only its hash is kept.
 */
@Service
public class StreamTickets {

    public static final String PREFIX = "stk_";
    public static final Duration TTL = Duration.ofMinutes(1);

    /** @param appId the application; @param userId the one user whose stream the ticket opens */
    public record Holder(String appId, String userId) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcClient jdbc;
    private final AgentClock clock;

    public StreamTickets(JdbcClient jdbc, AgentClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public StreamTicket issue(String appId, String userId) {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String ticket = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Instant expiresAt = clock.now().plus(TTL);
        jdbc.sql("INSERT INTO stream_ticket (ticket_hash, app_id, user_id, expires_at) VALUES (:hash, :appId, :userId, :expiresAt)")
                .param("hash", hash(ticket)).param("appId", appId).param("userId", userId).param("expiresAt", Db.ts(expiresAt))
                .update();
        // Tickets that can no longer be used are of no further interest.
        jdbc.sql("DELETE FROM stream_ticket WHERE expires_at < :before").param("before", Db.ts(clock.now().minus(Duration.ofHours(1))))
                .update();
        return new StreamTicket(ticket, expiresAt);
    }

    /** Uses the ticket up. Empty when it is unknown, expired or used already. */
    public Optional<Holder> use(String ticket) {
        if (ticket == null || !ticket.startsWith(PREFIX)) {
            return Optional.empty();
        }
        return jdbc.sql("""
                UPDATE stream_ticket SET used_at = :now
                 WHERE ticket_hash = :hash AND used_at IS NULL AND expires_at > :now
                RETURNING app_id, user_id
                """)
                .param("now", Db.ts(clock.now())).param("hash", hash(ticket))
                .query((rs, row) -> new Holder(rs.getString("app_id"), rs.getString("user_id")))
                .optional();
    }

    private static String hash(String ticket) {
        return Hashing.sha256Hex(ticket.getBytes(StandardCharsets.UTF_8));
    }
}
