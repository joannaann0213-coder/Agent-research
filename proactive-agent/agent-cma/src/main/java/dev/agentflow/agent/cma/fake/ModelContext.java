package dev.agentflow.agent.cma.fake;

import dev.agentflow.agent.cma.AgentSpec;
import dev.agentflow.agent.cma.CmaEvent;
import java.util.List;
import java.util.Map;

/**
 * What the scripted model can see when it is asked for its next step.
 *
 * @param userMessage the message that started the turn
 * @param step 0 when the turn starts, then one more for every time the model is asked again
 * @param toolResults the results that came in since the previous step; empty at step 0
 * @param history every stored event of the session so far, oldest first
 */
public record ModelContext(
        String sessionId,
        AgentSpec agent,
        Map<String, String> sessionMetadata,
        String userMessage,
        int step,
        List<ToolOutcome> toolResults,
        List<CmaEvent> history) {

    /** The result of the call of {@code tool} since the previous step, or {@code null}. */
    public ToolOutcome resultOf(String tool) {
        return toolResults.stream().filter(result -> tool.equals(result.tool())).findFirst().orElse(null);
    }

    /** Every message the session has received, oldest first. */
    public List<String> userMessages() {
        return history.stream()
                .filter(event -> event instanceof CmaEvent.UserMessage)
                .map(event -> ((CmaEvent.UserMessage) event).text())
                .toList();
    }
}
