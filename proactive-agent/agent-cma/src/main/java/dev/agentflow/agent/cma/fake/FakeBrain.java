package dev.agentflow.agent.cma.fake;

/**
 * The scripted stand-in for the model. It is asked when a turn starts and again whenever the
 * results of its tool calls are in.
 */
@FunctionalInterface
public interface FakeBrain {

    ModelStep next(ModelContext context);
}
