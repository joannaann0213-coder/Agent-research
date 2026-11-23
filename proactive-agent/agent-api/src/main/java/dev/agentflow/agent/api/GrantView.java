package dev.agentflow.agent.api;

import java.time.Instant;

/**
 * A user's standing authorisation: the main agent may start the tasks of one role on its own
 * initiative, without asking the user each time.
 *
 * @param agent the key of the main agent
 * @param role the role of its list of workers
 * @param grantedBy {@code user} when the user's own client gave it, {@code app} when the
 *     application did for them
 */
public record GrantView(String agent, String role, String grantedBy, Instant createdAt) {
}
