package dev.agentflow.agent.server.support;

import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.testkit.openapi.OpenApiTrafficFilter;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

/** Beans that make the agent service testable: the shared hand-driven clock, a scripted agent platform, a clean schema, a check of the traffic. */
@TestConfiguration(proxyBeanMethods = false)
public class AgentTestBeans {

    /** The agent service and the embedded Inbox read one clock, so a test moves time for both at once. */
    @Bean
    @Primary
    AgentClock sharedClock() {
        return () -> EmbeddedInbox.clock().now();
    }

    /** The agent platform of the tests: the scripted stand-in, on the shared clock. A test scripts it and looks into it. */
    @Bean
    @Primary
    FakeManagedAgents fakeManagedAgents() {
        return new FakeManagedAgents(() -> EmbeddedInbox.clock().now());
    }

    /** Every test JVM starts from an empty, freshly migrated schema. */
    @Bean
    FlywayMigrationStrategy cleanThenMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }

    /**
     * Checks every {@code /v1} exchange of the tests against the API specification. It sits in front
     * of the authentication filter, so the responses of that filter are checked as well.
     */
    @Bean
    FilterRegistrationBean<OpenApiTrafficFilter> openApiTrafficFilter() {
        FilterRegistrationBean<OpenApiTrafficFilter> registration =
                new FilterRegistrationBean<>(new OpenApiTrafficFilter(AgentTestBase.OPENAPI, "/v1/"));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
