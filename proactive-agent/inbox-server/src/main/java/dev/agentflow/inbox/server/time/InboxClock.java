package dev.agentflow.inbox.server.time;

import java.time.Instant;

/**
 * The only source of time in the service. Every timestamp written to the database and every "is it
 * due" comparison uses this clock, never the database's {@code now()}, so that leases, quiet windows,
 * backoff and timers can be driven deterministically in tests.
 *
 * <p>Nodes are expected to be NTP-synchronised. Clock skew can only make something eligible a little
 * early or late; ownership is decided by row state and the run epoch, never by time alone.
 */
public interface InboxClock {

    /** Current time, truncated to microseconds (the resolution PostgreSQL stores). */
    Instant now();
}
