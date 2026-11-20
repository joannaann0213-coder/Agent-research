package dev.agentflow.inbox.api;

/** State of a run. Wire form is the lower-case name. */
public enum RunStatus {
    /** Holds a lane slot and a lease. */
    ACTIVE,
    /** Waiting to be re-issued after a nack, a lease expiry or a suspend timeout. */
    QUEUED,
    /** Waiting for an event that carries its resume key. Holds no slot and no lease. */
    SUSPENDED,
    DONE, FAILED, DEAD;

    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static RunStatus fromWire(String value) {
        return valueOf(value.toUpperCase(java.util.Locale.ROOT));
    }

    public boolean isOpen() {
        return this == ACTIVE || this == QUEUED || this == SUSPENDED;
    }
}
