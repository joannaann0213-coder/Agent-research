package dev.agentflow.agent.cma.fake;

import dev.agentflow.agent.cma.AgentRef;
import dev.agentflow.agent.cma.AgentSpec;
import dev.agentflow.agent.cma.AgentSpec.ToolPolicy;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.CmaEvent.Permission;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.EnvironmentSpec;
import dev.agentflow.agent.cma.EventStream;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.SessionInfo;
import dev.agentflow.agent.cma.SessionSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;

/**
 * An in-process stand-in for Claude Managed Agents. It keeps environments, agents and sessions in
 * memory and behaves like the service does at the level the agent service depends on: queued
 * messages, tool calls that wait for an answer, permission policies, interrupts, streams without
 * replay. The model is a {@link FakeBrain}, a script.
 *
 * <p>Everything happens inside the call that causes it: by the time {@link #send} returns, the
 * events of the resulting work are already on the open streams. That keeps tests deterministic. To
 * get a session that is still working when a call returns, freeze its model with {@link #hold}.
 *
 * <p>Where the service's documentation does not say what happens, the fake assumes the following.
 * The live smoke test checks each of them against the real service.
 *
 * <ul>
 *   <li>A call that is approved runs at once, also while other calls of the same step still wait;
 *       the session then goes idle again and names the calls that remain.
 *   <li>An interrupt ends a turn that waits for answers, and the waiting calls are gone with it.
 *   <li>A message sent while the session waits for answers is refused, and the whole request with
 *       it, even when the same request carries the answers.
 *   <li>An answer to a call that is not waiting, or not waiting any more, is refused.
 * </ul>
 *
 * <p>Used by the tests of the agent service and by its local demo mode. It is not a simulator of
 * the model's behaviour, limits or cost.
 */
public final class FakeManagedAgents implements ManagedAgents {

    /** The calls of the port, for fault injection and for assertions. */
    public enum Operation {
        CREATE_ENVIRONMENT,
        UPDATE_ENVIRONMENT,
        CREATE_AGENT,
        UPDATE_AGENT,
        CREATE_SESSION,
        GET_SESSION,
        LIST_EVENTS,
        OPEN_STREAM,
        SEND
    }

    /**
     * A call that was made on the port.
     *
     * @param target the session, agent or environment the call was about; {@code null} for a create
     */
    public record Call(Operation operation, String target, Object argument) {
    }

    /** A tool call that the platform ran. */
    public record Execution(String sessionId, String server, String tool, JsonNode input) {
    }

    /** Decides a call under the {@code auto} policy. */
    @FunctionalInterface
    public interface AutoJudge {
        Permission judge(String server, String tool, JsonNode input);
    }

    private static final int MAX_STEPS_PER_TURN = 50;

    private static final Set<String> BUILT_IN_TOOLS =
            Set.of("bash", "read", "write", "edit", "glob", "grep", "web_fetch", "web_search");

    private final Object lock = new Object();
    private final Supplier<Instant> clock;
    private final Map<String, EnvironmentSpec> environments = new LinkedHashMap<>();
    private final Map<String, List<AgentSpec>> agents = new LinkedHashMap<>();
    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final Map<Operation, Deque<Fault>> faults = new EnumMap<>(Operation.class);
    private final Map<String, Function<JsonNode, ToolOutcome>> platformTools = new LinkedHashMap<>();
    private final List<Call> calls = new ArrayList<>();
    private final List<Execution> executions = new ArrayList<>();
    private FakeBrain brain = FakeBrains.echo();
    private AutoJudge autoJudge = (server, tool, input) -> Permission.ALLOW;
    private boolean interruptsHaveIds = true;
    private long nextId = 1;

    public FakeManagedAgents() {
        this(Instant::now);
    }

    /** @param clock gives the time that events are stamped with */
    public FakeManagedAgents(Supplier<Instant> clock) {
        this.clock = clock;
    }

    // ================================================================ scripting

