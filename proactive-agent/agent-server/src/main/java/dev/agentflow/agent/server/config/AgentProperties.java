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
    private final Tasks tasks = new Tasks();
    private final Webhooks webhooks = new Webhooks();
    private final Stream stream = new Stream();
    private final Limits limits = new Limits();
    private final Scheduling scheduling = new Scheduling();
    private final Cors cors = new Cors();

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

    public Tasks getTasks() {
        return tasks;
    }

    public Webhooks getWebhooks() {
        return webhooks;
    }

    public Stream getStream() {
        return stream;
    }

    public Limits getLimits() {
        return limits;
    }

    public Scheduling getScheduling() {
        return scheduling;
    }

    public Cors getCors() {
        return cors;
    }

    /** Web pages that call the API from the browser. */
    public static class Cors {

        /**
         * Origins of web pages, such as {@code https://app.example.com}, that may call the API from
         * the browser, with a user token. Without one, a browser reaches the API only from its own origin.
         */
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
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

        /**
         * The platform's rate limits, which hold for the whole organization: creations (of agents,
         * environments, sessions, vaults, credentials) and other requests, per minute. With several
         * instances of this service, give each its share.
         */
        private int creationsPerMinute = 300;
        private int requestsPerMinute = 600;

        /** How long a call waits for its turn under those limits before it is treated as refused. */
        private Duration throttleWait = Duration.ofSeconds(5);

        public int getCreationsPerMinute() {
            return creationsPerMinute;
        }

        public void setCreationsPerMinute(int creationsPerMinute) {
            this.creationsPerMinute = creationsPerMinute;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public Duration getThrottleWait() {
            return throttleWait;
        }

        public void setThrottleWait(Duration throttleWait) {
            this.throttleWait = throttleWait;
        }

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

        /**
         * How long a claim waits in the Inbox for work before it asks again. Short, because an instance
         * that stops lets its claims under way finish first.
         */
        private Duration claimWait = Duration.ofSeconds(5);

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

        /**
         * How often one turn in the background gives way to a message of the user on the same
         * line, starting over afterwards. 0 turns it off: the user's message then waits for the turn.
         */
        private int maxYields = 2;

        /**
         * How long a turn that gave way stays out of the way before it starts over. Longer than the
         * Inbox's quiet window, so that the user's message is claimed first.
         */
        private Duration yieldDelay = Duration.ofSeconds(5);

        /** A turn gives way only to a message of the user that is this recent: an older one is not somebody waiting. */
        private Duration yieldWindow = Duration.ofMinutes(2);

        /**
         * Whose reply is ended when the user writes again before it is there, so that the next turn
         * answers everything they have written in one reply: {@code main} for the conversations that
         * a main agent serves, {@code all} for every conversation, {@code none} for no reply, so that
         * each is finished and the new message answered after it.
         */
        private String supersedeReplies = "main";

        /**
         * How many replies in a row the user's new messages end in one conversation. The reply after
         * them is finished whatever comes, so that the user gets an answer in the end.
         */
        private int maxSuperseded = 5;

        /** How many messages of the user one turn answers at most. What waits beyond that is answered by the next turn. */
        private int maxMessages = 50;

        public String getSupersedeReplies() {
            return supersedeReplies;
        }

        public void setSupersedeReplies(String supersedeReplies) {
            this.supersedeReplies = supersedeReplies;
        }

        public int getMaxSuperseded() {
            return maxSuperseded;
        }

        public void setMaxSuperseded(int maxSuperseded) {
            this.maxSuperseded = maxSuperseded;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public Duration getLineBusyDelay() {
            return lineBusyDelay;
        }

        public int getMaxYields() {
            return maxYields;
        }

        public void setMaxYields(int maxYields) {
            this.maxYields = maxYields;
        }

        public Duration getYieldDelay() {
            return yieldDelay;
        }

        public void setYieldDelay(Duration yieldDelay) {
            this.yieldDelay = yieldDelay;
        }

        public Duration getYieldWindow() {
            return yieldWindow;
        }

        public void setYieldWindow(Duration yieldWindow) {
            this.yieldWindow = yieldWindow;
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

    /** Deliveries to the applications' webhooks. */
    public static class Webhooks {

        /** Lets webhooks reach private, loopback and link-local addresses. Never in production: it opens the service's own network. */
        private boolean allowPrivateNetworks = false;

        /** Lets webhooks use plain http. Never in production. */
        private boolean allowPlainHttp = false;

        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(10);

        /** The waits between the attempts of a delivery. After the last one fails, the webhook is turned off. */
        private java.util.List<Duration> retryDelays = java.util.List.of(Duration.ofSeconds(10), Duration.ofMinutes(1),
                Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(6), Duration.ofHours(12));

        private Duration interval = Duration.ofSeconds(1);
        private int batchSize = 50;

        public boolean isAllowPrivateNetworks() {
            return allowPrivateNetworks;
        }

        public void setAllowPrivateNetworks(boolean allowPrivateNetworks) {
            this.allowPrivateNetworks = allowPrivateNetworks;
        }

        public boolean isAllowPlainHttp() {
            return allowPlainHttp;
        }

        public void setAllowPlainHttp(boolean allowPlainHttp) {
            this.allowPlainHttp = allowPlainHttp;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public java.util.List<Duration> getRetryDelays() {
            return retryDelays;
        }

        public void setRetryDelays(java.util.List<Duration> retryDelays) {
            this.retryDelays = retryDelays;
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
    }

    /** The live stream of a user's updates. */
    public static class Stream {

        /** How long one connection stays open. The client then connects again, with Last-Event-ID. */
        private Duration maxDuration = Duration.ofMinutes(15);

        /** How often a quiet stream sends a comment, so that proxies keep it open. */
        private Duration heartbeat = Duration.ofSeconds(15);

        public Duration getMaxDuration() {
            return maxDuration;
        }

        public void setMaxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
        }

        public Duration getHeartbeat() {
            return heartbeat;
        }

        public void setHeartbeat(Duration heartbeat) {
            this.heartbeat = heartbeat;
        }
    }

    /** How many requests a caller may make: a bucket of this size, refilled at this rate per second. Per instance of the service. */
    public static class Limits {

        private double appPerSecond = 50;
        private double appBurst = 100;
        private double userPerSecond = 5;
        private double userBurst = 20;

        public double getAppPerSecond() {
            return appPerSecond;
        }

        public void setAppPerSecond(double appPerSecond) {
            this.appPerSecond = appPerSecond;
        }

        public double getAppBurst() {
            return appBurst;
        }

        public void setAppBurst(double appBurst) {
            this.appBurst = appBurst;
        }

        public double getUserPerSecond() {
            return userPerSecond;
        }

        public void setUserPerSecond(double userPerSecond) {
            this.userPerSecond = userPerSecond;
        }

        public double getUserBurst() {
            return userBurst;
        }

        public void setUserBurst(double userBurst) {
            this.userBurst = userBurst;
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

    /** The tasks a main agent hands to workers. */
    public static class Tasks {

        /**
         * The lane of the Event Inbox on which workers run: {@code batch} or {@code background}. On
         * {@code batch} a worker never holds up what wakes the main agent, and the Inbox's cap of that
         * lane ({@code inbox.lanes.batch.cap}) is how many workers of one user run at a time. On
         * {@code background} workers share the lane, and its cap, with everything else that happens
         * in the background.
         */
        private String lane = "batch";

        /** How many tasks of one main agent's conversation may be open, working or not, at a time. */
        private int maxOpen = 20;

        /** A task that nobody has touched for this long is closed when room is needed for a new one. */
        private Duration idleTtl = Duration.ofDays(7);

        /** The longest instructions of a task, and the longest later message to its worker, in characters. */
        private int maxInstructions = 8000;

        /** How much of a worker's report is kept and passed on, in characters. */
        private int maxReport = 16000;

        /** How many tasks of one main agent's conversation may wait for the user's confirmation at a time. */
        private int maxProposed = 5;

        /** How long a task waits for the user's confirmation. After that it can no longer be started. */
        private Duration proposalTtl = Duration.ofDays(3);

        /**
         * How many tasks a user's main agents may start in 24 hours without the user being asked: by
         * the user's standing authorisation, or because the application lets the role start unasked.
         * Beyond that a task waits for the user's confirmation like any other that the main agent
         * begins by itself. What the user asks for, and what the user confirms, does not count.
         */
        private int maxUnaskedPerDay = 10;

        /**
         * How forward a main agent is with a user who has not said how they want it, when the agent's
         * own configuration does not say either: {@code proactive}, {@code balanced} or {@code quiet}.
         */
        private String proactivity = "proactive";

        /**
         * The longest turn of a worker on a task. Longer than a conversation's turn, because nobody
         * waits for it; keep it below the Inbox's {@code inbox.lease.max-duration}. Work that takes
         * longer goes on in a further turn, which the worker sets itself a follow-up for.
         */
        private Duration turnDeadline = Duration.ofMinutes(20);

        public String getLane() {
            return lane;
        }

        public int getMaxUnaskedPerDay() {
            return maxUnaskedPerDay;
        }

        public void setMaxUnaskedPerDay(int maxUnaskedPerDay) {
            this.maxUnaskedPerDay = maxUnaskedPerDay;
        }

        public String getProactivity() {
            return proactivity;
        }

        public void setProactivity(String proactivity) {
            this.proactivity = proactivity;
        }

        public Duration getTurnDeadline() {
            return turnDeadline;
        }

        public void setTurnDeadline(Duration turnDeadline) {
            this.turnDeadline = turnDeadline;
        }

        public void setLane(String lane) {
            this.lane = lane;
        }

        public int getMaxOpen() {
            return maxOpen;
        }

        public void setMaxOpen(int maxOpen) {
            this.maxOpen = maxOpen;
        }

        public Duration getIdleTtl() {
            return idleTtl;
        }

        public void setIdleTtl(Duration idleTtl) {
            this.idleTtl = idleTtl;
        }

        public int getMaxInstructions() {
            return maxInstructions;
        }

        public void setMaxInstructions(int maxInstructions) {
            this.maxInstructions = maxInstructions;
        }

        public int getMaxReport() {
            return maxReport;
        }

        public void setMaxReport(int maxReport) {
            this.maxReport = maxReport;
        }

        public int getMaxProposed() {
            return maxProposed;
        }

        public void setMaxProposed(int maxProposed) {
            this.maxProposed = maxProposed;
        }

        public Duration getProposalTtl() {
            return proposalTtl;
        }

        public void setProposalTtl(Duration proposalTtl) {
            this.proposalTtl = proposalTtl;
        }
    }

    /** What an agent may send a user of its own accord. */
    public static class Notify {

        /** How many notifications one run may send. */
        private int perRun = 3;

        /** How many notifications a user gets within 24 hours, from all agents of the application together. */
        private int perUserPerDay = 20;

        /** The same, for a user who has asked their main agent to be quiet. Never more than {@code perUserPerDay}. */
        private int quietPerUserPerDay = 5;

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

        public int getQuietPerUserPerDay() {
            return quietPerUserPerDay;
        }

        public void setQuietPerUserPerDay(int quietPerUserPerDay) {
            this.quietPerUserPerDay = quietPerUserPerDay;
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
