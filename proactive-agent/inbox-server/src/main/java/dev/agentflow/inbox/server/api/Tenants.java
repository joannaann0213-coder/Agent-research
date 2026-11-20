package dev.agentflow.inbox.server.api;

import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import org.springframework.util.StringUtils;

/** Works out which tenant a request addresses. */
public final class Tenants {

    private Tenants() {
    }

    /**
     * A tenant client always acts inside its own tenant. A platform-scope client (the runtime, an
     * operator) must name the tenant.
     */
    public static String resolve(AuthenticatedClient client, String requested) {
        if (client.tenantId() != null) {
            if (StringUtils.hasText(requested) && !client.tenantId().equals(requested)) {
                throw InboxException.forbidden("this client belongs to another tenant");
            }
            return client.tenantId();
        }
        if (!StringUtils.hasText(requested)) {
            throw InboxException.invalid("tenant_id is required for a platform-scope client");
        }
        return requested;
    }

    /** Tenant filter for a list or claim: null means "all tenants" and is allowed only at platform scope. */
    public static String filter(AuthenticatedClient client, String requested) {
        if (client.tenantId() != null) {
            return resolve(client, requested);
        }
        return StringUtils.hasText(requested) ? requested : null;
    }
}
