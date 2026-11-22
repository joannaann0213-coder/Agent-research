package dev.agentflow.agent.cma;

import java.util.Map;

/**
 * A session's current state. Why an idle session is idle is not part of it: that is in its last
 * {@link CmaEvent.Idle} event.
 */
public record SessionInfo(String id, Status status, boolean archived, AgentRef agent, Map<String, String> metadata) {

    public enum Status {
        IDLE,
        RUNNING,
        RESCHEDULING,
        /** Ended for good. A new session is needed. */
        TERMINATED,
        UNKNOWN
    }

    /** Whether the session can still take events. */
    public boolean usable() {
        return !archived && status != Status.TERMINATED;
    }
}
