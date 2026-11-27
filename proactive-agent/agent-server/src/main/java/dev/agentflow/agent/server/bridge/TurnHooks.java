package dev.agentflow.agent.server.bridge;

import dev.agentflow.inbox.api.ClaimedRun;
import org.springframework.stereotype.Component;

/**
 * Named points in a turn where a test can make the worker die, to check that a run delivered again
 * picks the turn up correctly. In production nothing happens at them.
 */
public interface TurnHooks {

    enum Point {
        /** What the application scheduled for a worker has been handed to it; the run has not gone on, and the Inbox knows nothing. */
        HANDED_OVER,
        /** A turn in the background has given way to a message of the user and let go of the session; the Inbox has not suspended its run yet. */
        YIELDED,
        /** A reply that the user overtook by writing again has been stopped in its session; the turn is not ended yet. */
        SUPERSEDED,
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
        /** The turn waits for people, its actions are on record; the Inbox has not suspended the run yet. */
        PARKING,
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
