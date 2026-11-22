package dev.agentflow.agent.server.support;

import dev.agentflow.agent.api.CreateAppRequest;
import dev.agentflow.agent.api.CreateKeyRequest;
import dev.agentflow.agent.api.CreatedKey;
import dev.agentflow.agent.api.IssuedToken;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.server.AgentServerApplication;
import dev.agentflow.agent.server.api.OpenApiController;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.support.InvariantChecker;
import dev.agentflow.inbox.server.support.MutableClock;
import dev.agentflow.testkit.openapi.OpenApiTraffic;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base of every database test of the agent service: the real service on a random port against a
 * real PostgreSQL, next to a real Event Inbox in the same JVM, with one clock that only moves when
 * the test moves it. Each test starts from empty tables in both services, and ends with a check of
 * the Inbox's stored state and of the HTTP traffic of both against their API specifications.
 *
 * <p>The database comes from {@code AGENT_TEST_JDBC_URL} (default: the local {@code agent_test}
 * database that {@code scripts/pg-local.sh} creates); the Inbox uses {@code INBOX_TEST_JDBC_URL}.
 */
@SpringBootTest(classes = AgentServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.name=" + AgentServerApplication.CONFIG_NAME)
@ActiveProfiles("test")
@Import(AgentTestBeans.class)
public abstract class AgentTestBase {

    /** The published API specification, and a record of the traffic the tests put through it. */
    public static final OpenApiTraffic OPENAPI = OpenApiTraffic.forClasspathSpec(OpenApiController.SPECIFICATION);

    protected static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0123456789";
    protected static final String ACME = "acme";
    protected static final String GLOBEX = "globex";

    static {
        EmbeddedInbox.start();
    }

    @DynamicPropertySource
    static void pointAtTheEmbeddedInbox(DynamicPropertyRegistry registry) {
        registry.add("agent.inbox.base-url", () -> EmbeddedInbox.baseUri().toString());
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcClient jdbc;

    /** The agent platform of the tests. */
    @Autowired
    protected FakeManagedAgents cma;

    protected final MutableClock clock = EmbeddedInbox.clock();

    /** The Agent API as an operator of the service. */
    protected TestApi admin;
    /** The Agent API without a credential. */
    protected TestApi anonymous;

    @BeforeEach
    void resetState() {
        jdbc.sql("TRUNCATE TABLE agent_profile, inbox_relay, app_user, api_key, app RESTART IDENTITY CASCADE").update();
        EmbeddedInbox.reset();
        cma.reset();
        clock.reset();
        anonymous = new TestApi(baseUri(), null);
        admin = anonymous.as(ADMIN_KEY);
    }

    @AfterEach
    void checkTheInboxInvariants() {
        InvariantChecker.assertAll(EmbeddedInbox.jdbc());
    }

    /** Every request the test made, and every response it got, has to match the published specification. */
    @AfterEach
    void checkTrafficAgainstTheSpecification() {
        OPENAPI.assertNoViolations();
    }

    /** The same for what the agent service asked of the Inbox. */
    @AfterEach
    void checkInboxTrafficAgainstItsSpecification() {
        IntegrationTestBase.OPENAPI.assertNoViolations();
    }

    protected URI baseUri() {
        return URI.create("http://127.0.0.1:" + port);
    }

    /** Registers an application and returns its API as that application's back end. */
    protected TestApi app(String appId) {
        admin.post("/v1/admin/apps", new CreateAppRequest(appId, appId + " app")).as(Object.class);
        return anonymous.as(newKey(appId));
    }

    protected String newKey(String appId) {
        return admin.post("/v1/admin/apps/" + appId + "/keys", new CreateKeyRequest("test")).as(CreatedKey.class).apiKey();
    }

    /** The Agent API as an end user's client, with a token the application got for that user. */
    protected TestApi user(TestApi app, String userId) {
        return anonymous.as(app.post("/v1/users/" + userId + "/tokens", null).as(IssuedToken.class).token());
    }

    protected long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }
}
