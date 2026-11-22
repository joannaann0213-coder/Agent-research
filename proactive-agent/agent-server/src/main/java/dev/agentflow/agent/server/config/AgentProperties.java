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
    private final Relay relay = new Relay();
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

    public Relay getRelay() {
        return relay;
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
