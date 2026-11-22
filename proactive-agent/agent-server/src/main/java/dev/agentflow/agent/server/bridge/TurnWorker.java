package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Claims runs from the Event Inbox and hands each to the {@link TurnRunner}. It claims without
 * steering: messages that arrive while a turn runs are not pushed into it; the Inbox gathers them
 * into the line's next run.
 */
@Component
public class TurnWorker {

    private static final Logger log = LoggerFactory.getLogger(TurnWorker.class);

    private final InboxClient inbox;
    private final TurnRunner runner;
    private final AgentProperties properties;
    private final String workerId;
    private final List<Thread> loops = new ArrayList<>();
    private volatile boolean running;

    public TurnWorker(InboxClient inbox, TurnRunner runner, AgentProperties properties) {
        this.inbox = inbox;
        this.runner = runner;
        this.properties = properties;
        String configured = properties.getWorker().getId();
        this.workerId = configured != null ? configured : "agent-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Claims one run, if one can be claimed right now, and works on it to its end.
     *
     * @return whether there was a run
     */
    public boolean runOnce() {
        return runOnce(0);
    }

    private boolean runOnce(long waitMillis) {
        ApiResult<ClaimedRun> claimed = inbox.claim(new ClaimRequest(workerId, null, null, null, false,
                waitMillis > 0 ? waitMillis : null));
        if (!claimed.ok()) {
            throw new IllegalStateException("the Inbox refused the claim: " + claimed.status() + " " + claimed.code());
        }
        if (claimed.body() == null) {
            return false;
        }
        runner.run(claimed.body());
        return true;
    }

    @EventListener(ApplicationReadyEvent.class)
    void start() {
        if (!properties.getScheduling().isEnabled()) {
            return;
        }
        running = true;
        for (int i = 0; i < properties.getWorker().getConcurrency(); i++) {
            loops.add(Thread.ofVirtual().name("turn-worker-" + i).start(this::loop));
        }
        log.info("{} turn workers claim from the Inbox as {}", loops.size(), workerId);
    }

    private void loop() {
        while (running) {
            try {
                runOnce(properties.getWorker().getClaimWait().toMillis());
            } catch (RuntimeException e) {
                log.warn("claiming from the Inbox failed, trying again shortly: {}", e.toString());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException stop) {
                    return;
                }
            }
        }
    }

    @PreDestroy
    void stop() {
        running = false;
        loops.forEach(Thread::interrupt);
    }
}
