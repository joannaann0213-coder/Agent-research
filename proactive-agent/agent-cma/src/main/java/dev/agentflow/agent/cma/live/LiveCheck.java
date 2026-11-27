package dev.agentflow.agent.cma.live;

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
import dev.agentflow.agent.cma.SessionSpec;
import dev.agentflow.agent.cma.sdk.SdkManagedAgents;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks, against the real service, the behaviour of Claude Managed Agents that the agent service
 * depends on and that the documentation leaves open. Each check prints what it saw and ends in a
 * verdict: as assumed, different, or not observed.
 *
 * <p>It needs an API key with access to Managed Agents in {@code ANTHROPIC_API_KEY} and makes a
 * handful of small model requests. It creates one environment, up to two agents and up to two
 * sessions, and leaves them in place so that the run can be looked at in the Console afterwards.
 *
 * <pre>
 * scripts/cma-live-check.sh [--model claude-opus-5-5] [--no-sandbox] [--timeout-seconds 180]
 * </pre>
 *
 * <p>The same class runs in the unit tests against the fake, which is how the fake and this check
 * are kept telling the same story.
 */
public final class LiveCheck {

    /** How a check ended. */
    public enum Verdict {
        /** The service does what the agent service and the fake assume. */
        AS_ASSUMED,
        /** The service does something else. The fake and the bridge have to be changed. */
        DIFFERENT,
        /** The situation did not come about in this run, for example because the model made one call instead of two. */
        NOT_OBSERVED
    }

    public record Finding(String check, Verdict verdict, String detail) {
    }

    /**
     * @param sandbox whether to run the check of tool confirmations, which starts a container
     * @param turnTimeout how long to wait for the model in one turn
     * @param quiet how long to listen before concluding that nothing more comes
     */
    public record Options(String model, boolean sandbox, Duration turnTimeout, Duration quiet) {

        public static Options defaults() {
            return new Options("claude-opus-5-5", true, Duration.ofSeconds(180), Duration.ofSeconds(4));
        }

        static Options parse(String[] args) {
            Options options = defaults();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--model" -> options = new Options(args[++i], options.sandbox, options.turnTimeout, options.quiet);
                    case "--no-sandbox" -> options = new Options(options.model, false, options.turnTimeout, options.quiet);
                    case "--timeout-seconds" -> options = new Options(options.model, options.sandbox,
                            Duration.ofSeconds(Long.parseLong(args[++i])), options.quiet);
                    default -> throw new IllegalArgumentException("unknown option: " + args[i]);
                }
            }
            return options;
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String INSTRUCTIONS = "You are a test fixture for an integration test. Do exactly what "
            + "the message asks, nothing more, and keep every reply to one short sentence.";

    private final ManagedAgents cma;
    private final Options options;
    private final PrintStream out;
    private final List<Finding> findings = new ArrayList<>();
    private final List<String> sessions = new ArrayList<>();

    public LiveCheck(ManagedAgents cma, Options options, PrintStream out) {
        this.cma = cma;
        this.options = options;
        this.out = out;
    }