    /** Sets the script that plays the model, for every session. */
    public void brain(FakeBrain brain) {
        synchronized (lock) {
            this.brain = brain;
        }
    }

    /**
     * Says what a tool that the platform runs returns: a tool of an MCP server, or a built-in tool
     * when {@code server} is {@code null}. A tool nothing was said about returns {@code ok}.
     */
    public void platformTool(String server, String tool, Function<JsonNode, ToolOutcome> behaviour) {
        synchronized (lock) {
            platformTools.put(toolKey(server, tool), behaviour);
        }
    }

    /** Sets how calls are judged under the {@code auto} policy. By default all of them are allowed. */
    public void autoJudge(AutoJudge judge) {
        synchronized (lock) {
            this.autoJudge = judge;
        }
    }

    /** The service stores some interrupts without an id. Pass {@code false} to get those. */
    public void interruptsHaveIds(boolean value) {
        synchronized (lock) {
            this.interruptsHaveIds = value;
        }
    }

    /**
     * Freezes the model of a session: work that reaches the model stops there, and the session stays
     * {@code running} until {@link #release}. A message sent meanwhile is taken up but not answered.
     */
    public void hold(String sessionId) {
        synchronized (lock) {
            session(sessionId).held = true;
        }
    }

    /** Lets a frozen model continue. The events of the work that follows are emitted by this call. */
    public void release(String sessionId) {
        synchronized (lock) {
            Session session = session(sessionId);
            session.held = false;
            drive(session);
        }
    }

    /** Ends a session for good, as the service does after an error it cannot recover from. */
    public void terminate(String sessionId) {
        synchronized (lock) {
            Session session = session(sessionId);
            session.status = SessionInfo.Status.TERMINATED;
            session.inTurn = false;
            session.awaiting.clear();
            emit(session, new CmaEvent.Terminated(newEventId(), clock.get()));
        }
    }

    /** Reports an error inside a session, as the service does. The session's state does not change. */
    public void reportError(String sessionId, String errorType, String message, CmaEvent.Error.Retry retry) {
        synchronized (lock) {
            emit(session(sessionId), new CmaEvent.Error(newEventId(), clock.get(), errorType, message, retry));
        }
    }

    /** Breaks every open stream of a session, as a network failure would. */
    public void dropStreams(String sessionId) {
        synchronized (lock) {
            Session session = session(sessionId);
            session.streams.forEach(FakeStream::drop);
            session.streams.clear();
        }
    }

    /** Makes the next call of {@code operation} fail before it does anything. */
    public void failNext(Operation operation, CmaException error) {
        synchronized (lock) {
            faults.computeIfAbsent(operation, o -> new ArrayDeque<>()).add(new Fault(error, false));
        }
    }

    /**
     * Makes the next call of {@code operation} take effect and then fail, as when the response is
     * lost on the way back.
     */
    public void failNextAfterEffect(Operation operation, CmaException error) {
        synchronized (lock) {
            faults.computeIfAbsent(operation, o -> new ArrayDeque<>()).add(new Fault(error, true));
        }
    }

    // ================================================================ inspection

    /** Every call made on the port so far, including the ones that were made to fail. */
    public List<Call> calls() {
        synchronized (lock) {
            return List.copyOf(calls);
        }
    }

    public long count(Operation operation) {
        synchronized (lock) {
            return calls.stream().filter(call -> call.operation() == operation).count();
        }
    }

    /** Every tool call the platform ran, in order. */
    public List<Execution> executions() {
        synchronized (lock) {
            return List.copyOf(executions);
        }
    }

    public List<String> sessionIds() {
        synchronized (lock) {
            return List.copyOf(sessions.keySet());
        }
    }

    public SessionSpec sessionSpec(String sessionId) {
        synchronized (lock) {
            return session(sessionId).spec;
        }
    }

    /** The configuration of one version of an agent. */
    public AgentSpec agentSpec(AgentRef agent) {
        synchronized (lock) {
            return version(agent);
        }
    }

