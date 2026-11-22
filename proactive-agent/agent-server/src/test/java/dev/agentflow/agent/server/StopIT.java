package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.ActionView;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.api.StopResult;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The user stops the agent's reply. Only that conversation's reply stops; what comes after is answered as usual. */
class StopIT extends AgentTestBase {

    private static final String USER = "u42";

    private TestApi acme;
    private TestApi alice;

    @BeforeEach
    void aConversation() {
        acme = app(ACME);
        defaultAgent(acme);
        alice = user(acme, USER);
        cma.brain(context -> say("You said: " + context.userMessage()));
    }

    @Test
    void aReplyUnderWayIsStopped_andWhatTheAgentWroteIsDropped() {
        userSays(alice, USER, "hello");
        settle();
        cma.hold(cma.sessionIds().getFirst());
        userSays(alice, USER, "write me a very long essay");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> {
            StopResult stopped = stop(alice, "default").as(StopResult.class);
            assertThat(stopped.inProgress()).isTrue();
            assertThat(stopped.stoppedThrough()).isEqualTo(3);
        });

        assertThat(worker.runOnce()).isTrue();

        assertThat(interrupts()).isEqualTo(1);
        assertThat(repliesTo(alice, USER)).containsExactly("You said: hello");
        assertThat(jdbc.sql("SELECT outcome FROM turn ORDER BY created_at, run_id").query(String.class).list())
                .containsExactly("replied", "stopped");
        assertThat(runs()).containsExactly("done attempt 1", "done attempt 1");

        userSays(alice, USER, "never mind, thanks");
        settle();
        assertThat(repliesTo(alice, USER)).as("what comes after the stop is answered")
                .containsExactly("You said: hello", "You said: never mind, thanks");
    }

    @Test
    void aReplyThatHasNotBegun_doesNotBegin() {
        userSays(alice, USER, "a question I regret");

        assertThat(stop(alice, "default").as(StopResult.class).inProgress()).isTrue();
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(alice, USER)).isEmpty();
        assertThat(cma.count(Operation.CREATE_SESSION)).as("the agent was not even woken").isZero();
        assertThat(jdbc.sql("SELECT outcome FROM turn").query(String.class).single()).isEqualTo("stopped");
    }

    @Test
    void messagesThatComeAfterTheStop_areAnswered_withoutTheOnesBeforeIt() {
        userSays(alice, USER, "first");
        stop(alice, "default").as(StopResult.class);
        userSays(alice, USER, "second");

        assertThat(settle()).as("the two messages in one run").isEqualTo(1);

        assertThat(repliesTo(alice, USER)).containsExactly("You said: second");
        assertThat(cma.userMessages(cma.sessionIds().getFirst())).containsExactly("second");
    }

    @Test
    void onlyTheNamedConversationStops() {
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "a", null, "in a")).as(MessageAccepted.class);
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "b", null, "in b")).as(MessageAccepted.class);
        acme.post("/v1/users/u42/events", new PostEventRequest(null, "order.shipped", "order-1", null, "a", null, null))
                .as(EventAccepted.class);

        stop(acme, "a").as(StopResult.class);
        settle();

        assertThat(repliesTo(acme, USER)).containsExactly("You said: in b");
        assertThat(jdbc.sql("SELECT merge_key || ' ' || outcome FROM turn ORDER BY merge_key").query(String.class).list())
                .containsExactly("conv:a stopped", "conv:b replied", "task:order-1 silent");
    }

    @Test
    void aStopWithdrawsWhatTheConversationWaitsFor() {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null,
                List.of(new McpServerConfig("tickets", "https://mcp.acme.example/tickets", "always_ask", "user")))).as(AgentProfileView.class);
        cma.brain(context -> context.step() == 0 ? ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}"))
                : say("Ticket: " + context.toolResults().getFirst().text()));
        userSays(alice, USER, "close ticket T-1");
        settle();
        ActionView waiting = alice.get("/v1/users/u42/actions").as(ActionPage.class).actions().getFirst();

        stop(alice, "default").as(StopResult.class);
        settle();

        ActionView withdrawn = alice.get("/v1/users/u42/actions/" + waiting.actionId()).as(ActionView.class);
        assertThat(withdrawn.status()).isEqualTo("withdrawn");
        assertThat(withdrawn.note()).isEqualTo("the user stopped the reply");
        assertThat(repliesTo(alice, USER)).isEmpty();
        assertThat(cma.executions()).isEmpty();
        assertThat(runs()).containsExactly("done attempt 1");
        assertThat(jdbc.sql("SELECT outcome FROM turn").query(String.class).single()).isEqualTo("stopped");
    }

    @Test
    void stoppingWhenNothingIsUnderWay_changesNothing() {
        userSays(alice, USER, "hello");
        settle();

        StopResult result = stop(alice, "default").as(StopResult.class);

        assertThat(result).isEqualTo(new StopResult("default", 2, false));
        assertThat(stop(alice, "never-used").as(StopResult.class)).isEqualTo(new StopResult("never-used", 0, false));
        assertThat(stop(acme.as(newKey(ACME)), "default").status()).isEqualTo(202);
        userSays(alice, USER, "still here?");
        settle();
        assertThat(repliesTo(alice, USER)).containsExactly("You said: hello", "You said: still here?");
    }

    @Test
    void aUserStopsOnlyTheirOwnConversations() {
        TestApi bob = user(acme, "bob");

        assertThat(stop(bob, "default").status()).isEqualTo(403);
        assertThat(anonymous.post("/v1/users/u42/conversations/default/stop", null).status()).isEqualTo(401);
        assertThat(alice.post("/v1/users/u42/conversations/bad%20id/stop", null).status()).isEqualTo(400);
    }

    private TestApi.Reply stop(TestApi caller, String conversationId) {
        return caller.post("/v1/users/" + USER + "/conversations/" + conversationId + "/stop", null);
    }

    private long interrupts() {
        return cma.calls().stream().filter(call -> call.operation() == Operation.SEND)
                .filter(call -> ((List<?>) call.argument()).stream().anyMatch(Outgoing.Interrupt.class::isInstance)).count();
    }
}
