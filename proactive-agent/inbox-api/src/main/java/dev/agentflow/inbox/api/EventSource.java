package dev.agentflow.inbox.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Who produced an event. */
public enum EventSource {
    BIZ_WEBHOOK("biz_webhook"),
    CONNECTOR("connector"),
    TIMER("timer"),
    CONVERSATION("conversation"),
    RULE("rule"),
    AGENT_SELF("agent_self");

    private final String wire;

    EventSource(String wire) {
        this.wire = wire;
    }

    @JsonValue
    public String wire() {
        return wire;
    }

    @JsonCreator
    public static EventSource fromWire(String value) {
        for (EventSource source : values()) {
            if (source.wire.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("unknown source: " + value);
    }
}
