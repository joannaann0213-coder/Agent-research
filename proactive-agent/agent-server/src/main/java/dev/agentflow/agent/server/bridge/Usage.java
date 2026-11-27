package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.cma.CmaEvent;

/**
 * What the requests to the model used in a turn, or in a part of one.
 *
 * @param requests how many requests there were: one for every step of the agent
 * @param inputTokens input that was neither read from the cache nor written to it
 * @param cacheReadTokens input that was read from the cache
 * @param cacheCreationTokens input that was written to the cache
 */
public record Usage(int requests, long inputTokens, long outputTokens, long cacheReadTokens, long cacheCreationTokens) {

    public static final Usage NONE = new Usage(0, 0, 0, 0, 0);

    Usage plus(CmaEvent.ModelUsage one) {
        return new Usage(requests + 1, inputTokens + one.inputTokens(), outputTokens + one.outputTokens(),
                cacheReadTokens + one.cacheReadTokens(), cacheCreationTokens + one.cacheCreationTokens());
    }
}