    public static void main(String[] args) {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("Set ANTHROPIC_API_KEY to a key with access to Claude Managed Agents.");
            System.exit(2);
        }
        Options options = Options.parse(args);
        // The address is fixed on purpose: ANTHROPIC_BASE_URL may point somewhere else in a developer's shell.
        try (SdkManagedAgents cma = SdkManagedAgents.connect(key, "https://api.anthropic.com", Duration.ofSeconds(60))) {
            List<Finding> findings = new LiveCheck(cma, options, System.out).run();
            System.exit(findings.stream().anyMatch(finding -> finding.verdict() == Verdict.DIFFERENT) ? 1 : 0);
        } catch (CmaException e) {
            System.err.println("The check could not run: " + e.kind() + " (HTTP " + e.status() + "): " + e.getMessage());
            System.exit(2);
        }
    }

    /** Runs every check and prints a summary. */
    public List<Finding> run() {
        out.println("Claude Managed Agents live check, model " + options.model());
        String environment = cma.createEnvironment(new EnvironmentSpec("agentflow-live-check",
                "Created by the live check of the agent service. Safe to archive.", List.of(), false, false,
                Map.of("purpose", "live-check")));
        out.println("environment " + environment);

        AgentRef fixture = cma.createAgent(new AgentSpec("agentflow-live-check", "Fixture of the live check.",
                options.model(), "low", INSTRUCTIONS, null, null,
                List.of(new AgentSpec.CustomTool("probe_echo", "Returns the text it is given.", JSON.readTree(
                        "{\"type\": \"object\", \"properties\": {\"text\": {\"type\": \"string\"}}, \"required\": [\"text\"]}"))),
                Map.of("purpose", "live-check")));
        String session = newSession(fixture, environment, "live check: messages and custom tools");

        guarded("a plain turn", () -> plainTurn(session));
        guarded("the stored events", () -> storedEvents(session));
        guarded("a stream opened late", () -> lateStream(session));
        guarded("a custom tool call", () -> customTool(session));
        guarded("an interrupt while a call waits", () -> interruptWhileWaiting(session));
        guarded("an interrupt of an idle session", () -> interruptWhenIdle(session));

        if (options.sandbox()) {
            AgentRef sandboxed = cma.createAgent(new AgentSpec("agentflow-live-check-sandbox",
                    "Fixture of the live check, with a shell that asks before every call.", options.model(), "low",
                    INSTRUCTIONS, new AgentSpec.Sandbox(ToolPolicy.ALWAYS_ASK, false, false), null, null,
                    Map.of("purpose", "live-check")));
            String asking = newSession(sandboxed, environment, "live check: tool confirmations");
            guarded("tool confirmations", () -> confirmations(asking));
        } else {
            note("tool confirmations", Verdict.NOT_OBSERVED, "skipped with --no-sandbox");
        }

        out.println();
        out.println("Summary");
        for (Finding finding : findings) {
            out.printf("  %-13s %s%n      %s%n", finding.verdict(), finding.check(), finding.detail());
        }
        out.println();
        out.println("Sessions left in place (https://platform.claude.com/workspaces/default/sessions/<id>, "
                + "with your workspace in place of 'default'): " + sessions);
        return List.copyOf(findings);
    }

    // ================================================================ the checks

    private void plainTurn(String session) {
        try (EventStream stream = cma.openStream(session)) {
            List<CmaEvent> sent = cma.send(session, List.of(new Outgoing.UserMessage("Reply with the single word READY.")));
            List<CmaEvent> turn = read(stream, LiveCheck::turnOver, options.turnTimeout());
            trace(turn);

            boolean hasId = sent.size() == 1 && sent.getFirst().id() != null && !sent.getFirst().id().isBlank();
            note("a send returns the stored event with its id", hasId ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                    "returned " + describe(sent));

            // The bridge learns from the stream that its input was taken up. Whether the stream also shows the
            // message while it is still queued makes no difference to it.
            List<CmaEvent> echoes = turn.stream().filter(CmaEvent.UserMessage.class::isInstance).toList();
            boolean processed = hasId && !echoes.isEmpty()
                    && echoes.stream().allMatch(echo -> sent.getFirst().id().equals(echo.id()))
                    && echoes.getLast().processedAt() != null;
            note("a sent message shows on the stream as processed, under the id the send returned",
                    processed ? Verdict.AS_ASSUMED : Verdict.DIFFERENT, "saw " + describe(echoes));

            CmaEvent last = turn.getLast();
            note("a finished turn ends idle with end_turn",
                    last instanceof CmaEvent.Idle idle && idle.reason() == StopReason.END_TURN ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                    "ended with " + describe(List.of(last)) + "; the agent said: " + texts(turn));
        }
    }

    private void storedEvents(String session) {
        List<CmaEvent> stored = cma.listEvents(session, null);
        boolean ordered = true;
        Instant previous = Instant.MIN;
        for (CmaEvent event : stored) {
            if (event.processedAt() != null) {
                ordered &= !event.processedAt().isBefore(previous);
                previous = event.processedAt();
            }
        }
        long messages = stored.stream().filter(CmaEvent.UserMessage.class::isInstance).count();
        note("the stored list is oldest first and holds a sent message once",
                ordered && messages == 1 ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                stored.size() + " events: " + types(stored));

        List<CmaEvent> later = cma.listEvents(session, previous.plusSeconds(1));
        note("the stored list can be read from a point in time", later.isEmpty() ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                "from one second after the last event: " + types(later));
    }

    private void lateStream(String session) {
        try (EventStream stream = cma.openStream(session)) {
            List<CmaEvent> replayed = listen(stream, options.quiet());
            note("a stream has no replay", replayed.isEmpty() ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                    "a stream opened after the turn delivered " + types(replayed));
        }
    }

    private void customTool(String session) {
        try (EventStream stream = cma.openStream(session)) {
            cma.send(session, List.of(new Outgoing.UserMessage(
                    "Call the probe_echo tool once with the text ping. When you have its result, say what it returned.")));
            List<CmaEvent> first = read(stream, LiveCheck::idle, options.turnTimeout());
            trace(first);
            Optional<CmaEvent.CustomToolUse> call = last(first, CmaEvent.CustomToolUse.class);
            CmaEvent.Idle idle = (CmaEvent.Idle) first.getLast();
            boolean waits = call.isPresent() && idle.reason() == StopReason.REQUIRES_ACTION && idle.awaiting().contains(call.get().id());
            note("a custom tool call stops the session with requires_action and names the call",
                    waits ? Verdict.AS_ASSUMED : Verdict.DIFFERENT, "ended with " + describe(List.of(idle)));
            if (!waits) {
                return;
            }
            String id = call.get().id();

            Refusal message = refusal(() -> cma.send(session, List.of(new Outgoing.UserMessage("Are you there?"))));
            note("a message is refused while a call waits", message.verdict(), message.detail());

            Refusal unknown = refusal(() -> cma.send(session, List.of(new Outgoing.CustomToolResult("sevt_000000000000000000000000", "x", false))));
            note("a result for a call that does not exist is refused", unknown.verdict(), unknown.detail());

            Refusal both = refusal(() -> cma.send(session, List.of(new Outgoing.CustomToolResult(id, "ping", false),
                    new Outgoing.UserMessage("Also add the word EXTRA to your reply."))));
            note("a request with the answer and a message is refused as a whole", both.verdict(), both.detail());
            if (both.accepted()) {
                // the answer went in with that request
                trace(read(stream, LiveCheck::turnOver, options.turnTimeout()));
                listen(stream, options.quiet());
            } else {
                cma.send(session, List.of(new Outgoing.CustomToolResult(id, "ping", false)));
                List<CmaEvent> rest = read(stream, LiveCheck::turnOver, options.turnTimeout());
                trace(rest);
                note("the turn goes on with the result", texts(rest).toLowerCase().contains("ping") ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                        "the agent said: " + texts(rest));
            }

            Late again = late(stream, () -> cma.send(session, List.of(new Outgoing.CustomToolResult(id, "ping", false))));
            note("an answer to a call that was answered already changes nothing", again.verdict(), again.detail());
        }
    }

    private void interruptWhileWaiting(String session) {
        try (EventStream stream = cma.openStream(session)) {
            cma.send(session, List.of(new Outgoing.UserMessage("Call the probe_echo tool once with the text wait.")));
            List<CmaEvent> first = read(stream, LiveCheck::idle, options.turnTimeout());
            trace(first);
            Optional<CmaEvent.CustomToolUse> call = last(first, CmaEvent.CustomToolUse.class);
            if (call.isEmpty() || ((CmaEvent.Idle) first.getLast()).reason() != StopReason.REQUIRES_ACTION) {
                note("an interrupt ends a turn that waits for an answer", Verdict.NOT_OBSERVED, "the model did not call the tool");
                return;
            }

            List<CmaEvent> stored = cma.send(session, List.of(new Outgoing.Interrupt()));
            out.println("    the interrupt was stored as " + describe(stored));
            List<CmaEvent> after;
            try {
                after = read(stream, LiveCheck::idle, shorter(options.turnTimeout(), Duration.ofSeconds(60)));
            } catch (IllegalStateException nothing) {
                note("an interrupt ends a turn that waits for an answer", Verdict.DIFFERENT,
                        "the session stayed as it was; answering the call to move on");
                cma.send(session, List.of(new Outgoing.CustomToolResult(call.get().id(), "wait", false)));
                trace(read(stream, LiveCheck::turnOver, options.turnTimeout()));
                return;
            }
            trace(after);
            CmaEvent.Idle idle = (CmaEvent.Idle) after.getLast();
            note("an interrupt ends a turn that waits for an answer",
                    idle.reason() == StopReason.END_TURN ? Verdict.AS_ASSUMED : Verdict.DIFFERENT, "then idle with " + idle.reason());

            Late late = late(stream, () -> cma.send(session, List.of(new Outgoing.CustomToolResult(call.get().id(), "wait", false))));
            note("an answer to the call of an interrupted turn changes nothing", late.verdict(), late.detail());

            cma.send(session, List.of(new Outgoing.UserMessage("Reply with the single word AGAIN.")));
            List<CmaEvent> next = read(stream, LiveCheck::turnOver, options.turnTimeout());
            trace(next);
            note("after an interrupt the session takes messages again",
                    !texts(next).isBlank() ? Verdict.AS_ASSUMED : Verdict.DIFFERENT, "the agent said: " + texts(next));
        }
    }

    private void interruptWhenIdle(String session) {
        try (EventStream stream = cma.openStream(session)) {
            List<CmaEvent> stored = cma.send(session, List.of(new Outgoing.Interrupt()));
            List<CmaEvent> seen = listen(stream, options.quiet());
            boolean nothingHappened = seen.stream().allMatch(CmaEvent.UserInterrupt.class::isInstance);
            note("an interrupt of an idle session changes nothing", nothingHappened ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                    "stored as " + describe(stored) + "; the stream then showed " + types(seen));
        }
    }

    private void confirmations(String session) {
        try (EventStream stream = cma.openStream(session)) {
            cma.send(session, List.of(new Outgoing.UserMessage("Use the bash tool to run two commands, as two separate "
                    + "tool calls made at the same time: `echo one` and `echo two`. Then reply DONE.")));
            List<CmaEvent> first = read(stream, LiveCheck::idle, options.turnTimeout().multipliedBy(2));
            trace(first);
            List<CmaEvent.ToolUse> asked = first.stream().filter(CmaEvent.ToolUse.class::isInstance).map(CmaEvent.ToolUse.class::cast)
                    .filter(call -> call.permission() == Permission.ASK).toList();
            CmaEvent.Idle idle = (CmaEvent.Idle) first.getLast();
            boolean waits = !asked.isEmpty() && idle.reason() == StopReason.REQUIRES_ACTION
                    && idle.awaiting().containsAll(asked.stream().map(CmaEvent::id).toList());
            note("a call under always_ask stops the session with requires_action and names the call",
                    waits ? Verdict.AS_ASSUMED : Verdict.DIFFERENT, asked.size() + " call(s) asked; ended with " + describe(List.of(idle)));
            if (!waits) {
                return;
            }

            if (asked.size() >= 2) {
                cma.send(session, List.of(new Outgoing.ToolConfirmation(asked.get(0).id(), true, null)));
                // Wait for the approved call's result, but not for ever: it may only come once the other call is answered.
                List<CmaEvent> meanwhile = new ArrayList<>(collect(stream, event -> event instanceof CmaEvent.ToolResult result
                        && result.toolUseId().equals(asked.get(0).id()), shorter(options.turnTimeout(), Duration.ofSeconds(60))));
                meanwhile.addAll(listen(stream, options.quiet()));
                trace(meanwhile);
                boolean ranAtOnce = meanwhile.stream().anyMatch(event -> event instanceof CmaEvent.ToolResult result
                        && result.toolUseId().equals(asked.get(0).id()));
                boolean namesTheRest = meanwhile.stream().anyMatch(event -> event instanceof CmaEvent.Idle again
                        && again.reason() == StopReason.REQUIRES_ACTION && again.awaiting().contains(asked.get(1).id()));
                note("an approved call runs at once while another still waits", ranAtOnce ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                        "with one of two approved, the stream showed " + types(meanwhile));
                note("the session then goes idle again and names the call that remains",
                        !ranAtOnce ? Verdict.NOT_OBSERVED : namesTheRest ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                        "see the events above");
                cma.send(session, List.of(new Outgoing.ToolConfirmation(asked.get(1).id(), false, "Not needed for this test.")));
            } else {
                note("an approved call runs at once while another still waits", Verdict.NOT_OBSERVED,
                        "the model made one call at a time");
                cma.send(session, List.of(new Outgoing.ToolConfirmation(asked.get(0).id(), true, null)));
            }

            // Whatever else the model asks for is denied, so that the turn can end.
            List<CmaEvent> rest = new ArrayList<>();
            for (int round = 0; round < 6; round++) {
                List<CmaEvent> part = read(stream, LiveCheck::idle, options.turnTimeout().multipliedBy(2));
                rest.addAll(part);
                CmaEvent.Idle stop = (CmaEvent.Idle) part.getLast();
                if (stop.reason() != StopReason.REQUIRES_ACTION) {
                    break;
                }
                List<Outgoing> denials = new ArrayList<>();
                stop.awaiting().forEach(id -> denials.add(new Outgoing.ToolConfirmation(id, false, "Not needed for this test.")));
                cma.send(session, denials);
            }
            trace(rest);
            note("after the confirmations the turn ends", turnOver(rest.getLast()) ? Verdict.AS_ASSUMED : Verdict.DIFFERENT,
                    "the agent said: " + texts(rest));

            Late late = late(stream, () -> cma.send(session, List.of(new Outgoing.ToolConfirmation(asked.get(0).id(), true, null))));
            note("a confirmation for a call that no longer waits changes nothing", late.verdict(), late.detail());
        }
    }

    // ================================================================ helpers

    private String newSession(AgentRef agent, String environment, String title) {
        String session = cma.createSession(new SessionSpec(agent, environment, title, Map.of("purpose", "live-check"), null));
        sessions.add(session);
        out.println("session " + session + " (" + title + ")");
        return session;
    }

    private void guarded(String name, Runnable check) {
        out.println();
        out.println("== " + name);
        try {
            check.run();
        } catch (RuntimeException e) {
            note(name, Verdict.DIFFERENT, "the check failed: " + e);
        }
    }

    private void note(String check, Verdict verdict, String detail) {
        findings.add(new Finding(check, verdict, detail));
        out.println("  " + verdict + ": " + check + " - " + detail);
    }

    private record Refusal(boolean accepted, String detail) {
        Verdict verdict() {
            return accepted ? Verdict.DIFFERENT : Verdict.AS_ASSUMED;
        }
    }

    /**
     * What came of an answer that is sent although nothing waits for it: after a retry whose first
     * attempt got through, or after a turn was interrupted. The bridge needs it to be harmless. The
     * service may refuse it or take it, and a session that takes it may show itself running and
     * idle once more; what counts is that the call does not run and the model does not speak again.
     */
    private record Late(boolean harmless, String detail) {
        Verdict verdict() {
            return harmless ? Verdict.AS_ASSUMED : Verdict.DIFFERENT;
        }
    }

    private Late late(EventStream stream, Runnable answer) {
        Refusal refusal = refusal(answer);
        if (!refusal.accepted()) {
            return new Late(true, refusal.detail());
        }
        List<CmaEvent> then = listen(stream, options.quiet());
        boolean worked = then.stream().anyMatch(event -> event instanceof CmaEvent.AgentMessage
                || event instanceof CmaEvent.CustomToolUse || event instanceof CmaEvent.ToolUse
                || event instanceof CmaEvent.ToolResult);
        return new Late(!worked, "the service accepted it; the stream then showed " + describe(then));
    }

    /** Makes a call that is expected to be refused as an invalid request, and says what came of it. */
    private static Refusal refusal(Runnable call) {
        try {
            call.run();
            return new Refusal(true, "the service accepted it");
        } catch (CmaException e) {
            if (e.kind() == CmaException.Kind.INVALID_REQUEST || e.kind() == CmaException.Kind.NOT_FOUND
                    || e.kind() == CmaException.Kind.CONFLICT) {
                return new Refusal(false, "refused with HTTP " + e.status() + ": " + e.getMessage());
            }
            throw e;
        }
    }

    /** Reads until an event matches, and fails when none does in time. Events the agent service does not act on are left out. */
    private static List<CmaEvent> read(EventStream stream, Predicate<CmaEvent> last, Duration timeout) {
        List<CmaEvent> events = collect(stream, last, timeout);
        if (events.isEmpty() || !last.test(events.getLast())) {
            throw new IllegalStateException("the expected event did not arrive within " + timeout + "; saw " + types(events));
        }
        return events;
    }

    /** Reads until an event matches or the time is up, whichever comes first. */
    private static List<CmaEvent> collect(EventStream stream, Predicate<CmaEvent> last, Duration timeout) {
        List<CmaEvent> events = new ArrayList<>();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<CmaEvent> next = stream.poll(Duration.ofMillis(100));
            if (next.isEmpty() || next.get() instanceof CmaEvent.Other) {
                continue;
            }
            events.add(next.get());
            if (last.test(next.get())) {
                break;
            }
        }
        return events;
    }

    private static Duration shorter(Duration one, Duration other) {
        return one.compareTo(other) <= 0 ? one : other;
    }

    /** Collects whatever arrives during {@code time}. */
    private static List<CmaEvent> listen(EventStream stream, Duration time) {
        List<CmaEvent> events = new ArrayList<>();
        long deadline = System.nanoTime() + time.toNanos();
        while (System.nanoTime() < deadline) {
            stream.poll(Duration.ofMillis(100)).filter(event -> !(event instanceof CmaEvent.Other)).ifPresent(events::add);
        }
        return events;
    }

    private void trace(List<CmaEvent> events) {
        out.println("    " + describe(events));
    }

    private static boolean idle(CmaEvent event) {
        return event instanceof CmaEvent.Idle;
    }

    private static boolean turnOver(CmaEvent event) {
        return event instanceof CmaEvent.Idle idle && idle.reason() != StopReason.REQUIRES_ACTION;
    }

    private static <T extends CmaEvent> Optional<T> last(List<CmaEvent> events, Class<T> type) {
        return events.reversed().stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    private static List<String> types(List<CmaEvent> events) {
        return events.stream().map(CmaEvent::type).toList();
    }

    private static String texts(List<CmaEvent> events) {
        StringBuilder text = new StringBuilder();
        for (CmaEvent event : events) {
            if (event instanceof CmaEvent.AgentMessage message) {
                text.append(text.isEmpty() ? "" : " | ").append(message.text());
            }
        }
        return text.toString();
    }

    /** Events in a compact form: type, whether an id is there, whether it is processed, and what stands out. */
    private static String describe(List<CmaEvent> events) {
        List<String> parts = new ArrayList<>();
        for (CmaEvent event : events) {
            StringBuilder part = new StringBuilder(event.type());
            if (event.id() == null || event.id().isEmpty()) {
                part.append("(no id)");
            }
            if (event.processedAt() == null) {
                part.append("(queued)");
            }
            switch (event) {
                case CmaEvent.Idle idle -> part.append("[").append(idle.reason())
                        .append(idle.awaiting().isEmpty() ? "" : " " + idle.awaiting().size() + " waiting").append("]");
                case CmaEvent.ToolUse call -> part.append("[").append(call.name()).append(" ").append(call.permission()).append("]");
                case CmaEvent.CustomToolUse call -> part.append("[").append(call.name()).append("]");
                case CmaEvent.ToolResult result -> part.append(result.error() ? "[error]" : "[ok]");
                case CmaEvent.Error error -> part.append("[").append(error.errorType()).append(" ").append(error.retry()).append("]");
                default -> {
                }
            }
            parts.add(part.toString());
        }
        return parts.toString();
    }
}
