package dev.agentflow.agent.server.time;

import java.time.Instant;

/**
 * The only source of time in the service. Every timestamp written to the database and every "is it
 * due" comparison uses this clock, never the database's {@code now()}, so that expiry, deadlines and
 * backoff can be driven deterministically in tests.
 */
public interface AgentClock {

    /** Current time, truncated to microseconds (the resolution PostgreSQL stores). */
    Instant now();
}
