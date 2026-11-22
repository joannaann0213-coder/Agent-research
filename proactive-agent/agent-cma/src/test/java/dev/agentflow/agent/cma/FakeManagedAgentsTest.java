package dev.agentflow.agent.cma;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.agentflow.agent.cma.AgentSpec.ToolPolicy;
import dev.agentflow.agent.cma.fake.FakeBrains;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelStep;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** What is particular to the fake: its scripting, its fault injection, and the limits it enforces. */
class FakeManagedAgentsTest {

    private FakeManagedAgents fake;

    @BeforeEach
    void create() {
        fake = new FakeManagedAgents();
    }

    @Test
    void theModelSeesTheMessageTheStepTheResultsAndTheSession() {
        List<String> seen = new ArrayList<>();
        fake.brain(context -> {
            seen.add(context.step() + ":" + context.userMessage() + ":" + context.toolResults().size()
                    + ":" + context.sessionMetadata().get("conversation") + ":" + context.agent().name()
                    + ":" + context.userMessages());
            return context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("done");
        });
        String session = fake.createSession(new SessionSpec(fake.createAgent(ManagedAgentsContract.toolAgent()),
                fake.createEnvironment(new EnvironmentSpec("env", null, null, false, false, null)), null,
                Map.of("conversation", "c1"), null));

        fake.send(session, List.of(new Outgoing.UserMessage("go")));
        String call = fake.listEvents(session, null).stream().filter(CmaEvent.CustomToolUse.class::isInstance)
                .findFirst().orElseThrow().id();
        fake.send(session, List.of(new Outgoing.CustomToolResult(call, "three events", false)));

        assertThat(seen).containsExactly("0:go:0:c1:Tools:[go]", "1:go:1:c1:Tools:[go]");
    }

    @Test
    void aSequenceIsPlayedStepByStep_andSaysWhenItRunsOut() {
        fake.brain(FakeBrains.sequence(say("first"), say("second")));
        String session = session(ManagedAgentsContract.chatAgent());

        for (int i = 0; i < 3; i++) {
            fake.send(session, List.of(new Outgoing.UserMessage("next")));
        }

        assertThat(fake.listEvents(session, null).stream().filter(CmaEvent.AgentMessage.class::isInstance)
                .map(event -> ((CmaEvent.AgentMessage) event).text()))
                .containsExactly("first", "second", "(the script has no step 3)");
    }

