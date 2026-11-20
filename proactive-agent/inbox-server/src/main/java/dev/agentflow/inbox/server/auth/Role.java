package dev.agentflow.inbox.server.auth;

/** What a client may do. */
public enum Role {
    /** Writes events and timers into inboxes of its tenant and reads back its own events. */
    PRODUCER,
    /** The agent runtime: claims runs and drives their lifecycle. */
    RUNTIME,
    /** Replays, dead-letters, reconfigures and reads the audit log. */
    OPERATOR
}
