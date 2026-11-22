package dev.agentflow.agent.server.support;

import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.EventTail;
import dev.agentflow.agent.server.bridge.Patience;
import java.time.Duration;
import java.util.Optional;

/**
 * Waiting, in tests: nothing sleeps. The scripted platform has done all it is going to do by the
 * time a call to it returns, so when no event is there, none will come by waiting; the time the
 * turn would have waited passes on the tests' clock at once.
 */
public final class TestPatience implements Patience {

    @Override
    public Optional<CmaEvent> next(EventTail tail, Duration time) {
        Optional<CmaEvent> event = tail.poll(Duration.ZERO);
        if (event.isEmpty()) {
            EmbeddedInbox.clock().advance(time);
        }
        return event;
    }

    @Override
    public void pause(Duration time) {
        EmbeddedInbox.clock().advance(time);
    }
}
