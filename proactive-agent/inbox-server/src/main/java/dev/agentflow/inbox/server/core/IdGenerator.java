package dev.agentflow.inbox.server.core;

import dev.agentflow.inbox.server.time.InboxClock;
import org.springframework.stereotype.Component;

/** Mints ids for events, runs, timers and audit rows. */
@Component
public class IdGenerator {

    private final Ulid ulid = new Ulid();
    private final InboxClock clock;

    public IdGenerator(InboxClock clock) {
        this.clock = clock;
    }

    public String newId() {
        return ulid.next(clock.now().toEpochMilli());
    }
}
