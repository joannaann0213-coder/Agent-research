package dev.agentflow.agent.cma;

import static dev.agentflow.agent.cma.fake.ModelStep.builtin;
import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static dev.agentflow.agent.cma.fake.ModelStep.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import dev.agentflow.agent.cma.AgentSpec.ToolPolicy;
import dev.agentflow.agent.cma.CmaEvent.Permission;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import dev.agentflow.agent.cma.fake.FakeBrains;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the agent service relies on from {@link ManagedAgents}, whichever implementation is behind
 * it. The suite runs twice: on the in-process fake, and on the SDK adapter talking HTTP to a stub
 * that answers in the API's wire format with the same fake behind it. A test scripts the fake and
 * observes through {@link #cma}.
 */
abstract class ManagedAgentsContract {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration WAIT = Duration.ofSeconds(10);

    /** A clock that moves a millisecond on every reading, so that event order and time order agree. */
    private final AtomicLong tick = new AtomicLong();

    protected FakeManagedAgents fake;
    protected ManagedAgents cma;
    private final List<EventStream> open = new ArrayList<>();

    /** The implementation under test, backed by {@code fake}. */
    protected abstract ManagedAgents connect(FakeManagedAgents fake);

    @BeforeEach
    void connect() {
        fake = new FakeManagedAgents(() -> Instant.parse("2026-11-20T10:00:00Z").plusMillis(tick.incrementAndGet()));
        cma = connect(fake);
    }

    @AfterEach
    void closeStreams() {
        open.forEach(EventStream::close);
    }

    // ================================================================ set-up

    @Test
    void anAgentIsCreatedWithEverythingTheSpecificationSays() {
        AgentSpec spec = new AgentSpec("Support assistant", "Answers customers", "claude-opus-5-5", "high",
                "You are a support assistant.", new AgentSpec.Sandbox(ToolPolicy.AUTO, false, true),
                List.of(new AgentSpec.McpServer("tickets", "https://mcp.example.com/tickets", ToolPolicy.ALWAYS_ASK),
                        new AgentSpec.McpServer("wiki", "https://mcp.example.com/wiki", ToolPolicy.ALWAYS_ALLOW)),
                List.of(new AgentSpec.CustomTool("notify_user", "Sends the user a notification.", schema("""
                        {"type": "object", "properties": {"text": {"type": "string", "description": "What to say"},
                         "urgent": {"type": "boolean"}}, "required": ["text"], "additionalProperties": false}"""))),
                Map.of("app", "acme", "profile", "support"));

        AgentRef agent = cma.createAgent(spec);

        assertThat(agent.id()).isNotBlank();
        assertThat(agent.version()).isEqualTo(1);
        assertThat(fake.agentSpec(agent)).isEqualTo(spec);
    }

    @Test
    void anAgentWithoutToolsServersOrOptionalFieldsIsFine() {
        AgentSpec bare = new AgentSpec("Bare", null, "claude-opus-5-5", null, null, null, null, null, null);

        assertThat(fake.agentSpec(cma.createAgent(bare))).isEqualTo(bare);
    }

    @Test
    void anUpdateMakesANewVersion_onlyWhenSomethingChanged_andOnlyFromTheCurrentVersion() {
        AgentSpec first = chatAgent();
        AgentRef v1 = cma.createAgent(first);

        AgentRef same = cma.updateAgent(v1, first);
        assertThat(same).isEqualTo(v1);

        AgentSpec second = new AgentSpec(first.name(), null, first.model(), "low", "Be brief.", null, List.of(),
                List.of(), Map.of());
        AgentRef v2 = cma.updateAgent(v1, second);
        assertThat(v2).isEqualTo(new AgentRef(v1.id(), 2));
        assertThat(fake.agentSpec(v2)).isEqualTo(second);
        assertThat(fake.agentSpec(v1)).as("the old version is untouched").isEqualTo(first);

        CmaException stale = catchThrowableOfType(CmaException.class, () -> cma.updateAgent(v1, first));
        assertThat(stale.kind()).isEqualTo(CmaException.Kind.CONFLICT);
        assertThat(stale.retryable()).isFalse();

        assertThat(cma.getAgent(v1.id())).as("the current version, to start the next update from").isEqualTo(v2);
        assertThat(cma.updateAgent(cma.getAgent(v1.id()), first)).isEqualTo(new AgentRef(v1.id(), 3));
        assertThat(kindOf(() -> cma.getAgent("agent_nope"))).isEqualTo(CmaException.Kind.NOT_FOUND);
    }

    @Test
    void anEnvironmentKeepsItsNetworkRules() {
        EnvironmentSpec limited = new EnvironmentSpec("acme", "Containers of app acme",
                List.of("api.example.com", "files.example.com"), true, false, Map.of("app", "acme"));
        String id = cma.createEnvironment(limited);
        assertThat(fake.environmentSpec(id)).isEqualTo(limited);

        EnvironmentSpec open = new EnvironmentSpec("acme", "Containers of app acme", null, false, false, Map.of("app", "acme"));
        cma.updateEnvironment(id, open);
        assertThat(fake.environmentSpec(id).unrestricted()).isTrue();

        assertThat(fake.environmentSpec(cma.createEnvironment(
                new EnvironmentSpec("closed", null, List.of(), false, false, null))).allowedHosts()).isEmpty();
    }

    @Test
    void aSessionStartsIdle_pinnedToItsAgentVersion_andDoesNothingUntilAnEventIsSent() {
        AgentRef v1 = cma.createAgent(chatAgent());
        cma.updateAgent(v1, new AgentSpec("Chat", null, "claude-opus-5-5", null, "Changed.", null, null, null, null));
        String environment = cma.createEnvironment(new EnvironmentSpec("env", null, null, false, false, null));

        String session = cma.createSession(new SessionSpec(v1, environment, "conversation c1",
                Map.of("app", "acme", "user", "u42"), List.of("vlt_01")));

        SessionInfo info = cma.getSession(session);
        assertThat(info.status()).isEqualTo(SessionInfo.Status.IDLE);
        assertThat(info.usable()).isTrue();
        assertThat(info.agent()).isEqualTo(v1);
        assertThat(info.metadata()).containsEntry("app", "acme").containsEntry("user", "u42");
        assertThat(fake.sessionSpec(session).vaultIds()).containsExactly("vlt_01");
        assertThat(fake.sessionSpec(session).title()).isEqualTo("conversation c1");
        assertThat(cma.listEvents(session, null)).isEmpty();
    }

    @Test
    void whatDoesNotExistIsNotFound() {
        assertThat(kindOf(() -> cma.getSession("sesn_nope"))).isEqualTo(CmaException.Kind.NOT_FOUND);
        assertThat(kindOf(() -> cma.listEvents("sesn_nope", null))).isEqualTo(CmaException.Kind.NOT_FOUND);
        assertThat(kindOf(() -> cma.send("sesn_nope", List.of(new Outgoing.UserMessage("hi")))))
                .isEqualTo(CmaException.Kind.NOT_FOUND);
        assertThat(kindOf(() -> cma.openStream("sesn_nope"))).isEqualTo(CmaException.Kind.NOT_FOUND);
        assertThat(kindOf(() -> cma.createSession(new SessionSpec(new AgentRef("agent_nope", 1), "env_nope", null, null, null))))
                .isEqualTo(CmaException.Kind.NOT_FOUND);
    }

    // ================================================================ a turn

    @Test
    void aMessageIsAnswered_andTheStreamShowsTheTurnFromTheQueuedMessageToTheIdleSession() {
        fake.brain(context -> say("Hello, " + context.userMessage() + "!"));
        String session = newSession(chatAgent());
        EventStream stream = stream(session);

        List<CmaEvent> stored = cma.send(session, List.of(new Outgoing.UserMessage("world")));

        assertThat(stored).singleElement().isInstanceOfSatisfying(CmaEvent.UserMessage.class, message -> {
            assertThat(message.id()).isNotBlank();
            assertThat(message.text()).isEqualTo("world");
        });
        List<CmaEvent> turn = until(stream, ManagedAgentsContract::turnOver);
        assertThat(types(turn)).containsExactly("user.message", "user.message", "session.status_running",
                "agent.message", "session.status_idle");
        assertThat(turn.get(0).processedAt()).as("queued first").isNull();
        assertThat(turn.get(1).processedAt()).as("then processed").isNotNull();
        assertThat(turn.get(1).id()).isEqualTo(turn.get(0).id()).isEqualTo(stored.getFirst().id());
        assertThat(turn.get(3)).isInstanceOfSatisfying(CmaEvent.AgentMessage.class,
                message -> assertThat(message.text()).isEqualTo("Hello, world!"));
        assertThat(turn.get(4)).isInstanceOfSatisfying(CmaEvent.Idle.class, idle -> {
            assertThat(idle.reason()).isEqualTo(StopReason.END_TURN);
            assertThat(idle.awaiting()).isEmpty();
        });
        assertThat(cma.getSession(session).status()).isEqualTo(SessionInfo.Status.IDLE);
    }

    @Test
    void theStoredListHasEveryEventOnce_inOrder_andCanBeReadFromAPointInTime() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        cma.send(session, List.of(new Outgoing.UserMessage("one")));
        cma.send(session, List.of(new Outgoing.UserMessage("two")));

        List<CmaEvent> all = cma.listEvents(session, null);

        assertThat(types(all)).containsExactly(
                "user.message", "session.status_running", "agent.message", "session.status_idle",
                "user.message", "session.status_running", "agent.message", "session.status_idle");
        assertThat(all).allSatisfy(event -> assertThat(event.processedAt()).isNotNull());
        assertThat(all).extracting(CmaEvent::id).doesNotHaveDuplicates();
        assertThat(((CmaEvent.AgentMessage) all.get(6)).text()).isEqualTo("You said: two");

        Instant secondMessage = all.get(4).processedAt();
        assertThat(types(cma.listEvents(session, secondMessage))).startsWith("session.status_running").hasSizeLessThan(5);
        assertThat(cma.listEvents(session, all.getLast().processedAt().plusSeconds(60))).isEmpty();
    }

    @Test
    void aStreamShowsOnlyWhatHappensAfterItWasOpened() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        cma.send(session, List.of(new Outgoing.UserMessage("before")));

        EventStream late = stream(session);
        assertThat(late.poll(Duration.ofMillis(300))).as("nothing of the finished turn").isEmpty();

        cma.send(session, List.of(new Outgoing.UserMessage("after")));
        assertThat(until(late, ManagedAgentsContract::turnOver)).filteredOn(CmaEvent.AgentMessage.class::isInstance)
                .singleElement().extracting(event -> ((CmaEvent.AgentMessage) event).text()).isEqualTo("You said: after");
    }

    @Test
    void twoStreamsOfOneSessionBothSeeEverything() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        EventStream first = stream(session);
        EventStream second = stream(session);

        cma.send(session, List.of(new Outgoing.UserMessage("hi")));

        assertThat(types(until(first, ManagedAgentsContract::turnOver)))
                .isEqualTo(types(until(second, ManagedAgentsContract::turnOver)));
    }

    // ================================================================ custom tools

    @Test
    void aCustomToolCallWaitsForItsResult_andTheAgentGoesOnWithIt() {
        fake.brain(context -> context.step() == 0
                ? ModelStep.of(text("Let me look."), custom("read_events", "{\"limit\": 5}"))
                : say("The events say: " + context.resultOf("read_events").text()));
        String session = newSession(toolAgent());
        EventStream stream = stream(session);

        cma.send(session, List.of(new Outgoing.UserMessage("what is new?")));

        List<CmaEvent> first = until(stream, ManagedAgentsContract::idle);
        assertThat(types(first)).containsExactly("user.message", "user.message", "session.status_running",
                "agent.message", "agent.custom_tool_use", "session.status_idle");
        CmaEvent.CustomToolUse call = (CmaEvent.CustomToolUse) first.get(4);
        assertThat(call.name()).isEqualTo("read_events");
        assertThat(call.input().path("limit").asInt()).isEqualTo(5);
        assertThat(first.get(5)).isInstanceOfSatisfying(CmaEvent.Idle.class, idle -> {
            assertThat(idle.reason()).isEqualTo(StopReason.REQUIRES_ACTION);
            assertThat(idle.awaiting()).containsExactly(call.id());
        });
        assertThat(cma.getSession(session).status()).isEqualTo(SessionInfo.Status.IDLE);

        cma.send(session, List.of(new Outgoing.CustomToolResult(call.id(), "order 1234 is late", false)));

        List<CmaEvent> second = until(stream, ManagedAgentsContract::turnOver);
        assertThat(types(second)).containsExactly("user.custom_tool_result", "session.status_running",
                "agent.message", "session.status_idle");
        assertThat(second.get(0)).isInstanceOfSatisfying(CmaEvent.UserCustomToolResult.class, result -> {
            assertThat(result.customToolUseId()).isEqualTo(call.id());
            assertThat(result.processedAt()).as("a result is processed on receipt").isNotNull();
        });
        assertThat(((CmaEvent.AgentMessage) second.get(2)).text()).isEqualTo("The events say: order 1234 is late");
    }

    @Test
    void aFailedCustomToolIsToldToTheAgentAsAnError() {
        fake.brain(context -> context.step() == 0 ? ModelStep.of(custom("notify_user", "{\"text\": \"hi\"}"))
                : say(context.resultOf("notify_user").error() ? "It failed: " + context.resultOf("notify_user").text() : "Sent."));
        String session = newSession(toolAgent());
        EventStream stream = stream(session);
        cma.send(session, List.of(new Outgoing.UserMessage("go")));
        String call = lastOf(until(stream, ManagedAgentsContract::idle), CmaEvent.CustomToolUse.class).id();

        cma.send(session, List.of(new Outgoing.CustomToolResult(call, "the user has no device", true)));

        assertThat(lastOf(until(stream, ManagedAgentsContract::turnOver), CmaEvent.AgentMessage.class).text())
                .isEqualTo("It failed: the user has no device");
    }

    @Test
    void severalCallsInOneStepAllWait_andTheTurnGoesOnWhenTheLastOneIsAnswered() {
        fake.brain(context -> context.step() == 0
                ? ModelStep.of(custom("read_events", "{}"), custom("notify_user", "{\"text\": \"a\"}"))
                : say("done with " + context.toolResults().size() + " results"));
        String session = newSession(toolAgent());
        EventStream stream = stream(session);
        cma.send(session, List.of(new Outgoing.UserMessage("go")));
        List<CmaEvent> waiting = until(stream, ManagedAgentsContract::idle);
        List<String> calls = waiting.stream().filter(CmaEvent.CustomToolUse.class::isInstance).map(CmaEvent::id).toList();
        assertThat(calls).hasSize(2);
        assertThat(((CmaEvent.Idle) waiting.getLast()).awaiting()).containsExactlyElementsOf(calls);

        cma.send(session, List.of(new Outgoing.CustomToolResult(calls.get(0), "first", false)));
        assertThat(stream.poll(Duration.ofMillis(300)).map(CmaEvent::type)).contains("user.custom_tool_result");
        assertThat(stream.poll(Duration.ofMillis(300))).as("the session still waits for the second answer").isEmpty();

        cma.send(session, List.of(new Outgoing.CustomToolResult(calls.get(1), "second", false)));
        assertThat(lastOf(until(stream, ManagedAgentsContract::turnOver), CmaEvent.AgentMessage.class).text())
                .isEqualTo("done with 2 results");
    }

    @Test
    void whileACallWaits_aMessageIsRefused_andSoIsAnAnswerToAnythingElse() {
        fake.brain(context -> context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("ok"));
        String session = newSession(toolAgent());
        EventStream stream = stream(session);
        cma.send(session, List.of(new Outgoing.UserMessage("go")));
        String call = lastOf(until(stream, ManagedAgentsContract::idle), CmaEvent.CustomToolUse.class).id();

        CmaException refused = catchThrowableOfType(CmaException.class,
                () -> cma.send(session, List.of(new Outgoing.UserMessage("are you there?"))));
        assertThat(refused.kind()).isEqualTo(CmaException.Kind.INVALID_REQUEST);
        assertThat(refused.maybeDelivered()).isFalse();
        assertThat(kindOf(() -> cma.send(session, List.of(new Outgoing.CustomToolResult("sevt_other", "x", false)))))
                .isEqualTo(CmaException.Kind.INVALID_REQUEST);
        assertThat(kindOf(() -> cma.send(session, List.of(new Outgoing.ToolConfirmation(call, true, null)))))
                .as("a custom tool call takes a result, not a confirmation").isEqualTo(CmaException.Kind.INVALID_REQUEST);
        assertThat(fake.userMessages(session)).as("nothing of the refused requests was stored").containsExactly("go");

        cma.send(session, List.of(new Outgoing.CustomToolResult(call, "fine", false)));
        until(stream, ManagedAgentsContract::turnOver);
        assertThat(kindOf(() -> cma.send(session, List.of(new Outgoing.CustomToolResult(call, "again", false)))))
                .as("a call is answered once").isEqualTo(CmaException.Kind.INVALID_REQUEST);
    }

    // ================================================================ tools the platform runs

    @Test
    void anMcpCallThatNeedsPermissionWaits_runsOnceWhenAllowed_andNotAtAllWhenDenied() {
        fake.platformTool("tickets", "close_ticket", input -> ToolOutcome.ok("closed " + input.path("id").asString()));
        fake.brain(context -> context.step() == 0
                ? ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}"), mcp("tickets", "close_ticket", "{\"id\": \"T-2\"}"))
                : say(context.toolResults().stream().map(result -> result.error() ? "refused: " + result.text() : result.text())
                        .toList().toString()));
        String session = newSession(mcpAgent(ToolPolicy.ALWAYS_ASK));
        EventStream stream = stream(session);
        cma.send(session, List.of(new Outgoing.UserMessage("close both")));

        List<CmaEvent> waiting = until(stream, ManagedAgentsContract::idle);
        List<CmaEvent.ToolUse> calls = waiting.stream().filter(CmaEvent.ToolUse.class::isInstance).map(CmaEvent.ToolUse.class::cast).toList();
        assertThat(calls).hasSize(2).allSatisfy(call -> {
            assertThat(call.mcpServer()).isEqualTo("tickets");
            assertThat(call.name()).isEqualTo("close_ticket");
            assertThat(call.permission()).isEqualTo(Permission.ASK);
            assertThat(call.type()).isEqualTo("agent.mcp_tool_use");
        });
        assertThat(calls.get(0).input().path("id").asString()).isEqualTo("T-1");
        assertThat(((CmaEvent.Idle) waiting.getLast()).awaiting()).containsExactly(calls.get(0).id(), calls.get(1).id());
        assertThat(fake.executions()).as("nothing ran yet").isEmpty();

        cma.send(session, List.of(new Outgoing.ToolConfirmation(calls.get(0).id(), true, null),
                new Outgoing.ToolConfirmation(calls.get(1).id(), false, "the customer objected")));

        List<CmaEvent> rest = until(stream, ManagedAgentsContract::turnOver);
        assertThat(lastOf(rest, CmaEvent.AgentMessage.class).text())
                .isEqualTo("[closed T-1, refused: the customer objected]");
        assertThat(rest.stream().filter(CmaEvent.ToolResult.class::isInstance).map(CmaEvent.ToolResult.class::cast))
                .extracting(CmaEvent.ToolResult::toolUseId, CmaEvent.ToolResult::error, CmaEvent.ToolResult::mcp)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(calls.get(0).id(), false, true),
                        org.assertj.core.groups.Tuple.tuple(calls.get(1).id(), true, true));
        assertThat(fake.executions()).singleElement().satisfies(execution -> {
            assertThat(execution.server()).isEqualTo("tickets");
            assertThat(execution.input().path("id").asString()).isEqualTo("T-1");
        });
    }

    @Test
    void aCallThatIsAlwaysAllowedRunsWithoutAsking() {
        fake.brain(context -> context.step() == 0 ? ModelStep.of(mcp("tickets", "list_tickets", "{}"), builtin("bash", "{\"command\": \"ls\"}"))
                : say("ran " + context.toolResults().size()));
        AgentSpec agent = new AgentSpec("Ops", null, "claude-opus-5-5", null, null,
                new AgentSpec.Sandbox(ToolPolicy.ALWAYS_ALLOW, false, false),
                List.of(new AgentSpec.McpServer("tickets", "https://mcp.example.com/tickets", ToolPolicy.ALWAYS_ALLOW)), null, null);
        String session = newSession(agent);
        EventStream stream = stream(session);

        cma.send(session, List.of(new Outgoing.UserMessage("go")));

        List<CmaEvent> turn = until(stream, ManagedAgentsContract::turnOver);
        assertThat(types(turn)).containsExactly("user.message", "user.message", "session.status_running",
                "agent.mcp_tool_use", "agent.mcp_tool_result", "agent.tool_use", "agent.tool_result",
                "agent.message", "session.status_idle");
        assertThat(turn.stream().filter(CmaEvent.ToolUse.class::isInstance).map(CmaEvent.ToolUse.class::cast))
                .extracting(CmaEvent.ToolUse::permission).containsOnly(Permission.ALLOW);
        assertThat(lastOf(turn, CmaEvent.ToolUse.class).mcpServer()).as("a built-in tool has no server").isNull();
        assertThat(fake.executions()).hasSize(2);
    }

    @Test
    void underTheAutoPolicyACallRunsIsDeniedOrAsks_asThePlatformJudgesIt() {
        fake.autoJudge((server, tool, input) -> switch (input.path("risk").asString("")) {
            case "low" -> Permission.ALLOW;
            case "high" -> Permission.DENY;
            default -> Permission.ASK;
        });
        fake.brain(context -> context.step() == 0
                ? ModelStep.of(mcp("tickets", "act", "{\"risk\": \"low\"}"), mcp("tickets", "act", "{\"risk\": \"high\"}"),
                        mcp("tickets", "act", "{\"risk\": \"unclear\"}"))
                : say("done"));
        String session = newSession(mcpAgent(ToolPolicy.AUTO));
        EventStream stream = stream(session);

        cma.send(session, List.of(new Outgoing.UserMessage("go")));

        List<CmaEvent> waiting = until(stream, ManagedAgentsContract::idle);
        List<CmaEvent.ToolUse> calls = waiting.stream().filter(CmaEvent.ToolUse.class::isInstance).map(CmaEvent.ToolUse.class::cast).toList();
        assertThat(calls).extracting(CmaEvent.ToolUse::permission)
                .containsExactly(Permission.ALLOW, Permission.DENY, Permission.ASK);
        assertThat(((CmaEvent.Idle) waiting.getLast()).awaiting()).as("only the unclear call waits").containsExactly(calls.get(2).id());
        assertThat(kindOf(() -> cma.send(session, List.of(new Outgoing.ToolConfirmation(calls.get(1).id(), true, null)))))
                .as("a denial by the platform cannot be overridden").isEqualTo(CmaException.Kind.INVALID_REQUEST);
        assertThat(fake.executions()).hasSize(1);
    }

    // ================================================================ interrupts and queues

    @Test
    void anInterruptStopsARunningTurn_whichThenEndsLikeAnyOther() {
        String session = newSession(chatAgent());
        EventStream stream = stream(session);
        fake.hold(session);
        cma.send(session, List.of(new Outgoing.UserMessage("write a long report")));
        until(stream, event -> event instanceof CmaEvent.Running);
        assertThat(cma.getSession(session).status()).isEqualTo(SessionInfo.Status.RUNNING);

        cma.send(session, List.of(new Outgoing.Interrupt()));

        List<CmaEvent> stopped = until(stream, ManagedAgentsContract::idle);
        assertThat(types(stopped)).containsExactly("user.interrupt", "session.status_idle");
        assertThat(((CmaEvent.Idle) stopped.getLast()).reason()).isEqualTo(StopReason.END_TURN);
        assertThat(cma.getSession(session).status()).isEqualTo(SessionInfo.Status.IDLE);
        fake.release(session);
        assertThat(stream.poll(Duration.ofMillis(300))).as("the stopped turn produces nothing more").isEmpty();
    }

    @Test
    void anInterruptAlsoEndsATurnThatWaitsForAnAnswer_andTheSessionTakesMessagesAgain() {
        fake.brain(context -> context.userMessage().equals("go") && context.step() == 0
                ? ModelStep.of(custom("read_events", "{}")) : say("answer to " + context.userMessage()));
        String session = newSession(toolAgent());
        EventStream stream = stream(session);
        cma.send(session, List.of(new Outgoing.UserMessage("go")));
        String call = lastOf(until(stream, ManagedAgentsContract::idle), CmaEvent.CustomToolUse.class).id();

        cma.send(session, List.of(new Outgoing.Interrupt()));
        assertThat(((CmaEvent.Idle) until(stream, ManagedAgentsContract::idle).getLast()).reason()).isEqualTo(StopReason.END_TURN);
        assertThat(kindOf(() -> cma.send(session, List.of(new Outgoing.CustomToolResult(call, "late", false)))))
                .as("the call is gone with the turn").isEqualTo(CmaException.Kind.INVALID_REQUEST);

        cma.send(session, List.of(new Outgoing.UserMessage("next")));
        assertThat(lastOf(until(stream, ManagedAgentsContract::turnOver), CmaEvent.AgentMessage.class).text())
                .isEqualTo("answer to next");
    }

    @Test
    void anInterruptOfAnIdleSessionChangesNothing() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        EventStream stream = stream(session);

        cma.send(session, List.of(new Outgoing.Interrupt()));
        assertThat(until(stream, event -> event instanceof CmaEvent.UserInterrupt)).hasSize(1);
        assertThat(stream.poll(Duration.ofMillis(300))).isEmpty();

        cma.send(session, List.of(new Outgoing.UserMessage("still there?")));
        assertThat(lastOf(until(stream, ManagedAgentsContract::turnOver), CmaEvent.AgentMessage.class).text())
                .isEqualTo("You said: still there?");
    }

    @Test
    void aMessageSentDuringATurnWaitsItsTurn() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        EventStream stream = stream(session);
        fake.hold(session);
        cma.send(session, List.of(new Outgoing.UserMessage("first")));
        cma.send(session, List.of(new Outgoing.UserMessage("second")));
        assertThat(cma.listEvents(session, null).stream().filter(CmaEvent.UserMessage.class::isInstance))
                .extracting(CmaEvent::processedAt).as("the first is taken up, the second is queued")
                .satisfiesExactly(first -> assertThat(first).isNotNull(), second -> assertThat(second).isNull());

        fake.release(session);

        List<CmaEvent> both = until(stream, event -> event instanceof CmaEvent.AgentMessage message && message.text().endsWith("second"));
        assertThat(both.stream().filter(CmaEvent.AgentMessage.class::isInstance).map(event -> ((CmaEvent.AgentMessage) event).text()))
                .containsExactly("You said: first", "You said: second");
    }

    // ================================================================ failures

    @Test
    void aRefusalByTheServiceSaysHowLongToWait() {
        String session = newSession(chatAgent());
        fake.failNext(Operation.SEND, CmaException.rateLimited(Duration.ofSeconds(7)));

        CmaException limited = catchThrowableOfType(CmaException.class,
                () -> cma.send(session, List.of(new Outgoing.UserMessage("hi"))));

        assertThat(limited.kind()).isEqualTo(CmaException.Kind.RATE_LIMITED);
        assertThat(limited.retryAfter()).isEqualTo(Duration.ofSeconds(7));
        assertThat(limited.retryable()).isTrue();
        assertThat(limited.maybeDelivered()).isFalse();
        assertThat(fake.userMessages(session)).isEmpty();
    }

    @Test
    void anOverloadedServiceIsReportedAsSuch_andASendIsNotRepeatedBehindTheCallersBack() {
        String session = newSession(chatAgent());
        fake.failNext(Operation.SEND, CmaException.overloaded());

        CmaException overloaded = catchThrowableOfType(CmaException.class,
                () -> cma.send(session, List.of(new Outgoing.UserMessage("hi"))));

        assertThat(overloaded.kind()).isEqualTo(CmaException.Kind.OVERLOADED);
        assertThat(overloaded.retryable()).isTrue();
        assertThat(fake.count(Operation.SEND)).as("one attempt, no automatic retry").isEqualTo(1);
    }

    @Test
    void aSendWhoseAnswerIsLostMayHaveBeenDelivered_andTheStoredEventsTellWhether() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        fake.failNextAfterEffect(Operation.SEND, new CmaException(CmaException.Kind.SERVER, "the answer was lost"));

        CmaException lost = catchThrowableOfType(CmaException.class,
                () -> cma.send(session, List.of(new Outgoing.UserMessage("did you get this?"))));

        assertThat(lost.maybeDelivered()).isTrue();
        assertThat(cma.listEvents(session, null).stream().filter(CmaEvent.UserMessage.class::isInstance)
                .map(event -> ((CmaEvent.UserMessage) event).text())).containsExactly("did you get this?");
        assertThat(fake.count(Operation.SEND)).isEqualTo(1);
    }

    @Test
    void aBrokenStreamSaysSo_andANewOneWithTheStoredEventsCoversTheGap() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        EventStream stream = stream(session);
        cma.send(session, List.of(new Outgoing.UserMessage("one")));
        until(stream, ManagedAgentsContract::turnOver);

        fake.dropStreams(session);
        cma.send(session, List.of(new Outgoing.UserMessage("two")));

        CmaException lost = catchThrowableOfType(CmaException.class, () -> until(stream, ManagedAgentsContract::turnOver));
        assertThat(lost.kind()).isEqualTo(CmaException.Kind.CONNECTION);
        EventStream again = stream(session);
        assertThat(again.poll(Duration.ofMillis(300))).as("the new stream has no replay").isEmpty();
        assertThat(cma.listEvents(session, null).stream().filter(CmaEvent.AgentMessage.class::isInstance)
                .map(event -> ((CmaEvent.AgentMessage) event).text())).containsExactly("You said: one", "You said: two");
    }

    @Test
    void aTerminatedSessionSaysSoAndTakesNothingMore() {
        String session = newSession(chatAgent());
        EventStream stream = stream(session);

        fake.terminate(session);

        assertThat(until(stream, event -> event instanceof CmaEvent.Terminated)).hasSize(1);
        SessionInfo info = cma.getSession(session);
        assertThat(info.status()).isEqualTo(SessionInfo.Status.TERMINATED);
        assertThat(info.usable()).isFalse();
        assertThat(kindOf(() -> cma.send(session, List.of(new Outgoing.UserMessage("hello?")))))
                .isEqualTo(CmaException.Kind.INVALID_REQUEST);
    }

    @Test
    void anErrorInsideASessionIsReportedWithWhatThePlatformDoesAboutIt() {
        String session = newSession(chatAgent());
        EventStream stream = stream(session);

        fake.reportError(session, "model_overloaded_error", "The model is overloaded.", CmaEvent.Error.Retry.RETRYING);
        fake.reportError(session, "mcp_connection_failed_error", "Cannot reach tickets.", null);

        List<CmaEvent> errors = until(stream, event -> event instanceof CmaEvent.Error error && error.retry() == null);
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0)).isInstanceOfSatisfying(CmaEvent.Error.class, error -> {
            assertThat(error.errorType()).isEqualTo("model_overloaded_error");
            assertThat(error.message()).isEqualTo("The model is overloaded.");
            assertThat(error.retry()).isEqualTo(CmaEvent.Error.Retry.RETRYING);
        });
        assertThat(cma.listEvents(session, null)).hasSize(2).allSatisfy(event -> assertThat(event).isInstanceOf(CmaEvent.Error.class));
    }

    // ================================================================ the tail

    @Test
    void aTailHandsOutStoredEventsThenLiveOnes_eachOnce_acrossABrokenStream() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        cma.send(session, List.of(new Outgoing.UserMessage("one")));

        try (EventTail tail = EventTail.open(cma, session, null)) {
            List<CmaEvent> seen = new ArrayList<>();
            seen.addAll(untilTail(tail, ManagedAgentsContract::turnOver));
            assertThat(texts(seen)).containsExactly("You said: one");

            cma.send(session, List.of(new Outgoing.UserMessage("two")));
            seen.addAll(untilTail(tail, ManagedAgentsContract::turnOver));

            fake.dropStreams(session);
            cma.send(session, List.of(new Outgoing.UserMessage("three")));
            seen.addAll(untilTail(tail, ManagedAgentsContract::turnOver));
            cma.send(session, List.of(new Outgoing.UserMessage("four")));
            seen.addAll(untilTail(tail, ManagedAgentsContract::turnOver));

            assertThat(texts(seen)).containsExactly("You said: one", "You said: two", "You said: three", "You said: four");
            assertThat(seen.stream().filter(event -> event.processedAt() != null).map(CmaEvent::id)).doesNotHaveDuplicates();
            assertThat(tail.poll(Duration.ofMillis(200))).isEmpty();
        }
    }

    @Test
    void aTailThatStartsAtAPointInTimeLeavesOutWhatCameBefore() {
        fake.brain(FakeBrains.echo());
        String session = newSession(chatAgent());
        cma.send(session, List.of(new Outgoing.UserMessage("old")));
        Instant mark = cma.listEvents(session, null).getLast().processedAt().plusMillis(1);

        try (EventTail tail = EventTail.open(cma, session, mark)) {
            cma.send(session, List.of(new Outgoing.UserMessage("new")));
            assertThat(texts(untilTail(tail, ManagedAgentsContract::turnOver))).containsExactly("You said: new");
        }
    }

    // ================================================================ helpers

    protected static AgentSpec chatAgent() {
        return new AgentSpec("Chat", null, "claude-opus-5-5", null, "You are helpful.", null, null, null, null);
    }

    protected static AgentSpec toolAgent() {
        return new AgentSpec("Tools", null, "claude-opus-5-5", null, null, null, null, List.of(
                new AgentSpec.CustomTool("read_events", "Reads the events that woke you.", schema("{\"type\": \"object\", \"properties\": {\"limit\": {\"type\": \"integer\"}}}")),
                new AgentSpec.CustomTool("notify_user", "Sends the user a notification.", schema("{\"type\": \"object\", \"properties\": {\"text\": {\"type\": \"string\"}}, \"required\": [\"text\"]}"))),
                null);
    }

    protected static AgentSpec mcpAgent(ToolPolicy policy) {
        return new AgentSpec("Tickets", null, "claude-opus-5-5", null, null, null,
                List.of(new AgentSpec.McpServer("tickets", "https://mcp.example.com/tickets", policy)), null, null);
    }

    protected String newSession(AgentSpec agent) {
        return cma.createSession(new SessionSpec(cma.createAgent(agent),
                cma.createEnvironment(new EnvironmentSpec("env", null, null, false, false, null)), null, null, null));
    }

    protected EventStream stream(String session) {
        EventStream stream = cma.openStream(session);
        open.add(stream);
        return stream;
    }

    /** Reads until an event matches, and returns everything read except events this service does not act on. */
    protected static List<CmaEvent> until(EventStream stream, Predicate<CmaEvent> last) {
        return read(stream::poll, last);
    }

    protected static List<CmaEvent> untilTail(EventTail tail, Predicate<CmaEvent> last) {
        return read(tail::poll, last);
    }

    private static List<CmaEvent> read(java.util.function.Function<Duration, Optional<CmaEvent>> source, Predicate<CmaEvent> last) {
        List<CmaEvent> events = new ArrayList<>();
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<CmaEvent> next = source.apply(Duration.ofMillis(100));
            if (next.isEmpty() || next.get() instanceof CmaEvent.Other) {
                continue;
            }
            events.add(next.get());
            if (last.test(next.get())) {
                return events;
            }
        }
        throw new AssertionError("the expected event did not arrive within " + WAIT + "; got " + types(events));
    }

    protected static boolean idle(CmaEvent event) {
        return event instanceof CmaEvent.Idle;
    }

    /** The session is idle and waits for nobody: the turn is over. */
    protected static boolean turnOver(CmaEvent event) {
        return event instanceof CmaEvent.Idle idle && idle.reason() != StopReason.REQUIRES_ACTION;
    }

    protected static List<String> types(List<CmaEvent> events) {
        return events.stream().map(CmaEvent::type).toList();
    }

    private static List<String> texts(List<CmaEvent> events) {
        return events.stream().filter(CmaEvent.AgentMessage.class::isInstance).map(event -> ((CmaEvent.AgentMessage) event).text()).toList();
    }

    protected static <T extends CmaEvent> T lastOf(List<CmaEvent> events, Class<T> type) {
        return events.reversed().stream().filter(type::isInstance).map(type::cast).findFirst()
                .orElseThrow(() -> new AssertionError("no " + type.getSimpleName() + " among " + types(events)));
    }

    protected static CmaException.Kind kindOf(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        CmaException error = catchThrowableOfType(CmaException.class, call);
        assertThat(error).as("a CmaException was expected").isNotNull();
        return error.kind();
    }

    protected static JsonNode schema(String json) {
        return JSON.readTree(json);
    }
}
