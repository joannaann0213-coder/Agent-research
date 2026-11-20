package dev.agentflow.inbox.server.auth;

import dev.agentflow.inbox.server.config.InboxProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Known clients and their secrets. In this build they come from configuration (in practice from the
 * environment or a secret store); a self-service, database-backed registry belongs to roadmap stage M2.
 */
@Component
public class ClientRegistry {

    public record Entry(AuthenticatedClient client, String secret) {
    }

    private final Map<String, Entry> byId = new HashMap<>();

    public ClientRegistry(InboxProperties properties) {
        for (InboxProperties.Client config : properties.getAuth().getClients()) {
            if (!StringUtils.hasText(config.getId()) || !StringUtils.hasText(config.getSecret())) {
                throw new IllegalStateException("inbox.auth.clients: every client needs an id and a secret");
            }
            String tenant = StringUtils.hasText(config.getTenant()) ? config.getTenant() : null;
            AuthenticatedClient client = new AuthenticatedClient(config.getId(), tenant, Set.copyOf(config.getRoles()));
            if (byId.put(config.getId(), new Entry(client, config.getSecret())) != null) {
                throw new IllegalStateException("inbox.auth.clients: duplicate client id " + config.getId());
            }
        }
    }

    public Optional<Entry> find(String clientId) {
        return Optional.ofNullable(byId.get(clientId));
    }
}