    public EnvironmentSpec environmentSpec(String environmentId) {
        synchronized (lock) {
            return environment(environmentId);
        }
    }

    /** The texts of every message a session has received, oldest first. */
    public List<String> userMessages(String sessionId) {
        synchronized (lock) {
            return session(sessionId).log.stream()
                    .filter(event -> event instanceof CmaEvent.UserMessage)
                    .map(event -> ((CmaEvent.UserMessage) event).text())
                    .toList();
        }
    }

    /** How many streams of a session are open. */
    public int openStreams(String sessionId) {
        synchronized (lock) {
            return session(sessionId).streams.size();
        }
    }

    // ================================================================ the port: set-up

    @Override
    public String createEnvironment(EnvironmentSpec spec) {
        synchronized (lock) {
            Fault fault = begin(Operation.CREATE_ENVIRONMENT, null, spec);
            require(spec.name() != null && !spec.name().isBlank(), "an environment needs a name");
            String id = "env_" + sequence();
            environments.put(id, spec);
            end(fault);
            return id;
        }
    }

    @Override
    public void updateEnvironment(String environmentId, EnvironmentSpec spec) {
        synchronized (lock) {
            Fault fault = begin(Operation.UPDATE_ENVIRONMENT, environmentId, spec);
            environment(environmentId);
            environments.put(environmentId, spec);
            end(fault);
        }
    }

    @Override
    public AgentRef createAgent(AgentSpec spec) {
        synchronized (lock) {
            Fault fault = begin(Operation.CREATE_AGENT, null, spec);
            validate(spec);
            String id = "agent_" + sequence();
            List<AgentSpec> versions = new ArrayList<>();
            versions.add(spec);
            agents.put(id, versions);
            end(fault);
            return new AgentRef(id, 1);
        }
    }

    @Override
    public AgentRef updateAgent(AgentRef current, AgentSpec spec) {
        synchronized (lock) {
            Fault fault = begin(Operation.UPDATE_AGENT, current.id(), spec);
            List<AgentSpec> versions = agents.get(current.id());
            if (versions == null) {
                throw CmaException.notFound("no such agent: " + current.id());
            }
            if (current.version() != versions.size()) {
                throw CmaException.conflict("agent " + current.id() + " is at version " + versions.size()
                        + ", not " + current.version());
            }
            validate(spec);
            if (!versions.getLast().equals(spec)) {
                versions.add(spec);
            }
            end(fault);
            return new AgentRef(current.id(), versions.size());
        }
    }

    // ================================================================ the port: sessions

    @Override
    public String createSession(SessionSpec spec) {
        synchronized (lock) {
            Fault fault = begin(Operation.CREATE_SESSION, null, spec);
            AgentSpec agent = version(spec.agent());
            environment(spec.environmentId());
            require(spec.metadata().size() <= 8, "a session takes at most 8 metadata entries");
            String id = "sesn_" + sequence();
            sessions.put(id, new Session(id, spec, agent));
            end(fault);
            return id;
        }
    }

    @Override
    public SessionInfo getSession(String sessionId) {
        synchronized (lock) {
            Fault fault = begin(Operation.GET_SESSION, sessionId, null);
            Session session = session(sessionId);
            end(fault);
            return new SessionInfo(session.id, session.status, false, session.spec.agent(), session.spec.metadata());
        }
    }

    @Override
    public List<CmaEvent> listEvents(String sessionId, Instant createdSince) {
        synchronized (lock) {
            Fault fault = begin(Operation.LIST_EVENTS, sessionId, createdSince);
            Session session = session(sessionId);
            List<CmaEvent> found = new ArrayList<>();
            for (int i = 0; i < session.log.size(); i++) {
                if (createdSince == null || !session.createdAt.get(i).isBefore(createdSince)) {
                    found.add(session.log.get(i));
                }
            }
            end(fault);
            return found;
        }
    }

