package dev.agentflow.agent.server.support;

import dev.agentflow.agent.server.bridge.TurnHooks;

/**
 * Thrown by a test where a worker is to die. It is an {@link Error} so that nothing on its way up
 * handles it: like a killed process, the worker settles nothing and tells nobody.
 */
public final class SimulatedCrash extends Error {

    public SimulatedCrash(TurnHooks.Point point) {
        super("the worker dies at " + point, null, false, false);
    }
}
