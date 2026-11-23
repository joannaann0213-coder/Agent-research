package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.server.actions.ActionDesk;
import dev.agentflow.agent.server.tasks.Tasks;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the sweeps: for turns whose run ended without this service, for tasks that waited too long
 * for the user's confirmation, and for questions asked in passing that nobody answered in time.
 */
@Component
public class SweepLoop {

    private static final int BATCH = 100;

    private final TurnSweeper sweeper;
    private final Tasks tasks;
    private final ActionDesk actions;

    public SweepLoop(TurnSweeper sweeper, Tasks tasks, ActionDesk actions) {
        this.sweeper = sweeper;
        this.tasks = tasks;
        this.actions = actions;
    }

    @Scheduled(fixedDelayString = "${agent.sweep.interval:30s}")
    void tick() {
        sweeper.runOnce();
    }

    @Scheduled(fixedDelayString = "${agent.sweep.interval:30s}")
    void unconfirmedTasks() {
        tasks.closeUnconfirmed(BATCH);
    }

    @Scheduled(fixedDelayString = "${agent.sweep.interval:30s}")
    void unansweredQuestions() {
        actions.expireUnanswered(BATCH);
    }
}
