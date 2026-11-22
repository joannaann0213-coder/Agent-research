package dev.agentflow.agent.server.config;

import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.client.InboxClient;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InboxConfig {

    /** The client this service uses for the Event Inbox: it writes events as a producer and claims runs as a runtime. */
    @Bean
    InboxClient inboxClient(AgentProperties properties, AgentClock clock) {
        AgentProperties.Inbox inbox = properties.getInbox();
        if (inbox.getSecret() == null || inbox.getSecret().isBlank()) {
            throw new IllegalStateException("agent.inbox.secret is not set: it is the secret of this service's "
                    + "client in the Event Inbox (environment variable AGENT_INBOX_SECRET)");
        }
        return new InboxClient(URI.create(inbox.getBaseUrl()), inbox.getClientId(), inbox.getSecret(), clock::now,
                inbox.getRequestTimeout());
    }
}
