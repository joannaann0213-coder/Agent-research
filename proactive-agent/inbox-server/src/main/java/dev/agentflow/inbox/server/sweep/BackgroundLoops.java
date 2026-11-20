package dev.agentflow.inbox.server.sweep;

import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.time.InboxClock;
import dev.agentflow.inbox.server.timer.TimerScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the periodic jobs. Each job is a thin wrapper around a {@code runOnce()} method that tests call
 * directly, and each job is safe to run on every instance at the same time.
 */
@Component
@ConditionalOnProperty(prefix = "inbox.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackgroundLoops {

    private static final Logger log = LoggerFactory.getLogger(BackgroundLoops.class);

    private final InboxProperties.Scheduling config;
    private final InboxClock clock;
    private final LeaseSweeper leaseSweeper;
    private final SuspendSweeper suspendSweeper;
    private final TimerScheduler timerScheduler;
    private final ReclaimGate reclaimGate;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "inbox-background");
        thread.setDaemon(true);
        return thread;
    });

    public BackgroundLoops(InboxProperties properties, InboxClock clock, LeaseSweeper leaseSweeper,
            SuspendSweeper suspendSweeper, TimerScheduler timerScheduler) {
        this.config = properties.getScheduling();
        this.clock = clock;
        this.leaseSweeper = leaseSweeper;
        this.suspendSweeper = suspendSweeper;
        this.timerScheduler = timerScheduler;
        this.reclaimGate = new ReclaimGate(properties.getLease().getDuration(), clock.now());
    }

    @PostConstruct
    void start() {
        schedule("lease sweep", config.getLeaseSweepInterval(), this::leaseSweep);
        schedule("suspend sweep", config.getSuspendSweepInterval(), suspendSweeper::runOnce);
        schedule("timer scheduler", config.getTimerInterval(), timerScheduler::runOnce);
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }

    private void leaseSweep() {
        if (!reclaimGate.mayReclaim(clock.now())) {
            return;
        }
        try {
            leaseSweeper.runOnce();
        } catch (RuntimeException e) {
            reclaimGate.passFailed(clock.now());
            throw e;
        }
    }

    void schedule(String name, Duration interval, Runnable job) {
        executor.scheduleWithFixedDelay(() -> {
            try {
                job.run();
            } catch (RuntimeException e) {
                log.error("{} failed; it will run again on its next tick", name, e);
            }
        }, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }
}
