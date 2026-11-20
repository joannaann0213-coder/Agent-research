package dev.agentflow.inbox.server.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class SystemInboxClock implements InboxClock {

    @Override
    public Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