    @Override
    public EventStream openStream(String sessionId) {
        synchronized (lock) {
            Fault fault = begin(Operation.OPEN_STREAM, sessionId, null);
            Session session = session(sessionId);
            FakeStream stream = new FakeStream(session);
            session.streams.add(stream);
            end(fault);
            return stream;
        }
    }

    @Override
    public List<CmaEvent> send(String sessionId, List<Outgoing> events) {
        synchronized (lock) {
            Fault fault = begin(Operation.SEND, sessionId, List.copyOf(events));
            Session session = session(sessionId);
            check(session, events);
            List<CmaEvent> stored = new ArrayList<>();
            boolean wasWaiting = !session.awaiting.isEmpty();
            for (Outgoing event : events) {
                stored.add(accept(session, event));
            }
            if (wasWaiting && session.inTurn) {
                if (session.awaiting.isEmpty()) {
                    // every call has its answer: the session picks the turn up again
                    running(session);
                } else if (session.status == SessionInfo.Status.RUNNING) {
                    // an approved call ran, others still wait
                    idle(session, StopReason.REQUIRES_ACTION, List.copyOf(session.awaiting.keySet()));
                }
            }
            drive(session);
            end(fault);
            return stored;
        }
    }

    /** Refuses the whole request when any of its events is not acceptable; nothing is applied then. */
    private void check(Session session, List<Outgoing> events) {
        require(!events.isEmpty(), "a request needs at least one event");
        if (session.status == SessionInfo.Status.TERMINATED) {
            throw CmaException.invalid("session " + session.id + " is terminated");
        }
        Set<String> answered = new HashSet<>();
        for (Outgoing event : events) {
            switch (event) {
                case Outgoing.UserMessage message -> {
                    require(message.text() != null && !message.text().isEmpty(), "a message needs text");
                    if (!session.awaiting.isEmpty()) {
                        throw CmaException.invalid("session " + session.id + " is waiting for the answer to "
                                + session.awaiting.keySet() + "; until then it accepts only user.tool_confirmation, "
                                + "user.custom_tool_result and user.interrupt");
                    }
                }
                case Outgoing.Interrupt interrupt -> {
                }
                case Outgoing.ToolConfirmation confirmation ->
                        checkAnswer(session, confirmation.toolUseId(), false, answered);
                case Outgoing.CustomToolResult result ->
                        checkAnswer(session, result.customToolUseId(), true, answered);
            }
        }
    }

    private void checkAnswer(Session session, String eventId, boolean custom, Set<String> answered) {
        Waiting waiting = session.awaiting.get(eventId);
        if (waiting == null || waiting.custom != custom || !answered.add(eventId)) {
            throw CmaException.invalid("no " + (custom ? "custom tool call" : "tool call that waits for permission")
                    + " with id " + eventId + " is waiting for an answer in session " + session.id);
        }
    }

    private CmaEvent accept(Session session, Outgoing outgoing) {
        Instant now = clock.get();
        switch (outgoing) {
            case Outgoing.UserMessage message -> {
                CmaEvent.UserMessage queued = new CmaEvent.UserMessage(newEventId(), null, message.text());
                emit(session, queued);
                session.queued.add(queued.id());
                return queued;
            }
            case Outgoing.Interrupt interrupt -> {
                CmaEvent.UserInterrupt stored = new CmaEvent.UserInterrupt(interruptsHaveIds ? newEventId() : "", now);
                emit(session, stored);
                if (session.inTurn) {
                    // The turn stops where it is. An interrupted turn ends like any other one.
                    session.inTurn = false;
                    session.awaiting.clear();
                    session.results.clear();
                    idle(session, StopReason.END_TURN, List.of());
                }
                return stored;
            }
            case Outgoing.ToolConfirmation confirmation -> {
                CmaEvent.UserToolConfirmation stored = new CmaEvent.UserToolConfirmation(newEventId(), now,
                        confirmation.toolUseId(), confirmation.allow());
                emit(session, stored);
                Waiting waiting = session.awaiting.remove(confirmation.toolUseId());
                if (confirmation.allow()) {
                    // an approved call runs at once, whether or not other calls still wait
                    running(session);
                    run(session, waiting.eventId, waiting.server, waiting.tool, waiting.input);
                } else {
                    String why = confirmation.denyMessage() != null ? confirmation.denyMessage()
                            : "Permission to use " + waiting.tool + " has been denied.";
                    result(session, waiting.eventId, waiting.server, waiting.tool, ToolOutcome.failed(why));
                }
                return stored;
            }
            case Outgoing.CustomToolResult result -> {
                CmaEvent.UserCustomToolResult stored =
                        new CmaEvent.UserCustomToolResult(newEventId(), now, result.customToolUseId());
                emit(session, stored);
                Waiting waiting = session.awaiting.remove(result.customToolUseId());
                session.results.add(new ToolOutcome(waiting.eventId, null, waiting.tool, result.text(), result.error()));
                return stored;
            }
        }
    }

