package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * In demo mode the agent platform lives in the service's memory, so a restart of the service makes it
 * forget everything that the database still names. The service goes on as before all the same.
 */
class DemoRestartIT extends AgentTestBase {

    @Autowired
    AgentProfiles profiles;

    @Test
    void afterARestartInDemoMode_conversationsGoOnAndAgentsCanBeChanged() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say("Before the restart."));
        userSays(acme, "u42", "one");
        settle();

        // The service restarts: the platform is made anew, as the demo mode makes it.
        cma.reset();
        cma.restoreForgotten(profiles::platformAgent);
        cma.startIdsAt(1_000_000);
        cma.brain(context -> say("After the restart."));

        userSays(acme, "u42", "two");
        assertThat(settle()).isEqualTo(1);
        assertThat(messagesOf(acme, "u42")).extracting(MessageView::text)
                .containsExactly("one", "Before the restart.", "two", "After the restart.");

        AgentProfileView changed = acme.put("/v1/agents/default",
                new AgentProfileRequest("Assistant", null, null, "You are a brief assistant.", null, null)).as(AgentProfileView.class);
        assertThat(changed.revision()).isEqualTo(2);
        userSays(acme, "u42", "three");
        assertThat(settle()).isEqualTo(1);
        assertThat(messagesOf(acme, "u42")).extracting(MessageView::text).endsWith("After the restart.");
        assertThat(cma.agentSpec(cma.sessionSpec(cma.sessionIds().getLast()).agent()).system()).contains("You are a brief assistant.");
    }
}
