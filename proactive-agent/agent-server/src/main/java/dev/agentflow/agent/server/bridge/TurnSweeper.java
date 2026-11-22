package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.server.bridge.Turns.Turn;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.RunStatus;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Closes the turns of runs that ended in the Event Inbox without this service ending them: runs
 * that used up their attempts, or that an operator took out.
 *
 * <p>A turn normally ends together with its run, because the worker that ends the one tells the
 * Inbox to end the other. When every worker of a run died, or kept failing, the Inbox ends the run
 * on its own, and its turn would stay open for ever. The session that such a turn leaves behind
 * needs no cleaning up here: the line's next turn brings it to rest before it uses it.
 */
@Component
public class TurnSweeper {

    private static final Logger log = LoggerFactory.getLogger(TurnSweeper.class);

    private static final int BATCH = 100;

    private final Turns turns;
    private final InboxClient inbox;
    private final AgentClock clock;
    private final AgentProperties.Sweep config;

    public TurnSweeper(Turns turns, InboxClient inbox, AgentClock clock, AgentProperties properties) {
        this.turns = turns;
        this.inbox = inbox;
        this.clock = clock;
        this.config = properties.getSweep();
    }

    /**
     * One pass over the open turns that nobody has touched for a while.
     *
     * @return how many turns were closed
     */
    public int runOnce() {
        int closed = 0;
        for (Turn turn : turns.openAndUntouchedSince(clock.now().minus(config.getGrace()), BATCH)) {
            ApiResult<RunView> run = inbox.getRun(turn.runId());
            String ended = run.ok() ? endOf(run.body())
                    : run.status() == 404 ? "the Inbox does not know the run" : null;
            if (ended == null) {
                // The run is still open, or the Inbox could not say. Look again after another while.
                turns.touch(turn.runId());
            } else if (turns.abandon(turn.runId(), ended)) {
                log.warn("turn of run {} closed: {}", turn.runId(), ended);
                closed++;
            }
        }
        return closed;
    }

    /** Why the run is over, or {@code null} when it is not. */
    private static String endOf(RunView run) {
        RunStatus status = RunStatus.fromWire(run.status());
        if (status.isOpen()) {
            return null;
        }
        String reason = "the run ended in the Inbox as " + run.status();
        if (run.endReason() != null) {
            reason += " (" + run.endReason() + ")";
        }
        return run.lastError() == null ? reason : reason + ": " + run.lastError();
    }
}
