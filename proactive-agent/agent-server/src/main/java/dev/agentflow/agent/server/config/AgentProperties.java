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
    private final Sweep sweep = new Sweep();
    private final Notify notify = new Notify();
    private final Actions actions = new Actions();
    private final Followups followups = new Followups();
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

    public Sweep getSweep() {
        return sweep;
    }

    public Notify getNotify() {
        return notify;
    }

    public Actions getActions() {
        return actions;
    }

    public Followups getFollowups() {
        return followups;
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

        /** How long a read of the session's stream waits before the turn looks at its lease and its deadline again. */
        private Duration poll = Duration.ofSeconds(1);

        /**
         * How long a turn may take from its first attempt. At the deadline the session is interrupted
         * and the turn fails. It must be shorter than the Inbox's longest lease (inbox.lease.max-duration).
         */
        private Duration deadline = Duration.ofMinutes(10);

        /** How often a call to the agent platform is tried before the run goes back to the Inbox. */
        private int attempts = 4;

        /** The delay before the second attempt of a call. It doubles with every further attempt. */
        private Duration retryDelay = Duration.ofSeconds(1);

        private Duration maxRetryDelay = Duration.ofSeconds(30);

        /** How long the Inbox holds a run back after this service gave up on the platform for now. */
        private Duration nackDelay = Duration.ofSeconds(30);

        /** How long a session that was left busy gets to come to rest after an interrupt, before it is replaced. */
        private Duration settleWait = Duration.ofSeconds(30);

        /** How many earlier messages a session that replaces another is told about. */
        private int transcriptMessages = 20;

        /** How long a run waits before it tries again when an earlier turn of its line has to finish first. */
        private Duration lineBusyDelay = Duration.ofSeconds(2);

        public Duration getLineBusyDelay() {
            return lineBusyDelay;
        }

        public void setLineBusyDelay(Duration lineBusyDelay) {
            this.lineBusyDelay = lineBusyDelay;
        }

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

        public Duration getDeadline() {
            return deadline;
        }

        public void setDeadline(Duration deadline) {
            this.deadline = deadline;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public Duration getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
        }

        public Duration getMaxRetryDelay() {
            return maxRetryDelay;
        }

        public void setMaxRetryDelay(Duration maxRetryDelay) {
            this.maxRetryDelay = maxRetryDelay;
        }

        public Duration getNackDelay() {
            return nackDelay;
        }

        public void setNackDelay(Duration nackDelay) {
            this.nackDelay = nackDelay;
        }

        public Duration getSettleWait() {
            return settleWait;
        }

        public void setSettleWait(Duration settleWait) {
            this.settleWait = settleWait;
        }

        public int getTranscriptMessages() {
            return transcriptMessages;
        }

        public void setTranscriptMessages(int transcriptMessages) {
            this.transcriptMessages = transcriptMessages;
        }
    }

    /** The sweep for turns whose run ended without this service. */
    public static class Sweep {

        private Duration interval = Duration.ofSeconds(30);

        /** How long an open turn is left alone before the sweep asks the Inbox about its run. */
        private Duration grace = Duration.ofMinutes(2);

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public Duration getGrace() {
            return grace;
        }

        public void setGrace(Duration grace) {
            this.grace = grace;
        }
    }

    /** What the agent waits for people to decide. */
    public static class Actions {

        /**
         * How long an approval or a question waits for a decision. When nobody decides in time, the
         * agent is told so and goes on. The Inbox suspends a run for at most inbox.suspend.max-timeout.
         */
        private Duration ttl = Duration.ofHours(24);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    /** The follow-ups an agent sets itself. */
    public static class Followups {

        /** How many follow-ups may come one after the other, each set by the turn of the one before. */
        private int maxChain = 5;

        /** How many follow-ups may wait on one line at a time. */
        private int maxPending = 5;

        /** How far ahead a follow-up may be set. */
        private Duration maxDelay = Duration.ofDays(30);

        public int getMaxChain() {
            return maxChain;
        }

        public void setMaxChain(int maxChain) {
            this.maxChain = maxChain;
        }

        public int getMaxPending() {
            return maxPending;
        }

        public void setMaxPending(int maxPending) {
            this.maxPending = maxPending;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }
    }

    /** What an agent may send a user of its own accord. */
    public static class Notify {

        /** How many notifications one run may send. */
        private int perRun = 3;

        /** How many notifications a user gets within 24 hours, from all agents of the application together. */
        private int perUserPerDay = 20;

        /** The longest text of one notification, in characters. */
        private int maxText = 4000;

        public int getPerRun() {
            return perRun;
        }

        public void setPerRun(int perRun) {
            this.perRun = perRun;
        }

        public int getPerUserPerDay() {
            return perUserPerDay;
        }

        public void setPerUserPerDay(int perUserPerDay) {
            this.perUserPerDay = perUserPerDay;
        }

        public int getMaxText() {
            return maxText;
        }

        public void setMaxText(int maxText) {
            this.maxText = maxText;
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
