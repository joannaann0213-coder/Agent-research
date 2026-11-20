package dev.agentflow.inbox.server.support;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base of every database test: the real service on a random port against a real PostgreSQL, with a
 * clock that only moves when the test moves it. Each test starts from empty tables.
 *
 * <p>The database comes from {@code INBOX_TEST_JDBC_URL} (default: the local {@code inbox_test}
 * database that {@code scripts/pg-local.sh} creates).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestBeans.class)
public abstract class IntegrationTestBase {

    protected static final String ACME = "acme";
    protected static final String GLOBEX = "globex";

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected MutableClock clock;

    /** Producer of tenant acme. */
    protected InboxClient acme;
    /** A second producer of tenant acme. */
    protected InboxClient acme2;
    /** Producer of tenant globex. */
    protected InboxClient globex;
    /** The agent runtime (platform scope). */
    protected InboxClient runtime;
    /** An operator (platform scope). */
    protected InboxClient ops;

    @BeforeEach
    void resetState() {
        jdbc.sql("TRUNCATE TABLE event_transition, admin_audit, timer, event, run, inbox RESTART IDENTITY CASCADE").update();
        clock.reset();
        acme = client("acme-bot", "acme-secret");
        acme2 = client("acme-bot-2", "acme-secret-2");
        globex = client("globex-bot", "globex-secret");
        runtime = client("runtime-1", "runtime-secret");
        ops = client("ops", "ops-secret");
        StatementCounter.reset();
    }

    @AfterEach
    void checkInvariants() {
        InvariantChecker.assertAll(jdbc);
    }

    /** One claim attempt on an inbox of tenant acme, without waiting. */
    protected ApiResult<ClaimedRun> claim(String workerId, String inboxId) {
        return runtime.claim(ClaimRequest.inbox(workerId, ACME, inboxId));
    }

    protected String write(String inboxId, WriteEventRequest.Builder event) {
        return acme.writeEvent(inboxId, event.build()).require().eventId();
    }

    protected InboxClient client(String clientId, String secret) {
        return new InboxClient(baseUri(), clientId, secret, clock::now, Duration.ofSeconds(70));
    }

    protected URI baseUri() {
        return URI.create("http://127.0.0.1:" + port);
    }

    /** A webhook event with the given idempotency key and payload. */
    protected static WriteEventRequest.Builder webhook(String sourceEventId, String payloadJson) {
        return WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "order.paid.v1")
                .sourceEventId(sourceEventId)
                .payloadJson(payloadJson);
    }

    /** A user message in conversation {@code conversation}. */
    protected static WriteEventRequest.Builder message(String sourceEventId, String conversation, String text) {
        return WriteEventRequest.builder(EventSource.CONVERSATION, "conversation.message.v1")
                .sourceEventId(sourceEventId)
                .mergeKey("conversation:" + conversation)
                .payloadJson("{\"text\":\"" + text.replace("\"", "\\\"") + "\"}");
    }

    protected long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    protected String eventStatus(String eventId) {
        return jdbc.sql("SELECT status FROM event WHERE event_id = :id").param("id", eventId).query(String.class).single();
    }
}
