package dev.agentflow.inbox.server.support;

import dev.agentflow.testkit.openapi.OpenApiTrafficFilter;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

/** Beans that make the service testable: a hand-driven clock, a clean schema, a statement counter, a check of the traffic. */
@TestConfiguration(proxyBeanMethods = false)
public class TestBeans {

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock();
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
     * Checks every {@code /v1} exchange of the tests against the API specification. It sits in front of
     * the authentication filter, so the responses of that filter are checked as well.
     */
    @Bean
    FilterRegistrationBean<OpenApiTrafficFilter> openApiTrafficFilter() {
        FilterRegistrationBean<OpenApiTrafficFilter> registration =
                new FilterRegistrationBean<>(new OpenApiTrafficFilter(IntegrationTestBase.OPENAPI, "/v1/"));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    @Bean
    static BeanPostProcessor statementCountingDataSource() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return bean instanceof DataSource dataSource ? StatementCounter.wrap(dataSource) : bean;
            }
        };
    }
}
