package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.server.events.AppEvents.AppEvent;
import dev.agentflow.agent.server.schedules.Schedules.Schedule;
import java.time.Instant;
import java.util.List;

/** What a run asks of the agent. A run carries one kind or the other, never both. */
sealed interface Work {

    /**
     * The user said something and waits for an answer. What the agent writes last is that answer.
     * The list is empty when the user stopped the reply before the turn began.
     */
    record Chat(List<MessageView> messages) implements Work {
    }

    /**
     * Work in the background: something happened in the application's systems, or a schedule or a
     * follow-up is due. The user waits for nothing and sees nothing of the turn; they hear of it only
     * if the agent notifies them.
     */
    record Task(List<AppEvent> events, List<Due> due) implements Work {

        /** Where the user is told, if they are: the conversation of the latest event, or of what is due. */
        String conversationId() {
            return !events.isEmpty() ? events.getLast().conversationId() : due.getLast().schedule().conversationId();
        }

        /** How many follow-ups in a row led to this turn; 0 when it is not a follow-up's. */
        int chain() {
            return due.stream().filter(item -> item.schedule().followup()).mapToInt(item -> item.schedule().chain()).max().orElse(0);
        }

        List<Due> followups() {
            return due.stream().filter(item -> item.schedule().followup()).toList();
        }

        List<Due> scheduled() {
            return due.stream().filter(item -> !item.schedule().followup()).toList();
        }
    }

    /**
     * A schedule that is due.
     *
     * @param dueAt when it was due
     * @param late whether it comes late, because the service was not running when it was due
     */
    record Due(Schedule schedule, Instant dueAt, boolean late) {
    }
}
