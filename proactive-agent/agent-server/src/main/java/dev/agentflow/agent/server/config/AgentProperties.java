package dev.agentflow.agent.server.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Everything configurable about the agent service, under {@code agent.*}. */
@ConfigurationProperties("agent")
public class AgentProperties {

    private final Admin admin = new Admin();
    private final Tokens tokens = new Tokens();
    private final Inbox inbox = new Inbox();
    private final Cma cma = new Cma();
    private final Relay relay = new Relay();
    private final Worker worker = new Worker();
    private final Turn turn = new Turn();
    private final Scheduling scheduling = new Scheduling();

    public Admin getAdmin() {
        return admin;
    }

    public Tokens getTokens() {
        return tokens;
    }

    public Inbox getInbox() {
        return inbox;
    }

    public Cma getCma() {
        return cma;
    }

    public Relay getRelay() {
        return relay;
    }

    public Worker getWorker() {
        return worker;
    }

    public Turn getTurn() {
        return turn;
    }

    public Scheduling getScheduling() {
        return scheduling;
    }

    /** The administration API. */
    public static class Admin {

        /** Keys that may call {@code /v1/admin}. Without one the administration API is closed. */
        private List<String> keys = new ArrayList<>();

        public List<String> getKeys() {
            return keys;
        }

        public void setKeys(List<String> keys) {
            this.keys = keys;
        }
    }

    /** Tokens for end users. */
    public static class Tokens {

        /**
         * Secrets that sign user tokens. The first one signs; every one verifies, so a secret can be
         * replaced without invalidating the tokens that are out.
         */
        private List<String> secrets = new ArrayList<>();

        /** How long a token is valid when the request does not say. */
        private Duration defaultTtl = Duration.ofMinutes(15);

        /** The longest a token may be valid. */
        private Duration maxTtl = Duration.ofHours(1);

        public List<String> getSecrets() {
            return secrets;
        }

        public void setSecrets(List<String> secrets) {
            this.secrets = secrets;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getMaxTtl() {
            return maxTtl;
        }

        public void setMaxTtl(Duration maxTtl) {
            this.maxTtl = maxTtl;
        }
    }

    /** The Event Inbox this service writes events to and claims runs from. */
    public static class Inbox {

        private String baseUrl = "http://127.0.0.1:8080";

        /** This service's client in the Inbox. It needs the roles RUNTIME and PRODUCER and no tenant of its own. */
        private String clientId = "agent-server";

        private String secret;

        private Duration requestTimeout = Duration.ofSeconds(40);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    /** The agent platform, Claude Managed Agents. */
    public static class Cma {

        /** {@code live} talks to the real service; {@code demo} runs a scripted stand-in and needs no key. */
        private String mode = "live";

        private String apiKey;

        /** Set on purpose and not taken from ANTHROPIC_BASE_URL, which may point elsewhere on a developer's machine. */
        private String baseUrl = "https://api.anthropic.com";

        private Duration requestTimeout = Duration.ofSeconds(60);

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    /** Delivery of events to the Inbox. */
    public static class Relay {

        /** Whether an event is relayed right after the transaction that queued it, without waiting for the loop. */
        private boolean immediate = true;

        /** How often the loop looks for events that are still waiting. */
        private Duration interval = Duration.ofSeconds(1);

        private int batchSize = 50;

        /** The delay before the second attempt. It doubles with every further attempt. */
        private Duration backoff = Duration.ofSeconds(1);

        private Duration maxBackoff = Duration.ofMinutes(1);

        public boolean isImmediate() {
            return immediate;
        }

        public void setImmediate(boolean immediate) {
            this.immediate = immediate;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getBackoff() {
            return backoff;
        }

        public void setBackoff(Duration backoff) {
            this.backoff = backoff;
        }

        public Duration getMaxBackoff() {
            return maxBackoff;
        }

        public void setMaxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
        }
    }

    /** The workers that claim runs from the Inbox. */
    public static class Worker {

        /** The name this instance claims under. It shows in the Inbox's records; a random one when absent. */
        private String id;

        /** How many turns this instance works on at once. */
        private int concurrency = 8;

        /** How long a claim waits in the Inbox for work before it asks again. */
        private Duration claimWait = Duration.ofSeconds(20);

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public Duration getClaimWait() {
            return claimWait;
        }

        public void setClaimWait(Duration claimWait) {
            this.claimWait = claimWait;
        }
    }

    /** One turn of a session. */
    public static class Turn {

        /** How often the run's lease in the Inbox is renewed. A third of the Inbox's lease duration is a good value. */
        private Duration heartbeat = Duration.ofSeconds(20);

        /** How long a read of the session's stream waits before the turn looks at its lease again. */
        private Duration poll = Duration.ofSeconds(1);

        public Duration getHeartbeat() {
            return heartbeat;
        }

        public void setHeartbeat(Duration heartbeat) {
            this.heartbeat = heartbeat;
        }

        public Duration getPoll() {
            return poll;
        }

        public void setPoll(Duration poll) {
            this.poll = poll;
        }
    }

    /** The background loops. */
    public static class Scheduling {

        /** Off in tests, which run each loop by hand, one pass at a time. */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
