package dev.agentflow.agent.server.config;

import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.ThrottledManagedAgents;
import dev.agentflow.agent.cma.sdk.SdkManagedAgents;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.agent.server.tools.DemoAgent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
    ManagedAgents managedAgents(AgentProperties properties, AgentClock clock, ObjectProvider<AgentProfiles> profiles) {
        AgentProperties.Cma cma = properties.getCma();
        switch (cma.getMode()) {
            case "demo" -> {
                log.warn("agent.cma.mode is demo: agents are played by a script, no model is called");
                FakeManagedAgents demo = new FakeManagedAgents(clock::now);
                demo.brain(new DemoAgent());
                // This platform lives in memory and forgets what it made when the service restarts,
                // while the database still names it: it takes that back, an agent with the
                // configuration of its profile, and makes no id that it made before.
                demo.restoreForgotten(agentId -> profiles.getObject().platformAgent(agentId));
                demo.startIdsAt(System.currentTimeMillis());
                return demo;
            }
            case "live" -> {
                if (cma.getApiKey() == null || cma.getApiKey().isBlank()) {
                    throw new IllegalStateException("agent.cma.api-key is not set (environment variable "
                            + "ANTHROPIC_API_KEY). To try the service without a key, set agent.cma.mode to demo "
                            + "(AGENT_CMA_MODE=demo)");
                }
                refuseExampleSecrets(properties);
                // The platform's rate limits hold for the whole organization; this keeps the service inside them.
                return new ThrottledManagedAgents(SdkManagedAgents.connect(cma.getApiKey(), cma.getBaseUrl(), cma.getRequestTimeout()),
                        cma.getCreationsPerMinute(), cma.getRequestsPerMinute(), cma.getThrottleWait());
            }
            default -> throw new IllegalStateException("agent.cma.mode must be live or demo, not " + cma.getMode());
        }
    }

    /**
     * The documentation and docker-compose.yml show example secrets, which start with {@code change-me},
     * so that the service can be tried at once. In live mode it holds real conversations and spends real
     * money, so it does not start with a secret that everybody who has read the documentation knows.
     */
    static void refuseExampleSecrets(AgentProperties properties) {
        List<String> examples = new ArrayList<>();
        if (properties.getAdmin().getKeys().stream().anyMatch(CmaConfig::isExample)) {
            examples.add("AGENT_ADMIN_KEYS_0 (agent.admin.keys)");
        }
        if (properties.getTokens().getSecrets().stream().anyMatch(CmaConfig::isExample)) {
            examples.add("AGENT_TOKENS_SECRETS_0 (agent.tokens.secrets)");
        }
        if (isExample(properties.getInbox().getSecret())) {
            examples.add("AGENT_INBOX_SECRET (agent.inbox.secret)");
        }
        if (!examples.isEmpty()) {
            throw new IllegalStateException("live mode needs secrets of your own, not the examples of the documentation: set "
                    + String.join(", ", examples) + "; make each with openssl rand -hex 32");
        }
    }

    private static boolean isExample(String secret) {
        return secret != null && secret.startsWith("change-me");
    }
}