    @Test
    void aScriptThatCallsAToolTheAgentDoesNotHaveIsAMistakeInTheTest() {
        fake.brain(context -> ModelStep.of(custom("launch_rocket", "{}")));
        String session = session(ManagedAgentsContract.toolAgent());

        assertThatThrownBy(() -> fake.send(session, List.of(new Outgoing.UserMessage("go"))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("launch_rocket");
    }

    @Test
    void aScriptThatNeverEndsItsTurnIsStopped() {
        fake.brain(context -> ModelStep.of(ModelStep.mcp("tickets", "list", "{}")));
        String session = session(ManagedAgentsContract.mcpAgent(ToolPolicy.ALWAYS_ALLOW));

        assertThatThrownBy(() -> fake.send(session, List.of(new Outgoing.UserMessage("go"))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("50 steps");
    }

    @Test
    void aToolTheAgentDoesNotHaveIsDeniedByThePlatform() {
        fake.brain(context -> context.step() == 0
                ? ModelStep.of(ModelStep.builtin("bash", "{}"), ModelStep.mcp("unknown", "x", "{}"), ModelStep.builtin("web_search", "{}"))
                : say("results: " + context.toolResults().stream().filter(result -> result.error()).count()));
        AgentSpec noWeb = new AgentSpec("Sandboxed", null, "claude-opus-5-5", null, null,
                new AgentSpec.Sandbox(ToolPolicy.ALWAYS_ALLOW, false, false), null, null, null);
        String session = session(noWeb);

        fake.send(session, List.of(new Outgoing.UserMessage("go")));

        List<CmaEvent> events = fake.listEvents(session, null);
        assertThat(events.stream().filter(CmaEvent.ToolUse.class::isInstance).map(event -> ((CmaEvent.ToolUse) event).permission()))
                .containsExactly(CmaEvent.Permission.ALLOW, CmaEvent.Permission.DENY, CmaEvent.Permission.DENY);
        assertThat(((CmaEvent.AgentMessage) events.get(events.size() - 2)).text()).isEqualTo("results: 2");
        assertThat(fake.executions()).hasSize(1);
    }

    @Test
    void theDocumentedLimitsAreEnforced() {
        JsonMapper json = JsonMapper.builder().build();
        var schema = json.readTree("{\"type\": \"object\"}");

        assertInvalid(() -> fake.createAgent(new AgentSpec("", null, "claude-opus-5-5", null, null, null, null, null, null)));
        assertInvalid(() -> fake.createAgent(new AgentSpec("A", null, null, null, null, null, null, null, null)));
        assertInvalid(() -> fake.createAgent(new AgentSpec("A", null, "m", null, "x".repeat(100_001), null, null, null, null)));
        assertInvalid(() -> fake.createAgent(new AgentSpec("A", null, "m", null, null, null, List.of(
                new AgentSpec.McpServer("s", "https://a.example.com", null), new AgentSpec.McpServer("s", "https://b.example.com", null)), null, null)));
        assertInvalid(() -> fake.createAgent(new AgentSpec("A", null, "m", null, null, null, List.of(
                new AgentSpec.McpServer("s", "http://plain.example.com", null)), null, null)));
        for (String name : List.of("mcp__x", "ant__x", "bash")) {
            assertInvalid(() -> fake.createAgent(new AgentSpec("A", null, "m", null, null, null, null,
                    List.of(new AgentSpec.CustomTool(name, "d", schema)), null)));
        }
        assertInvalid(() -> fake.createAgent(new AgentSpec("A", null, "m", null, null, null, null,
                List.of(new AgentSpec.CustomTool("t", "d", json.readTree("{\"type\": \"string\"}"))), null)));

        AgentRef agent = fake.createAgent(ManagedAgentsContract.chatAgent());
        String environment = fake.createEnvironment(new EnvironmentSpec("env", null, null, false, false, null));
        Map<String, String> nine = new java.util.HashMap<>();
        for (int i = 0; i < 9; i++) {
            nine.put("k" + i, "v");
        }
        assertInvalid(() -> fake.createSession(new SessionSpec(agent, environment, null, nine, null)));
        assertInvalid(() -> fake.send(fake.createSession(new SessionSpec(agent, environment, null, null, null)), List.of()));
    }

    @Test
    void someInterruptsHaveNoId() {
        fake.interruptsHaveIds(false);
        String session = session(ManagedAgentsContract.chatAgent());

        assertThat(fake.send(session, List.of(new Outgoing.Interrupt()))).singleElement()
                .isInstanceOfSatisfying(CmaEvent.UserInterrupt.class, interrupt -> assertThat(interrupt.id()).isEmpty());
    }

    @Test
    void faultsHitTheNextCallOnly_andEveryCallIsOnRecord() {
        String session = session(ManagedAgentsContract.chatAgent());
        fake.failNext(Operation.LIST_EVENTS, CmaException.overloaded());
        fake.failNext(Operation.LIST_EVENTS, CmaException.rateLimited(Duration.ofSeconds(1)));

        assertThat(ManagedAgentsContract.kindOf(() -> fake.listEvents(session, null))).isEqualTo(CmaException.Kind.OVERLOADED);
        assertThat(ManagedAgentsContract.kindOf(() -> fake.listEvents(session, null))).isEqualTo(CmaException.Kind.RATE_LIMITED);
        assertThat(fake.listEvents(session, null)).isEmpty();

        assertThat(fake.count(Operation.LIST_EVENTS)).isEqualTo(3);
        assertThat(fake.calls()).extracting(FakeManagedAgents.Call::operation).startsWith(Operation.CREATE_AGENT,
                Operation.CREATE_ENVIRONMENT, Operation.CREATE_SESSION, Operation.LIST_EVENTS);
        assertThat(fake.calls().get(2).argument()).isInstanceOf(SessionSpec.class);
        assertThat(fake.sessionIds()).containsExactly(session);
    }

    @Test
    void aStreamThatIsClosedIsForgotten() {
        String session = session(ManagedAgentsContract.chatAgent());
        EventStream stream = fake.openStream(session);
        assertThat(fake.openStreams(session)).isEqualTo(1);

        stream.close();

        assertThat(fake.openStreams(session)).isZero();
        assertThat(ManagedAgentsContract.kindOf(() -> stream.poll(Duration.ZERO))).isEqualTo(CmaException.Kind.CONNECTION);
    }

    private String session(AgentSpec agent) {
        return fake.createSession(new SessionSpec(fake.createAgent(agent),
                fake.createEnvironment(new EnvironmentSpec("env", null, null, false, false, null)), null, null, null));
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThat(ManagedAgentsContract.kindOf(call)).isEqualTo(CmaException.Kind.INVALID_REQUEST);
    }
}
