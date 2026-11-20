package dev.agentflow.inbox.server.auth;

import java.util.Set;

/**
 * The caller of a request.
 *
 * @param tenantId the tenant the client belongs to, or null for a platform-scope client
 */
public record AuthenticatedClient(String clientId, String tenantId, Set<Role> roles) {

    public static final String REQUEST_ATTRIBUTE = AuthenticatedClient.class.getName();

    public boolean has(Role role) {
        return roles.contains(role);
    }

    /** Name recorded in transition and audit rows. */
    public String actor() {
        return "client:" + clientId;
    }
}
