package dev.agentflow.agent.server.support;

import dev.agentflow.agent.server.bridge.TurnHooks;
import dev.agentflow.inbox.api.ClaimedRun;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Lets a test step into a turn at one of its named points: to make the worker die there, or to do something meanwhile. */
public final class TestHooks implements TurnHooks {

    private final Map<Point, Deque<Consumer<ClaimedRun>>> planned = new EnumMap<>(Point.class);
    private final Set<Point> fatal = EnumSet.noneOf(Point.class);

    @Override
    public void at(Point point, ClaimedRun run) {
        Consumer<ClaimedRun> action;
        synchronized (this) {
            if (fatal.contains(point)) {
                throw new SimulatedCrash(point);
            }
            Deque<Consumer<ClaimedRun>> actions = planned.get(point);
            action = actions == null ? null : actions.poll();
        }
        if (action != null) {
            action.accept(run);
        }
    }

    /** The next turn that reaches {@code point} runs {@code action} there, and then goes on. */
    public synchronized void once(Point point, Consumer<ClaimedRun> action) {
        planned.computeIfAbsent(point, p -> new ArrayDeque<>()).add(action);
    }

    /** The next worker that reaches {@code point} dies there. The one after it gets through. */
    public void crashAt(Point point) {
        once(point, run -> {
            throw new SimulatedCrash(point);
        });
    }

    /** Every worker that reaches {@code point} dies there, until {@link #reset}. */
    public synchronized void crashAlwaysAt(Point point) {
        fatal.add(point);
    }

    public synchronized void reset() {
        planned.clear();
        fatal.clear();
    }
}
