package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.server.events.AppEvents.AppEvent;
import java.util.List;

/** What a run asks of the agent. A line of work carries one kind or the other, never both. */
sealed interface Work {

    /** The user said something and waits for an answer. What the agent writes last is that answer. */
    record Chat(List<MessageView> messages) implements Work {
    }

    /**
     * Something happened in the application's systems. The user waits for nothing and sees nothing
     * of the turn; they hear of it only if the agent decides to notify them.
     */
    record Task(List<AppEvent> events) implements Work {

        /** Where the user is told, if they are: the conversation that the latest event names. */
        String conversationId() {
            return events.getLast().conversationId();
        }
    }
}
