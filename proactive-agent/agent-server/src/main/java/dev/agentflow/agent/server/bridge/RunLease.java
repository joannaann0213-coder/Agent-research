package dev.agentflow.agent.server.bridge;

import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the lease of a run alive while a turn works on it, from a thread of its own, and notices
 * when the lease is gone.
 */
final class RunLease implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RunLease.class);

    private final InboxClient inbox;
    private final ClaimedRun run;
    private final Thread beat;
    private volatile boolean lost;

    RunLease(InboxClient inbox, ClaimedRun run, Duration interval) {
        this.inbox = inbox;
        this.run = run;
        this.beat = Thread.ofVirtual().name("lease-" + run.runId()).start(() -> beat(interval));
    }

    /** Whether the Inbox has said that this worker no longer holds the run. */
    boolean lost() {
        return lost;
    }

    private void beat(Duration interval) {
        while (!lost && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(interval);
                ApiResult<HeartbeatResponse> result = inbox.heartbeat(run.runId(), new HeartbeatRequest(run.epoch(), null));
                if (result.status() == 409 || result.status() == 404) {
                    log.warn("run {} is no longer held under epoch {}; the turn stops", run.runId(), run.epoch());
                    lost = true;
                }
            } catch (InterruptedException closed) {
                return;
            } catch (RuntimeException e) {
                // A heartbeat that did not get through is not the end of the lease; the next one may.
                log.warn("heartbeat for run {} failed: {}", run.runId(), e.toString());
            }
        }
    }

    @Override
    public void close() {
        beat.interrupt();
    }
}
