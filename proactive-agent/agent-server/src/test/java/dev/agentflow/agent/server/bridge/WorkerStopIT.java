package dev.agentflow.agent.server.bridge;

import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.inbox.client.InboxClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An instance that stops gives back at once what its last claims bring, instead of leaving it to its
 * lease. That costs the run nothing: it is not the run that failed.
 */
class WorkerStopIT extends AgentTestBase {

    @Autowired
    InboxClient inbox;

    @Autowired
    TurnRunner runner;

    @Autowired
    AgentProperties properties;

    @Test
    void aRunThatAStoppingInstanceClaims_goesBackAtOnce_andTheNextInstanceAnswersIt() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say("Hello."));
        userSays(acme, "u42", "hi");
        relay.runOnce();
        clock.advance(QUIET_WINDOW);

        TurnWorker stopping = new TurnWorker(inbox, runner, cma, properties);
        stopping.stopClaiming();
        assertThat(stopping.runOnce()).as("it claimed the run").isTrue();
        assertThat(cma.sessionIds()).as("and did not work on it").isEmpty();
        assertThat(runs()).as("the run has the attempt it had").containsExactly("queued attempt 1");

        assertThat(settle()).as("the run is ready again at once, without waiting for its lease").isEqualTo(1);
        assertThat(repliesTo(acme, "u42")).containsExactly("Hello.");
    }

    @Test
    void aRunIsNotLostToAnInstanceWhoseClaimsStillWaitWhileItStops() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say("Hello."));
        userSays(acme, "u42", "hi");
        relay.runOnce();
        clock.advance(QUIET_WINDOW);

        // Every claim loop of the instance has a claim waiting at the Inbox when it stops, and the run that one of
        // them gives back is handed to the next. More of them than a run has attempts.
        TurnWorker stopping = new TurnWorker(inbox, runner, cma, properties);
        stopping.stopClaiming();
        for (int loop = 1; loop <= properties.getWorker().getConcurrency(); loop++) {
            assertThat(stopping.runOnce()).as("claim %d is handed the run", loop).isTrue();
        }

        assertThat(runs()).as("the run is not dead, and has all its attempts").containsExactly("queued attempt 1");
        assertThat(settle()).isEqualTo(1);
        assertThat(repliesTo(acme, "u42")).as("the next instance answers the user").containsExactly("Hello.");
    }
}
