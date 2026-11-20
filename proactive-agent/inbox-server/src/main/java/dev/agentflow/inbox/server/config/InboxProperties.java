package dev.agentflow.inbox.server.config;

import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.server.auth.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Every tunable of the service, under the {@code inbox.*} prefix. The defaults are the numbers of the
 * design document; per-inbox overrides live in the {@code inbox} table.
 */
@ConfigurationProperties(prefix = "inbox")
public class InboxProperties {

    private final Auth auth = new Auth();
    private final Payload payload = new Payload();
    private final Fingerprint fingerprint = new Fingerprint();
    private final Interrupt interrupt = new Interrupt();
    private final Lease lease = new Lease();
    private final Retry retry = new Retry();
    private final Batch batch = new Batch();
    private final Lanes lanes = new Lanes();
    private final Claim claim = new Claim();
    private final Suspend suspend = new Suspend();
    private final Timers timers = new Timers();
    private final Scheduling scheduling = new Scheduling();
    private final Wake wake = new Wake();

    public Auth getAuth() {
        return auth;
    }

    public Payload getPayload() {
        return payload;
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public Interrupt getInterrupt() {
        return interrupt;
    }

    public Lease getLease() {
        return lease;
    }

    public Retry getRetry() {
        return retry;
    }

    public Batch getBatch() {
        return batch;
    }

    public Lanes getLanes() {
        return lanes;
    }

    public Claim getClaim() {
        return claim;
    }

    public Suspend getSuspend() {
        return suspend;
    }

    public Timers getTimers() {
        return timers;
    }

    public Scheduling getScheduling() {
        return scheduling;
    }

    public Wake getWake() {
        return wake;
    }

    public static class Auth {
        /** When false every request is treated as {@link #devClient}. For local development only. */
        private boolean enabled = true;
        /** Largest accepted difference between X-Timestamp and the server clock. */
        private Duration clockSkew = Duration.ofSeconds(300);
        /** Request bodies larger than this are refused before parsing. */
        private int maxBodyBytes = 128 * 1024;
        private List<Client> clients = new ArrayList<>();
        private final Client devClient = Client.dev();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getClockSkew() {
            return clockSkew;
        }

        public void setClockSkew(Duration clockSkew) {
            this.clockSkew = clockSkew;
        }

        public int getMaxBodyBytes() {
            return maxBodyBytes;
        }

        public void setMaxBodyBytes(int maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
        }

        public List<Client> getClients() {
            return clients;
        }

        public void setClients(List<Client> clients) {
            this.clients = clients;
        }

        public Client getDevClient() {
            return devClient;
        }
    }

    /** A caller: its HMAC secret, the tenant it belongs to (empty = platform scope) and what it may do. */
    public static class Client {
        private String id;
        private String secret;
        private String tenant;
        private Set<Role> roles = EnumSet.noneOf(Role.class);

        static Client dev() {
            Client client = new Client();
            client.id = "dev";
            client.tenant = "dev";
            client.roles = EnumSet.allOf(Role.class);
            return client;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public Set<Role> getRoles() {
            return roles;
        }

        public void setRoles(Set<Role> roles) {
            this.roles = roles;
        }
    }

    public static class Payload {
        /** Largest canonical payload, in bytes. */
        private int maxBytes = 64 * 1024;

        public int getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(int maxBytes) {
            this.maxBytes = maxBytes;
        }
    }

    public static class Fingerprint {
        /** Width of the time bucket used when an event has neither an id nor an occurred_at. */
        private Duration bucket = Duration.ofSeconds(60);

        public Duration getBucket() {
            return bucket;
        }

        public void setBucket(Duration bucket) {
            this.bucket = bucket;
        }
    }

    public static class Interrupt {
        /** Exact (trimmed, case-insensitive) texts of a conversation message that ask a run to stop. */
        private List<String> keywords = new ArrayList<>(List.of("stop", "cancel", "停止", "取消"));
        /** JSON pointer to the message text inside the payload. */
        private String textPointer = "/text";

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public String getTextPointer() {
            return textPointer;
        }

        public void setTextPointer(String textPointer) {
            this.textPointer = textPointer;
        }
    }

    public static class Lease {
        /** How long one heartbeat keeps a run alive. */
        private Duration duration = Duration.ofSeconds(60);
        /** Longest a single lease grant can be renewed to. */
        private Duration maxDuration = Duration.ofMinutes(30);

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

        public Duration getMaxDuration() {
            return maxDuration;
        }

        public void setMaxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
        }
    }

    public static class Retry {
        private int maxAttempts = 5;
        /** Backoff after attempt n is base^n, capped. */
        private Duration backoffBase = Duration.ofSeconds(2);
        private Duration backoffCap = Duration.ofMinutes(5);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBackoffBase() {
            return backoffBase;
        }

        public void setBackoffBase(Duration backoffBase) {
            this.backoffBase = backoffBase;
        }

        public Duration getBackoffCap() {
            return backoffCap;
        }

        public void setBackoffCap(Duration backoffCap) {
            this.backoffCap = backoffCap;
        }
    }

    public static class Batch {
        private int maxEvents = 20;
        private int maxBytes = 256 * 1024;

        public int getMaxEvents() {
            return maxEvents;
        }

        public void setMaxEvents(int maxEvents) {
            this.maxEvents = maxEvents;
        }

        public int getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(int maxBytes) {
            this.maxBytes = maxBytes;
        }
    }

    public static class Lanes {
        private final LaneSettings interactive = new LaneSettings(1);
        private final LaneSettings background = new LaneSettings(2);
        private final LaneSettings batch = new LaneSettings(1);

        public LaneSettings getInteractive() {
            return interactive;
        }

        public LaneSettings getBackground() {
            return background;
        }

        public LaneSettings getBatch() {
            return batch;
        }

        public LaneSettings of(Lane lane) {
            return switch (lane) {
                case INTERACTIVE -> interactive;
                case BACKGROUND -> background;
                case BATCH -> batch;
            };
        }
    }

    public static class LaneSettings {
        /** Concurrent runs of this lane inside one inbox. */
        private int cap;
        /** Batching waits until no same-key event has arrived for this long. */
        private Duration quietWindow = Duration.ofMillis(1500);
        /** Upper bound on that wait, from the first event of the batch. Empty means four quiet windows. */
        private Duration maxWait;

        LaneSettings(int cap) {
            this.cap = cap;
        }

        public int getCap() {
            return cap;
        }

        public void setCap(int cap) {
            this.cap = cap;
        }

        public Duration getQuietWindow() {
            return quietWindow;
        }

        public void setQuietWindow(Duration quietWindow) {
            this.quietWindow = quietWindow;
        }

        public Duration getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(Duration maxWait) {
            this.maxWait = maxWait;
        }

        public Duration effectiveMaxWait() {
            return maxWait != null ? maxWait : quietWindow.multipliedBy(4);
        }
    }

    public static class Claim {
        /** How often a long-polling claim re-checks when no wake hint arrives. */
        private Duration pollInterval = Duration.ofSeconds(2);
        /** Longest a claim request may wait. */
        private Duration maxWait = Duration.ofSeconds(30);
        /** Inboxes tried per discovery round. */
        private int discoveryCandidates = 32;
        /** Unclaimed events loaded per inbox when planning a claim. */
        private int planningWindow = 1000;
        /** How long a claim waits for a busy inbox row before answering "busy". */
        private Duration lockTimeout = Duration.ofMillis(500);

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public Duration getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(Duration maxWait) {
            this.maxWait = maxWait;
        }

        public int getDiscoveryCandidates() {
            return discoveryCandidates;
        }

        public void setDiscoveryCandidates(int discoveryCandidates) {
            this.discoveryCandidates = discoveryCandidates;
        }

        public int getPlanningWindow() {
            return planningWindow;
        }

        public void setPlanningWindow(int planningWindow) {
            this.planningWindow = planningWindow;
        }

        public Duration getLockTimeout() {
            return lockTimeout;
        }

        public void setLockTimeout(Duration lockTimeout) {
            this.lockTimeout = lockTimeout;
        }
    }

    public static class Suspend {
        /** Longest timeout a run may ask for when it suspends. */
        private Duration maxTimeout = Duration.ofDays(30);

        public Duration getMaxTimeout() {
            return maxTimeout;
        }

        public void setMaxTimeout(Duration maxTimeout) {
            this.maxTimeout = maxTimeout;
        }
    }

    public static class Timers {
        /** Cron timers may not fire more often than this. */
        private Duration minCronPeriod = Duration.ofSeconds(60);

        public Duration getMinCronPeriod() {
            return minCronPeriod;
        }

        public void setMinCronPeriod(Duration minCronPeriod) {
            this.minCronPeriod = minCronPeriod;
        }
    }

    public static class Scheduling {
        /** Runs the background loops (lease sweeper, suspend sweeper, timer scheduler, gauges). */
        private boolean enabled = true;
        private Duration leaseSweepInterval = Duration.ofSeconds(1);
        private Duration suspendSweepInterval = Duration.ofSeconds(1);
        private Duration timerInterval = Duration.ofSeconds(1);
        private Duration gaugeInterval = Duration.ofSeconds(15);
        /** Rows handled per loop tick. */
        private int batchSize = 200;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getLeaseSweepInterval() {
            return leaseSweepInterval;
        }

        public void setLeaseSweepInterval(Duration leaseSweepInterval) {
            this.leaseSweepInterval = leaseSweepInterval;
        }

        public Duration getSuspendSweepInterval() {
            return suspendSweepInterval;
        }

        public void setSuspendSweepInterval(Duration suspendSweepInterval) {
            this.suspendSweepInterval = suspendSweepInterval;
        }

        public Duration getTimerInterval() {
            return timerInterval;
        }

        public void setTimerInterval(Duration timerInterval) {
            this.timerInterval = timerInterval;
        }

        public Duration getGaugeInterval() {
            return gaugeInterval;
        }

        public void setGaugeInterval(Duration gaugeInterval) {
            this.gaugeInterval = gaugeInterval;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class Wake {
        /** In-process wake hints. Turning them off only costs latency: claimers fall back to polling. */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
