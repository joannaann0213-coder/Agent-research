package dev.agentflow.inbox.server.wake;

import dev.agentflow.inbox.server.config.InboxProperties;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * Wakes claimers that long-poll on this instance. Claimers on other instances find the work on their
 * next poll; a cross-instance channel is a later addition.
 */
@Component
public class InProcessWakeBus implements WakeBus {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();
    private final boolean enabled;
    private long generation;

    public InProcessWakeBus(InboxProperties properties) {
        this.enabled = properties.getWake().isEnabled();
    }

    @Override
    public long generation() {
        lock.lock();
        try {
            return generation;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void publish() {
        if (!enabled) {
            return;
        }
        lock.lock();
        try {
            generation++;
            changed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean await(long seenGeneration, Duration timeout) throws InterruptedException {
        long remaining = timeout.toNanos();
        lock.lock();
        try {
            while (generation == seenGeneration) {
                if (remaining <= 0) {
                    return false;
                }
                remaining = changed.awaitNanos(remaining);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    static long toNanos(Duration duration) {
        return TimeUnit.NANOSECONDS.convert(duration);
    }
}
