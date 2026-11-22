package dev.agentflow.agent.server.bridge;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the sweep for turns whose run ended without this service. */
@Component
public class SweepLoop {

    private final TurnSweeper sweeper;

    public SweepLoop(TurnSweeper sweeper) {
        this.sweeper = sweeper;
    }

    @Scheduled(fixedDelayString = "${agent.sweep.interval:30s}")
    void tick() {
        sweeper.runOnce();
    }
}
