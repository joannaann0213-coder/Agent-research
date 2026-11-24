package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.server.events.AppEvents.AppEvent;
import dev.agentflow.agent.server.schedules.Schedules.Schedule;
import dev.agentflow.agent.server.tasks.Tasks;
import java.time.Instant;
import java.util.List;

/** What a run asks of the agent. A run carries one kind, never two. */
sealed interface Work {

    /**
     * The user said something and waits for an answer. What the agent writes last is that answer.
     * The list is empty when the user stopped the reply before the turn began.
     *
     * @param news for a main agent: what its tasks have for it that no turn of it has been given yet.
     *     It answers the user knowing how everything stands; empty on any other line
     */
    record Chat(List<MessageView> messages, List<Tasks.Report> news) implements Work {

        Chat(List<MessageView> messages) {
            this(messages, List.of());
        }
    }

    /**
     * Work in the background: something happened in the application's systems, a schedule or a
     * follow-up is due, or, for a main agent, a worker has reported on a task. The user waits for
     * nothing and sees nothing of the turn; they hear of it only if the agent notifies them.
     *
     * @param reports what the tasks of a main agent have for it: what workers report, and what tells
     *     it how its tasks stand. Everything that no turn of it has been given yet, not only what woke
     *     this one; empty on any other line
     */
    record Task(List<AppEvent> events, List<Due> due, List<Tasks.Report> reports) implements Work {

        /** Where the user is told, if they are: the conversation of the latest event, of what is due, or of the task reported on. */
        String conversationId() {
            return !events.isEmpty() ? events.getLast().conversationId()
                    : !due.isEmpty() ? due.getLast().schedule().conversationId()
                    : reports.getLast().task().conversationId();
        }

        /** How many follow-ups in a row led to this turn; 0 when it is not a follow-up's. */
        int chain() {
            return Work.chain(due);
        }

        List<Due> followups() {
            return due.stream().filter(item -> item.schedule().followup()).toList();
        }

        List<Due> scheduled() {
            return due.stream().filter(item -> !item.schedule().followup()).toList();
        }
    }

    /**
     * A worker's turn on a task that a main agent delegated: the main agent sent something, or a
     * follow-up that the worker set for itself on the task is due. Nobody but the main agent hears
     * of the turn: what the worker writes last is its report to it.
     *
     * @param notes what the main agent sent and this turn is given; empty when only a follow-up is due
     * @param due the worker's own follow-ups that are due
     */
    record Job(Tasks.Task task, List<Tasks.Note> notes, List<Due> due) implements Work {

        /** How many follow-ups in a row led to this turn; 0 when the main agent started it. */
        int chain() {
            return Work.chain(due);
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

    private static int chain(List<Due> due) {
        return due.stream().filter(item -> item.schedule().followup()).mapToInt(item -> item.schedule().chain()).max().orElse(0);
    }
}
