package dev.agentflow.agent.server.relay;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Picks up the events that the relay could not deliver right after their commit. */
@Component
public class RelayLoop {

    private final InboxRelay relay;

    public RelayLoop(InboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${agent.relay.interval:1s}")
    void tick() {
        relay.runOnce();
    }
}