    // ================================================================ the engine

    /** Advances a session as far as it can go: until it waits for the caller, is frozen, or has nothing to do. */
    private void drive(Session session) {
        while (session.status != SessionInfo.Status.TERMINATED && session.awaiting.isEmpty()) {
            if (!session.inTurn) {
                String next = session.queued.poll();
                if (next == null) {
                    return;
                }
                session.turnMessage = processed(session, next);
                session.inTurn = true;
                session.step = 0;
                session.results.clear();
                running(session);
            }
            if (session.held) {
                return;
            }
            if (session.step >= MAX_STEPS_PER_TURN) {
                throw new IllegalStateException("the script made " + MAX_STEPS_PER_TURN
                        + " steps in one turn of session " + session.id + " without ending it");
            }
            ModelStep step = brain.next(new ModelContext(session.id, session.agent, session.spec.metadata(),
                    session.turnMessage, session.step, List.copyOf(session.results), List.copyOf(session.log)));
            session.step++;
            session.results.clear();
            for (ModelStep.Block block : step.blocks()) {
                perform(session, block);
            }
            if (!session.awaiting.isEmpty()) {
                idle(session, StopReason.REQUIRES_ACTION, List.copyOf(session.awaiting.keySet()));
            } else if (!step.callsTools()) {
                session.inTurn = false;
                idle(session, StopReason.END_TURN, List.of());
            }
        }
    }

    private void perform(Session session, ModelStep.Block block) {
        Instant now = clock.get();
        switch (block) {
            case ModelStep.Text text -> emit(session, new CmaEvent.AgentMessage(newEventId(), now, text.text()));
            case ModelStep.Custom call -> {
                if (session.agent.customTools().stream().noneMatch(tool -> tool.name().equals(call.name()))) {
                    throw new IllegalStateException("the script calls the custom tool " + call.name()
                            + ", which the agent of session " + session.id + " does not have");
                }
                String id = newEventId();
                emit(session, new CmaEvent.CustomToolUse(id, now, call.name(), call.input()));
                session.awaiting.put(id, new Waiting(id, true, null, call.name(), call.input()));
            }
            case ModelStep.Mcp call -> platformCall(session, call.server(), call.name(), call.input(),
                    session.agent.mcpServers().stream().filter(server -> server.name().equals(call.server()))
                            .map(AgentSpec.McpServer::policy).findFirst().orElse(null));
            case ModelStep.Builtin call -> platformCall(session, null, call.name(), call.input(),
                    session.agent.sandbox() == null || !enabled(session.agent.sandbox(), call.name())
                            ? null : session.agent.sandbox().policy());
        }
    }

    private static boolean enabled(AgentSpec.Sandbox sandbox, String tool) {
        return switch (tool) {
            case "web_search" -> sandbox.webSearch();
            case "web_fetch" -> sandbox.webFetch();
            default -> BUILT_IN_TOOLS.contains(tool);
        };
    }

