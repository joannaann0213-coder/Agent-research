package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.server.tasks.Tasks;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the sweeps: for turns whose run ended without this service, and for tasks that waited too
 * long for the user's confirmation.
 */
@Component
public class SweepLoop {

    private static final int BATCH = 100;

    private final TurnSweeper sweeper;
    private final Tasks tasks;

    public SweepLoop(TurnSweeper sweeper, Tasks tasks) {
        this.sweeper = sweeper;
        this.tasks = tasks;
    }

    @Scheduled(fixedDelayString = "${agent.sweep.interval:30s}")
    void tick() {
        sweeper.runOnce();
    }

    @Scheduled(fixedDelayString = "${agent.sweep.interval:30s}")
    void unconfirmedTasks() {
        tasks.closeUnconfirmed(BATCH);
    }
}
