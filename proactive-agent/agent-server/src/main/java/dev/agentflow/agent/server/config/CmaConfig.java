package dev.agentflow.agent.server.config;

import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.sdk.SdkManagedAgents;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.agent.server.tools.DemoAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CmaConfig {

    private static final Logger log = LoggerFactory.getLogger(CmaConfig.class);

    /**
     * The agent platform. In {@code live} mode it is Claude Managed Agents, reached with the
     * configured API key. In {@code demo} mode it is an in-process stand-in whose agents are played
     * by {@link DemoAgent}: the whole service can be tried without a key, and no model is ever called.
     */
    @Bean
    ManagedAgents managedAgents(AgentProperties properties, AgentClock clock) {
        AgentProperties.Cma cma = properties.getCma();
        switch (cma.getMode()) {
            case "demo" -> {
                log.warn("agent.cma.mode is demo: agents are played by a script, no model is called");
                FakeManagedAgents demo = new FakeManagedAgents(clock::now);
                demo.brain(new DemoAgent());
                return demo;
            }
            case "live" -> {
                if (cma.getApiKey() == null || cma.getApiKey().isBlank()) {
                    throw new IllegalStateException("agent.cma.api-key is not set (environment variable "
                            + "ANTHROPIC_API_KEY). To try the service without a key, set agent.cma.mode to demo "
                            + "(AGENT_CMA_MODE=demo)");
                }
                return SdkManagedAgents.connect(cma.getApiKey(), cma.getBaseUrl(), cma.getRequestTimeout());
            }
            default -> throw new IllegalStateException("agent.cma.mode must be live or demo, not " + cma.getMode());
        }
    }
}
