package dev.agentflow.agent.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Runs the background loops on a timer. With {@code agent.scheduling.enabled=false} nothing runs by itself. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "agent.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
