package dev.agentflow.agent.server.core;

import dev.agentflow.agent.server.time.AgentClock;
import org.springframework.stereotype.Component;

/** Hands out ids: a short prefix that says what the id is for, and a ULID. */
@Component
public class Ids {

    private final Ulid ulid = new Ulid();
    private final AgentClock clock;

    public Ids(AgentClock clock) {
        this.clock = clock;
    }

    /** For example {@code key_01M7WZZSCQPP6KTQPVXCBECVHK}. */
    public String next(String prefix) {
        return prefix + "_" + ulid.next(clock.now().toEpochMilli());
    }
}
