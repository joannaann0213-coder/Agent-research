package dev.agentflow.agent.server.support;

import dev.agentflow.inbox.client.InboxClient;
import dev.agentflow.inbox.server.InboxServerApplication;
import dev.agentflow.inbox.server.support.MutableClock;
import dev.agentflow.inbox.server.support.TestBeans;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * A real Event Inbox in the test JVM, started once and shared by every test. The agent service
 * under test talks to it over HTTP, as it does in production; the tests reach into it only for what
 * a test needs to control or check: its clock, its background loops and its tables.
 *
 * <p>It runs with the Inbox's own test set-up: a clock that only moves when a test moves it, a
 * freshly migrated schema, and a check of every request against the Inbox's API specification.
 */
public final class EmbeddedInbox {

    /** The client the agent service uses. It has no tenant of its own and both roles the service needs. */
    public static final String AGENT_CLIENT = "agent-server";
    public static final String AGENT_SECRET = "agent-inbox-secret";

    private static ConfigurableApplicationContext context;

    private EmbeddedInbox() {
    }

    public static synchronized void start() {
        if (context != null) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.port", "0");
        properties.put("spring.datasource.url",
                System.getenv().getOrDefault("INBOX_TEST_JDBC_URL", "jdbc:postgresql://127.0.0.1:5432/inbox_test"));
        properties.put("spring.datasource.username", System.getenv().getOrDefault("INBOX_TEST_DB_USER", "inbox"));
        properties.put("spring.datasource.password", System.getenv().getOrDefault("INBOX_TEST_DB_PASSWORD", "inbox"));
        properties.put("spring.datasource.hikari.maximum-pool-size", "20");
        properties.put("spring.flyway.clean-disabled", "false");
        properties.put("inbox.scheduling.enabled", "false");
        properties.put("inbox.auth.clients[0].id", AGENT_CLIENT);
        properties.put("inbox.auth.clients[0].secret", AGENT_SECRET);
        properties.put("inbox.auth.clients[0].roles", "RUNTIME,PRODUCER");
        properties.put("inbox.auth.clients[1].id", "ops");
        properties.put("inbox.auth.clients[1].secret", "ops-secret");
        properties.put("inbox.auth.clients[1].roles", "OPERATOR");
        context = new SpringApplicationBuilder(InboxServerApplication.class, TestBeans.class)
                .properties(properties)
                .run();
    }

    public static int port() {
        return Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
    }

    public static URI baseUri() {
        return URI.create("http://127.0.0.1:" + port());
    }

    /** The one clock of the test JVM. The agent service under test reads the same one. */
    public static MutableClock clock() {
        return context.getBean(MutableClock.class);
    }

    public static JdbcClient jdbc() {
        return context.getBean(JdbcClient.class);
    }

    public static <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    /** An operator's client, for looking at what arrived. */
    public static InboxClient operator() {
        return new InboxClient(baseUri(), "ops", "ops-secret", clock()::now, Duration.ofSeconds(30));
    }

    /** Empties every table of the Inbox. */
    public static void reset() {
        jdbc().sql("TRUNCATE TABLE event_transition, admin_audit, timer, event, run, inbox RESTART IDENTITY CASCADE").update();
    }
}
