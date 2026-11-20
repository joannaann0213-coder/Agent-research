package dev.agentflow.inbox.server.claim;

/** What a running run should know about events that arrived after it started. */
public final class RunSignals {

    public record Signals(int steerPending, boolean interruptRequested, String interruptEventId) {
    }

    private static final Signals NONE = new Signals(0, false, null);

    private RunSignals() {
    }

    public static Signals none() {
        return NONE;
    }
}
