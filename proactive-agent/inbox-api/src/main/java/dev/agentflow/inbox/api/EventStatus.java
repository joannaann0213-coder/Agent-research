package dev.agentflow.inbox.api;

/**
 * Delivery state of an event row. Wire form is the lower-case name. A deferred event that has not
 * fired yet has no row; the API reports it as {@code scheduled}.
 */
public enum EventStatus {
    PENDING, CLAIMED, DONE, FAILED, DEAD;

    public static final String SCHEDULED = "scheduled";

    public String wire() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static EventStatus fromWire(String value) {
        return valueOf(value.toUpperCase(java.util.Locale.ROOT));
    }

    public boolean isFinal() {
        return this == DONE || this == FAILED || this == DEAD;
    }
}
