package dev.agentflow.inbox.server.support;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Beans that make the service testable: a hand-driven clock, a clean schema, a statement counter. */
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