    /** @param policy the policy that applies to the tool; {@code null} when the agent does not have it */
    private void platformCall(Session session, String server, String tool, JsonNode input, ToolPolicy policy) {
        Permission permission = policy == null ? Permission.DENY : switch (policy) {
            case ALWAYS_ALLOW -> Permission.ALLOW;
            case ALWAYS_ASK -> Permission.ASK;
            case AUTO -> autoJudge.judge(server, tool, input);
        };
        String id = newEventId();
        emit(session, new CmaEvent.ToolUse(id, clock.get(), tool, server, input, permission));
        switch (permission) {
            case ALLOW -> run(session, id, server, tool, input);
            case ASK -> session.awaiting.put(id, new Waiting(id, false, server, tool, input));
            case DENY -> result(session, id, server, tool,
                    ToolOutcome.failed("Permission to use " + tool + " has been denied."));
        }
    }

    private void run(Session session, String eventId, String server, String tool, JsonNode input) {
        executions.add(new Execution(session.id, server, tool, input));
        Function<JsonNode, ToolOutcome> behaviour = platformTools.get(toolKey(server, tool));
        result(session, eventId, server, tool, behaviour == null ? ToolOutcome.ok("ok") : behaviour.apply(input));
    }

    private void result(Session session, String eventId, String server, String tool, ToolOutcome outcome) {
        emit(session, new CmaEvent.ToolResult(newEventId(), clock.get(), eventId, outcome.error(), server != null));
        session.results.add(outcome.of(eventId, server, tool));
    }

    private void running(Session session) {
        if (session.status != SessionInfo.Status.RUNNING) {
            session.status = SessionInfo.Status.RUNNING;
            emit(session, new CmaEvent.Running(newEventId(), clock.get()));
        }
    }

    private void idle(Session session, StopReason reason, List<String> awaiting) {
        session.status = SessionInfo.Status.IDLE;
        emit(session, new CmaEvent.Idle(newEventId(), clock.get(), reason, awaiting));
    }

    /** Stamps a queued message as processed. The stream shows it a second time; the stored list shows it once. */
    private String processed(Session session, String eventId) {
        for (int i = 0; i < session.log.size(); i++) {
            if (session.log.get(i) instanceof CmaEvent.UserMessage message && message.id().equals(eventId)) {
                CmaEvent.UserMessage done = new CmaEvent.UserMessage(eventId, clock.get(), message.text());
                session.log.set(i, done);
                session.streams.forEach(stream -> stream.offer(done));
                return message.text();
            }
        }
        throw new IllegalStateException("queued message " + eventId + " is not in the log");
    }

    private void emit(Session session, CmaEvent event) {
        session.log.add(event);
        // an event that is processed at once was created at that same moment; a queued one, now
        session.createdAt.add(event.processedAt() != null ? event.processedAt() : clock.get());
        session.streams.forEach(stream -> stream.offer(event));
    }

    // ================================================================ helpers

    private Fault begin(Operation operation, String target, Object argument) {
        calls.add(new Call(operation, target, argument));
        Deque<Fault> pending = faults.get(operation);
        Fault fault = pending == null ? null : pending.poll();
        if (fault != null && !fault.afterEffect) {
            throw fault.error;
        }
        return fault;
    }

    private static void end(Fault fault) {
        if (fault != null) {
            throw fault.error;
        }
    }

