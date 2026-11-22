package dev.agentflow.agent.server.bridge;

import dev.agentflow.inbox.api.ClaimedRun;
import org.springframework.stereotype.Component;

/**
 * Named points in a turn where a test can make the worker die, to check that a run delivered again
 * picks the turn up correctly. In production nothing happens at them.
 */
public interface TurnHooks {

    enum Point {
        /** A session exists on the platform; the line does not know it yet. */
        SESSION_CREATED,
        /** The turn knows where the session stood before its input. */
        BOUNDARY_FIXED,
        /** The input is on the platform; the turn has not recorded its id. */
        INPUT_SENT,
        /** The turn has recorded the input's id and starts reading. */
        INPUT_RECORDED,
        /** The calls the session waits for have been run; the session has not been given their answers. */
        TOOLS_RUN,
        /** The session has the answers to the calls it waited for. */
        ANSWERS_SENT,
        /** The reply is stored and the turn is done; the Inbox has not been told. */
        RESULT_STORED
    }

    void at(Point point, ClaimedRun run);

    @Component
    class None implements TurnHooks {

        @Override
        public void at(Point point, ClaimedRun run) {
        }
    }
}
