package dev.agentflow.agent.server.delivery;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * Wakes the streams of a user in this process as soon as something was recorded for them. Streams
 * also look on their own every second, which is what serves them when the update was recorded by
 * another instance of the service.
 */
@Component
public class StreamHub {

    private static final class Signal {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition changed = lock.newCondition();
        private long version;
    }

    private final Map<String, Signal> signals = new ConcurrentHashMap<>();

    /** The current version of the user's signal, to wait from. */
    public long version(String appId, String userId) {
        Signal signal = signal(appId, userId);
        signal.lock.lock();
        try {
            return signal.version;
        } finally {
            signal.lock.unlock();
        }
    }

    /** Waits until the user's signal moves past {@code version}, or until {@code time} has passed. */
    public void await(String appId, String userId, long version, Duration time) {
        Signal signal = signal(appId, userId);
        signal.lock.lock();
        try {
            long left = time.toNanos();
            while (signal.version == version && left > 0) {
                left = signal.changed.awaitNanos(left);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            signal.lock.unlock();
        }
    }

    public void wake(String appId, String userId) {
        Signal signal = signal(appId, userId);
        signal.lock.lock();
        try {
            signal.version++;
            signal.changed.signalAll();
        } finally {
            signal.lock.unlock();
        }
    }

    private Signal signal(String appId, String userId) {
        return signals.computeIfAbsent(appId + "\n" + userId, key -> new Signal());
    }

    @SuppressWarnings("unused")
    private static long millis(Duration time) {
        return TimeUnit.NANOSECONDS.toMillis(time.toNanos());
    }
}
