package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.EventTail;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * How a turn waits: for the next event of its session, and between two attempts of a call. It is
 * an interface so that tests can let time pass by moving their clock instead of sleeping.
 */
public interface Patience {

    /** The tail's next event, waiting up to {@code time} for one. */
    Optional<CmaEvent> next(EventTail tail, Duration time);

    /** Lets {@code time} pass. */
    void pause(Duration time);

    /** Really waits. */
    @Component
    class Real implements Patience {

        @Override
        public Optional<CmaEvent> next(EventTail tail, Duration time) {
            return tail.poll(time);
        }

        @Override
        public void pause(Duration time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting", e);
            }
        }
    }
}