    private Session session(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw CmaException.notFound("no such session: " + sessionId);
        }
        return session;
    }

    private EnvironmentSpec environment(String environmentId) {
        EnvironmentSpec environment = environments.get(environmentId);
        if (environment == null) {
            throw CmaException.notFound("no such environment: " + environmentId);
        }
        return environment;
    }

    private AgentSpec version(AgentRef agent) {
        List<AgentSpec> versions = agents.get(agent.id());
        if (versions == null || agent.version() < 1 || agent.version() > versions.size()) {
            throw CmaException.notFound("no such agent version: " + agent);
        }
        return versions.get(agent.version() - 1);
    }

    /** The limits the service documents, so that a configuration it would refuse is refused here too. */
    private static void validate(AgentSpec spec) {
        require(spec.name() != null && !spec.name().isEmpty() && spec.name().length() <= 256,
                "an agent's name has 1 to 256 characters");
        require(spec.model() != null && !spec.model().isBlank(), "an agent needs a model");
        require(spec.system() == null || spec.system().length() <= 100_000,
                "an agent's system prompt has at most 100,000 characters");
        require(spec.description() == null || spec.description().length() <= 2048,
                "an agent's description has at most 2048 characters");
        require(spec.metadata().size() <= 16, "an agent takes at most 16 metadata entries");
        spec.metadata().forEach((key, value) -> require(key.length() <= 64 && value.length() <= 512,
                "an agent's metadata keys have at most 64 characters and its values at most 512"));
        require(spec.mcpServers().size() <= 20, "an agent takes at most 20 MCP servers");
        Set<String> servers = new HashSet<>();
        for (AgentSpec.McpServer server : spec.mcpServers()) {
            require(servers.add(server.name()), "MCP server names are unique: " + server.name());
            require(server.url() != null && server.url().startsWith("https://"), "an MCP server needs an https URL");
        }
        Set<String> tools = new HashSet<>();
        for (AgentSpec.CustomTool tool : spec.customTools()) {
            require(tools.add(tool.name()), "custom tool names are unique: " + tool.name());
            require(!tool.name().startsWith("mcp__") && !tool.name().startsWith("ant__")
                            && !BUILT_IN_TOOLS.contains(tool.name()),
                    "a custom tool's name cannot start with mcp__ or ant__ or be a built-in tool's name: " + tool.name());
            require(tool.inputSchema() != null && "object".equals(tool.inputSchema().path("type").asString("")),
                    "a custom tool needs an input schema of type object: " + tool.name());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw CmaException.invalid(message);
        }
    }

    private static String toolKey(String server, String tool) {
        return (server == null ? "" : server) + "/" + tool;
    }

    private String sequence() {
        return String.format("%06d", nextId++);
    }

    private String newEventId() {
        return "sevt_" + sequence();
    }

    private record Fault(CmaException error, boolean afterEffect) {
    }

    /** A call that waits for the caller: a custom tool call, or a platform call that needs permission. */
    private record Waiting(String eventId, boolean custom, String server, String tool, JsonNode input) {
    }

    private static final class Session {
        final String id;
        final SessionSpec spec;
        final AgentSpec agent;
        final List<CmaEvent> log = new ArrayList<>();
        final List<Instant> createdAt = new ArrayList<>();
        final Deque<String> queued = new ArrayDeque<>();
        final Map<String, Waiting> awaiting = new LinkedHashMap<>();
        final List<ToolOutcome> results = new ArrayList<>();
        final List<FakeStream> streams = new ArrayList<>();
        SessionInfo.Status status = SessionInfo.Status.IDLE;
        boolean inTurn;
        boolean held;
        int step;
        String turnMessage;

        Session(String id, SessionSpec spec, AgentSpec agent) {
            this.id = id;
            this.spec = spec;
            this.agent = agent;
        }
    }

    private final class FakeStream implements EventStream {

        private static final Object DROPPED = new Object();

        private final Session session;
        private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        private volatile boolean closed;

        FakeStream(Session session) {
            this.session = session;
        }

        void offer(CmaEvent event) {
            queue.add(event);
        }

        void drop() {
            queue.add(DROPPED);
        }

        @Override
        public Optional<CmaEvent> poll(Duration timeout) {
            if (closed) {
                throw CmaException.connection("the stream is closed", null);
            }
            Object next;
            try {
                next = queue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw CmaException.connection("interrupted while waiting for an event", e);
            }
            if (next == DROPPED) {
                closed = true;
                throw CmaException.connection("the stream of session " + session.id + " was lost", null);
            }
            return Optional.ofNullable((CmaEvent) next);
        }

        @Override
        public void close() {
            closed = true;
            synchronized (lock) {
                session.streams.remove(this);
            }
        }
    }
}
