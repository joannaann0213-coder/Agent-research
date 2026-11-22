package dev.agentflow.agent.cma.fake;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Ready-made scripts. */
public final class FakeBrains {

    private FakeBrains() {
    }

    /** Answers every message with {@code You said: <the message>}. */
    public static FakeBrain echo() {
        return context -> ModelStep.say("You said: " + context.userMessage());
    }

    /**
     * Plays the given steps in order, one per time the model is asked, whatever it is asked. When
     * they run out it says so, which ends the turn and shows in the test.
     */
    public static FakeBrain sequence(ModelStep... steps) {
        List<ModelStep> script = List.of(steps);
        AtomicInteger next = new AtomicInteger();
        return context -> {
            int index = next.getAndIncrement();
            return index < script.size() ? script.get(index) : ModelStep.say("(the script has no step " + (index + 1) + ")");
        };
    }
}
