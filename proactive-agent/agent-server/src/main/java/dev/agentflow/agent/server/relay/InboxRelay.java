package dev.agentflow.agent.server.relay;

import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Gets events into the Event Inbox without ever writing to two systems at once.
 *
 * <p>Whoever has an event for the Inbox queues it here, inside the transaction that writes its own
 * row. After the commit the event is relayed; if that fails it is tried again with a growing delay.
 * The Inbox dedupes on the event's idempotency key, so an event that is relayed twice, by a retry or
 * by two instances of this service, is still one event.
 *
 * <p>A request the Inbox refuses as wrong is not retried: the event is marked failed and stays in
 * the table for someone to look at.
 */
@Service
public class InboxRelay {

    private static final Logger log = LoggerFactory.getLogger(InboxRelay.class);

    /** Answers that will not change by asking again. */
    private static final Set<Integer> REFUSED_FOR_GOOD = Set.of(400, 404, 409, 413, 422);

    private record Due(long relayId, String appId, String userId, String request, int attempts) {
    }

    private final JdbcClient jdbc;
    private final InboxClient inbox;
    private final AgentClock clock;
    private final AgentProperties.Relay config;
    private final Executor afterCommit = Executors.newVirtualThreadPerTaskExecutor();

    public InboxRelay(JdbcClient jdbc, InboxClient inbox, AgentClock clock, AgentProperties properties) {
        this.jdbc = jdbc;
        this.inbox = inbox;
        this.clock = clock;
        this.config = properties.getRelay();
    }

    /**
     * Queues an event for the inbox of a user. Call it inside the transaction that writes the row the
     * event is about; outside of one it refuses, because that would be the double write this class
     * exists to avoid.
     */
    public void enqueue(String appId, String userId, WriteEventRequest event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("an event for the Inbox must be queued inside a transaction");
        }
        Instant now = clock.now();
        jdbc.sql("""
                INSERT INTO inbox_relay (app_id, user_id, request, status, next_attempt_at, created_at)
                VALUES (:appId, :userId, CAST(:request AS jsonb), 'pending', :now, :now)
                """)
                .param("appId", appId).param("userId", userId)
                .param("request", InboxJson.mapper().writeValueAsString(event)).param("now", Db.ts(now))
                .update();
        if (config.isImmediate()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    afterCommit.execute(InboxRelay.this::runQuietly);
                }
            });
        }
    }

    /** One pass over the events that are due. Returns how many the Inbox took. */
    public int runOnce() {
        int relayed = 0;
        while (true) {
            List<Due> due = jdbc.sql("""
                    SELECT relay_id, app_id, user_id, request::text AS request, attempts
                      FROM inbox_relay
                     WHERE status = 'pending' AND next_attempt_at <= :now
                     ORDER BY relay_id
                     LIMIT :limit
                    """)
                    .param("now", Db.ts(clock.now())).param("limit", config.getBatchSize())
                    .query((rs, row) -> new Due(rs.getLong("relay_id"), rs.getString("app_id"), rs.getString("user_id"),
                            rs.getString("request"), rs.getInt("attempts")))
                    .list();
            for (Due event : due) {
                if (relay(event)) {
                    relayed++;
                }
            }
            if (due.size() < config.getBatchSize()) {
                return relayed;
            }
        }
    }

    /** How many events wait to be relayed, now or later. */
    public long pending() {
        return jdbc.sql("SELECT count(*) FROM inbox_relay WHERE status = 'pending'").query(Long.class).single();
    }

    private void runQuietly() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            log.warn("relaying to the Inbox failed; the loop will try again", e);
        }
    }

    private boolean relay(Due event) {
        String failure;
        boolean forGood = false;
        try {
            WriteEventRequest request = InboxJson.mapper().readValue(event.request(), WriteEventRequest.class);
            ApiResult<WriteEventResponse> result = inbox.writeEvent(event.appId(), event.userId(), request);
            if (result.ok()) {
                finish(event, "relayed", null);
                return true;
            }
            forGood = REFUSED_FOR_GOOD.contains(result.status());
            failure = "HTTP " + result.status() + " " + result.code()
                    + (result.problem() == null || result.problem().detail() == null ? "" : ": " + result.problem().detail());
        } catch (RuntimeException e) {
            failure = e.toString();
        }
        if (forGood) {
            log.error("the Inbox refused relay {} for app {} for good: {}", event.relayId(), event.appId(), failure);
            finish(event, "failed", failure);
        } else {
            Duration delay = backoff(event.attempts());
            log.warn("relay {} for app {} failed, next attempt in {}: {}", event.relayId(), event.appId(), delay, failure);
            jdbc.sql("""
                    UPDATE inbox_relay SET attempts = attempts + 1, next_attempt_at = :next, last_error = :error
                     WHERE relay_id = :id AND status = 'pending'
                    """)
                    .param("next", Db.ts(clock.now().plus(delay))).param("error", shorten(failure)).param("id", event.relayId())
                    .update();
        }
        return false;
    }

    private void finish(Due event, String status, String error) {
        jdbc.sql("""
                UPDATE inbox_relay SET status = :status, attempts = attempts + 1, finished_at = :now, last_error = :error
                 WHERE relay_id = :id AND status = 'pending'
                """)
                .param("status", status).param("now", Db.ts(clock.now())).param("error", shorten(error))
                .param("id", event.relayId())
                .update();
    }

    /** The configured delay, doubled for every attempt already made, up to the maximum. */
    private Duration backoff(int attemptsSoFar) {
        Duration delay = config.getBackoff().multipliedBy(1L << Math.min(attemptsSoFar, 20));
        return delay.compareTo(config.getMaxBackoff()) > 0 ? config.getMaxBackoff() : delay;
    }

    private static String shorten(String text) {
        return text == null || text.length() <= 500 ? text : text.substring(0, 500);
    }
}
