package dev.agentflow.agent.cma;

import java.time.Duration;
import java.util.Optional;

/** The live events of one session, from the moment the stream was opened. Not thread-safe. */
public interface EventStream extends AutoCloseable {

    /**
     * The next event, waiting up to {@code timeout} for one.
     *
     * @return empty when nothing arrived in time; the stream is still good
     * @throws CmaException with kind {@link CmaException.Kind#CONNECTION} when the stream is lost or
     *     has ended; open a new one
     */
    Optional<CmaEvent> poll(Duration timeout);

    @Override
    void close();
}
