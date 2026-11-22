package dev.agentflow.agent.cma;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Keeps the calls to the platform within its rate limits, which hold for the whole organization:
 * so many creations a minute, and so many other requests. A call waits for its turn for a moment;
 * when that is not enough, it fails as {@link CmaException.Kind#RATE_LIMITED}, like a refusal by the
 * platform would, and the caller backs off. The limits are per instance of the service: with several,
 * give each its share.
 */
public final class ThrottledManagedAgents implements ManagedAgents {

    private static final class Bucket {
        private final double perNano;
        private final double size;
        private double tokens;
        private long at;

        Bucket(int perMinute, long now) {
            this.perNano = perMinute / 60e9;
            this.size = Math.max(1, perMinute / 6.0);
            this.tokens = size;
            this.at = now;
        }

        synchronized long take(long now) {
            tokens = Math.min(size, tokens + (now - at) * perNano);
            at = now;
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            return (long) Math.ceil((1 - tokens) / perNano);
        }

        synchronized boolean low(long now) {
            return Math.min(size, tokens + (now - at) * perNano) < 1;
        }
    }

    private final ManagedAgents platform;
    private final Bucket creations;
    private final Bucket requests;
    private final Duration maxWait;
    private final LongSupplier nanos;

    /**
     * @param creationsPerMinute what the platform allows for creating agents, environments, sessions, vaults and credentials
     * @param requestsPerMinute what it allows for everything else
     * @param maxWait how long a call waits for its turn before it gives up
     */
    public ThrottledManagedAgents(ManagedAgents platform, int creationsPerMinute, int requestsPerMinute, Duration maxWait) {
        this(platform, creationsPerMinute, requestsPerMinute, maxWait, System::nanoTime);
    }

    ThrottledManagedAgents(ManagedAgents platform, int creationsPerMinute, int requestsPerMinute, Duration maxWait, LongSupplier nanos) {
        this.platform = platform;
        this.nanos = nanos;
        this.creations = new Bucket(creationsPerMinute, nanos.getAsLong());
        this.requests = new Bucket(requestsPerMinute, nanos.getAsLong());
        this.maxWait = maxWait;
    }

    /** Whether the platform's limits are reached for now: a good moment not to take on new work. */
    public boolean saturated() {
        long now = nanos.getAsLong();
        return creations.low(now) || requests.low(now);
    }

    private void await(Bucket bucket) {
        long deadline = nanos.getAsLong() + maxWait.toNanos();
        while (true) {
            long wait = bucket.take(nanos.getAsLong());
            if (wait == 0) {
                return;
            }
            if (nanos.getAsLong() + wait > deadline) {
                throw CmaException.rateLimited(Duration.ofNanos(wait));
            }
            try {
                Thread.sleep(Duration.ofNanos(wait));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw CmaException.connection("interrupted while waiting for the platform's rate limit", e);
            }
        }
    }

    private void creation() {
        await(creations);
    }

    private void request() {
        await(requests);
    }

    @Override
    public String createEnvironment(EnvironmentSpec spec) {
        creation();
        return platform.createEnvironment(spec);
    }

    @Override
    public void updateEnvironment(String environmentId, EnvironmentSpec spec) {
        request();
        platform.updateEnvironment(environmentId, spec);
    }

    @Override
    public AgentRef createAgent(AgentSpec spec) {
        creation();
        return platform.createAgent(spec);
    }

    @Override
    public AgentRef updateAgent(AgentRef current, AgentSpec spec) {
        request();
        return platform.updateAgent(current, spec);
    }

    @Override
    public AgentRef getAgent(String agentId) {
        request();
        return platform.getAgent(agentId);
    }

    @Override
    public String createVault(String displayName, Map<String, String> metadata) {
        creation();
        return platform.createVault(displayName, metadata);
    }

    @Override
    public String createCredential(String vaultId, McpCredential credential) {
        creation();
        return platform.createCredential(vaultId, credential);
    }

    @Override
    public void updateCredential(String vaultId, String credentialId, String bearerToken) {
        request();
        platform.updateCredential(vaultId, credentialId, bearerToken);
    }

    @Override
    public void archiveCredential(String vaultId, String credentialId) {
        request();
        platform.archiveCredential(vaultId, credentialId);
    }

    @Override
    public List<McpCredential.Stored> listCredentials(String vaultId) {
        request();
        return platform.listCredentials(vaultId);
    }

    @Override
    public String createMemoryStore(String name, String description, Map<String, String> metadata) {
        creation();
        return platform.createMemoryStore(name, description, metadata);
    }

    @Override
    public String createSession(SessionSpec spec) {
        creation();
        return platform.createSession(spec);
    }

    @Override
    public SessionInfo getSession(String sessionId) {
        request();
        return platform.getSession(sessionId);
    }

    @Override
    public List<CmaEvent> listEvents(String sessionId, Instant createdSince) {
        request();
        return platform.listEvents(sessionId, createdSince);
    }

    @Override
    public EventStream openStream(String sessionId) {
        request();
        return platform.openStream(sessionId);
    }

    @Override
    public List<CmaEvent> send(String sessionId, List<Outgoing> events) {
        request();
        return platform.send(sessionId, events);
    }
}
