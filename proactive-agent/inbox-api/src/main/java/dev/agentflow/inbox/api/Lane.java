package dev.agentflow.inbox.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Priority channel of an inbox. Each lane has its own concurrency cap; a lower rank is claimed first.
 */
public enum Lane {
    INTERACTIVE("interactive", 0),
    BACKGROUND("background", 1),
    BATCH("batch", 2);

    private final String wire;
    private final int rank;

    Lane(String wire, int rank) {
        this.wire = wire;
        this.rank = rank;
    }

    @JsonValue
    public String wire() {
        return wire;
    }

    public int rank() {
        return rank;
    }

    @JsonCreator
    public static Lane fromWire(String value) {
        for (Lane lane : values()) {
            if (lane.wire.equals(value)) {
                return lane;
            }
        }
        throw new IllegalArgumentException("unknown lane: " + value);
    }

    /** Lane used when a producer does not name one. */
    public static Lane defaultFor(EventSource source) {
        return switch (source) {
            case CONVERSATION, CONNECTOR, BIZ_WEBHOOK -> INTERACTIVE;
            case TIMER, AGENT_SELF -> BACKGROUND;
            case RULE -> BATCH;
        };
    }
}
