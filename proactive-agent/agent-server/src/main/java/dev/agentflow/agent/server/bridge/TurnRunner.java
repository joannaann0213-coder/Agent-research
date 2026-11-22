package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.EventTail;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.SessionInfo;
import dev.agentflow.agent.cma.SessionSpec;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.bridge.Turns.StaleTurnException;
import dev.agentflow.agent.server.bridge.Turns.Turn;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns one run of the Event Inbox into one turn of a session on the agent platform, and the
 * session's answer into the agent's reply.
 *
 * <p>The run is the unit of work and of retry. Whatever a turn has done is on record, in its
 * {@code turn} row and in the session itself, so a run that is delivered again continues where it
 * stopped: it does not send its input twice and does not store its reply twice.
 *
 * <ol>
 *   <li>Take the turn, under the epoch of the lease. A worker of an earlier lease can no longer write.
 *   <li>Make sure the line has a session that can be used. One that is terminated, gone or stuck is
 *       replaced by a new one, which is told what was said before.
 *   <li>Bring the session to rest if an earlier turn left it busy, then fix the boundary: the last
 *       event the session holds before this turn's input. Anything after it is this turn's own.
 *   <li>Open the stream, then read what the session holds after the boundary. If the input is not
 *       there, send it.
 *   <li>Read until the session is idle after having processed the input, or until the turn's deadline.
 *       Tool calls that wait for an answer get one on the way.
 *   <li>Store the reply and end the turn in one transaction, then tell the Inbox.
 * </ol>
 *
 * <p>A call to the platform that fails for a passing reason is tried again a few times here, while
 * the lease is kept alive; after that the run goes back to the Inbox, which delivers it again later.
 * The one call that is never simply repeated is the send of the input: when its answer is lost, the
 * session is read to see whether the input arrived.
 */
@Component
public class TurnRunner {

    private static final Logger log = LoggerFactory.getLogger(TurnRunner.class);

    private static final int TRANSCRIPT_CHARS_PER_MESSAGE = 2000;

    private sealed interface Settlement {
        record Done() implements Settlement {
        }

        record Failed(String error) implements Settlement {
        }

        /** @param delay how long the Inbox holds the run back; {@code null} for its own growing delay */
        record Retry(String reason, Duration delay) implements Settlement {
        }
    }

    /** The line's session cannot carry this turn. The line gets a new one. */
    private static final class SessionLost extends RuntimeException {
        private final transient Threads.Thread thread;

        SessionLost(Threads.Thread thread, String why) {
            super("session " + thread.sessionId() + " of line " + thread.mergeKey() + " cannot be used: " + why);
            this.thread = thread;
        }
    }

    private final ManagedAgents cma;
    private final InboxClient inbox;
    private final Turns turns;
    private final Threads threads;
    private final Messages messages;
    private final AgentProfiles profiles;
    private final TransactionTemplate tx;
    private final AgentClock clock;
    private final Patience patience;
    private final TurnHooks hooks;
    private final AgentProperties.Turn config;

    public TurnRunner(ManagedAgents cma, InboxClient inbox, Turns turns, Threads threads, Messages messages,
            AgentProfiles profiles, TransactionTemplate tx, AgentClock clock, Patience patience, TurnHooks hooks,
            AgentProperties properties) {
        this.cma = cma;
        this.inbox = inbox;
        this.turns = turns;
        this.threads = threads;
        this.messages = messages;
        this.profiles = profiles;
        this.tx = tx;
        this.clock = clock;
        this.patience = patience;
        this.hooks = hooks;
        this.config = properties.getTurn();
    }

    /** Works on a claimed run to its end and settles it with the Inbox. Never throws. */
    public void run(ClaimedRun run) {
        Settlement settlement;
        try (RunLease lease = new RunLease(inbox, run, config.getHeartbeat())) {
            settlement = work(run, lease);
        } catch (StaleTurnException stale) {
            // Another worker holds the run now. Whatever is left to do is its to do.
            log.info("{}", stale.getMessage());
            return;
        } catch (CmaException e) {
            // The platform kept failing through the attempts made here. The Inbox delivers the run again later.
            log.warn("run {}: the agent platform failed ({} {}): {}", run.runId(), e.kind(), e.status(), e.getMessage());
            Duration delay = e.retryAfter() != null && e.retryAfter().compareTo(config.getNackDelay()) > 0
                    ? e.retryAfter() : config.getNackDelay();
            settlement = new Settlement.Retry("agent platform: " + e.kind() + " " + e.getMessage(), delay);
        } catch (AgentException e) {
            settlement = e.status() == HttpStatus.SERVICE_UNAVAILABLE
                    ? new Settlement.Retry(e.getMessage(), config.getNackDelay())
                    : fail(run, e.getMessage());
        } catch (RuntimeException e) {
            log.error("run {} failed unexpectedly", run.runId(), e);
            settlement = new Settlement.Retry(e.toString(), null);
        }
        settle(run, settlement);
    }

    private Settlement work(ClaimedRun run, RunLease lease) {
        Turn turn = turns.open(run).orElseThrow(() -> new StaleTurnException(run.runId(), run.epoch()));
        if (Turns.DONE.equals(turn.state())) {
            return new Settlement.Done();
        }
        if (Turns.FAILED.equals(turn.state())) {
            return new Settlement.Failed(turn.error());
        }

        List<MessageView> inputs = inputs(run);
        if (inputs.isEmpty() || run.mergeKey() == null
                || threads.find(run.tenantId(), run.inboxId(), run.mergeKey()).isEmpty()) {
            return fail(run, "the run holds nothing this service can work on");
        }
        for (int session = 1; ; session++) {
            Threads.Thread thread = threads.find(run.tenantId(), run.inboxId(), run.mergeKey()).orElseThrow();
            try {
                return inSession(run, lease, turn, thread, inputs);
            } catch (SessionLost lost) {
                log.warn("run {}: {}", run.runId(), lost.getMessage());
                threads.dropSession(lost.thread);
                if (session == 2) {
                    return new Settlement.Retry(lost.getMessage(), config.getNackDelay());
                }
            }
        }
    }

    private Settlement inSession(ClaimedRun run, RunLease lease, Turn opened, Threads.Thread line, List<MessageView> inputs) {
        Threads.Thread thread = withSession(run, line);
        Turn turn = turns.inSession(opened, thread.generation(), thread.sessionId());
        try {
            if (!turn.boundarySet()) {
                turn = fixBoundary(turn, thread);
                hooks.at(Point.BOUNDARY_FIXED, run);
            }
            Instant deadline = turn.createdAt().plus(config.getDeadline());
            Instant from = since(turn.boundaryAt());
            try (EventTail tail = platform(() -> EventTail.open(cma, thread.sessionId(), from))) {
                Reading reading = new Reading(turn.inputEventId());
                after(tail.stored(), turn.boundaryEventId(), turn.boundaryAt()).forEach(event -> see(thread, reading, event));
                if (!reading.inputSeen) {
                    String inputId = sendInput(turn, thread, input(thread, inputs));
                    hooks.at(Point.INPUT_SENT, run);
                    turns.input(turn, inputId);
                    reading.inputId = inputId;
                }
                hooks.at(Point.INPUT_RECORDED, run);
                while (reading.end == null) {
                    if (lease.lost()) {
                        throw new StaleTurnException(run.runId(), run.epoch());
                    }
                    if (!clock.now().isBefore(deadline)) {
                        return giveUp(run, turn, thread);
                    }
                    answerWaitingCalls(thread, reading);
                    patience.next(tail, config.getPoll()).ifPresent(event -> see(thread, reading, event));
                }
                return finish(run, turn, thread, reading);
            }
        } catch (CmaException e) {
            if (e.kind() == CmaException.Kind.NOT_FOUND) {
                throw new SessionLost(thread, "the platform does not know it");
            }
            throw e;
        }
    }

    // ================================================================ the session

    private Threads.Thread withSession(ClaimedRun run, Threads.Thread thread) {
        if (thread.sessionId() != null) {
            return thread;
        }
        AgentProfiles.Ready profile = profiles.ready(thread.appId(), thread.agentKey());
        String sessionId = platform(() -> cma.createSession(new SessionSpec(profile.agent(), profile.environmentId(),
                thread.appId() + " / " + thread.userId() + " / " + thread.mergeKey(),
                Map.of("app", thread.appId(), "user", thread.userId(), "line", thread.mergeKey(),
                        "generation", Integer.toString(thread.generation() + 1)),
                List.of())));
        hooks.at(Point.SESSION_CREATED, run);
        return threads.startSession(thread, sessionId, profile.agent());
    }

    /**
     * Fixes where the session stands before this turn's input. A session that an earlier turn left
     * working, or waiting for an answer nobody will give, is stopped first: otherwise this turn's
     * input would queue behind that work, or be refused, and the reply read here would be the answer
     * to something else.
     */
    private Turn fixBoundary(Turn turn, Threads.Thread thread) {
        String session = thread.sessionId();
        SessionInfo info = platform(() -> cma.getSession(session));
        if (!info.usable()) {
            throw new SessionLost(thread, info.archived() ? "it is archived" : "it is terminated");
        }
        List<CmaEvent> recent = platform(() -> cma.listEvents(session, since(thread.lastEventAt())));
        List<CmaEvent> news = after(recent, thread.lastEventId(), thread.lastEventAt());
        CmaEvent.Idle lastIdle = null;
        for (CmaEvent event : news) {
            if (event instanceof CmaEvent.Terminated) {
                throw new SessionLost(thread, "it is terminated");
            } else if (event instanceof CmaEvent.Idle idle) {
                lastIdle = idle;
            }
        }
        boolean waiting = lastIdle != null && lastIdle.reason() == StopReason.REQUIRES_ACTION;
        if (info.status() != SessionInfo.Status.IDLE || waiting) {
            log.warn("session {} of line {} was left busy by an earlier turn; stopping it", session, thread.mergeKey());
            bringToRest(thread);
            recent = platform(() -> cma.listEvents(session, since(thread.lastEventAt())));
        } else if (lastIdle != null && (lastIdle.reason() == StopReason.RETRIES_EXHAUSTED
                || lastIdle.reason() == StopReason.BUDGET_REACHED)) {
            throw new SessionLost(thread, "its last turn ended with " + lastIdle.reason().name().toLowerCase());
        }
        CmaEvent last = null;
        for (CmaEvent event : recent) {
            if (stored(event)) {
                last = event;
            }
        }
        return turns.boundary(turn, last == null ? null : last.id(), last == null ? null : last.processedAt());
    }

    /**
     * Interrupts the session and waits until it is idle with nothing waiting. A session that does not
     * get there in time is given up; the line then continues in a new one.
     */
    private void bringToRest(Threads.Thread thread) {
        String session = thread.sessionId();
        Instant giveUpAt = clock.now().plus(config.getSettleWait());
        try (EventTail tail = platform(() -> EventTail.open(cma, session, since(thread.lastEventAt())))) {
            tail.stored();
            platform(() -> cma.send(session, List.of(new Outgoing.Interrupt())));
            while (clock.now().isBefore(giveUpAt)) {
                Optional<CmaEvent> next = patience.next(tail, config.getPoll());
                if (next.isPresent() && next.get() instanceof CmaEvent.Idle idle && idle.reason() != StopReason.REQUIRES_ACTION) {
                    return;
                }
                if (next.isPresent() && next.get() instanceof CmaEvent.Terminated) {
                    throw new SessionLost(thread, "it is terminated");
                }
                if (next.isEmpty() && atRest(thread)) {
                    // it had come to rest by itself before the interrupt, which then had nothing to stop
                    return;
                }
            }
        }
        throw new SessionLost(thread, "it did not come to rest within " + config.getSettleWait() + " of an interrupt");
    }

    private boolean atRest(Threads.Thread thread) {
        if (platform(() -> cma.getSession(thread.sessionId())).status() != SessionInfo.Status.IDLE) {
            return false;
        }
        CmaEvent.Idle lastIdle = null;
        for (CmaEvent event : platform(() -> cma.listEvents(thread.sessionId(), since(thread.lastEventAt())))) {
            if (event instanceof CmaEvent.Idle idle) {
                lastIdle = idle;
            }
        }
        return lastIdle == null || lastIdle.reason() != StopReason.REQUIRES_ACTION;
    }

    // ================================================================ the input

    /** The user's messages that the run's events announce, in the order they were received. */
    private List<MessageView> inputs(ClaimedRun run) {
        List<MessageView> inputs = new ArrayList<>();
        for (ClaimedEvent event : run.events()) {
            if (!Messages.EVENT_TYPE.equals(event.type())) {
                continue;
            }
            String conversationId = event.payload().path("conversation_id").asString(null);
            String messageId = event.payload().path("message_id").asString(null);
            if (conversationId != null && messageId != null) {
                messages.find(run.tenantId(), run.inboxId(), conversationId, messageId).ifPresent(inputs::add);
            }
        }
        inputs.sort(Comparator.comparingLong(MessageView::seq));
        return inputs;
    }

    /**
     * The text of the turn's input: the messages, in order. In a session that replaces another one,
     * the first input begins with what was said before, because the new session knows none of it.
     */
    private String input(Threads.Thread thread, List<MessageView> inputs) {
        StringBuilder text = new StringBuilder();
        if (thread.generation() > 1 && thread.lastEventId() == null) {
            List<MessageView> earlier = messages.before(thread.appId(), thread.userId(), thread.conversationId(),
                    inputs.getFirst().seq(), config.getTranscriptMessages());
            if (!earlier.isEmpty()) {
                text.append("[From the service that runs you, not from the user: this conversation continues in a new "
                        + "session. This is what was said before, for your reference.]\n");
                for (MessageView message : earlier) {
                    String said = message.text().length() <= TRANSCRIPT_CHARS_PER_MESSAGE ? message.text()
                            : message.text().substring(0, TRANSCRIPT_CHARS_PER_MESSAGE) + "…";
                    text.append(message.role().equals("user") ? "User: " : "You: ").append(said).append('\n');
                }
                text.append("[End of the earlier conversation. The user's new message follows.]\n\n");
            }
        }
        for (int i = 0; i < inputs.size(); i++) {
            text.append(i == 0 ? "" : "\n\n").append(inputs.get(i).text());
        }
        return text.toString();
    }

    /**
     * Sends the input and returns its id on the platform. When the answer to a send is lost, the
     * input may have arrived all the same; the session is then read to see, before anything is sent
     * again.
     */
    private String sendInput(Turn turn, Threads.Thread thread, String text) {
        for (int attempt = 1; ; attempt++) {
            try {
                List<CmaEvent> stored = cma.send(thread.sessionId(), List.of(new Outgoing.UserMessage(text)));
                return stored.isEmpty() ? null : stored.getFirst().id();
            } catch (CmaException e) {
                if (e.kind() == CmaException.Kind.INVALID_REQUEST) {
                    throw new SessionLost(thread, "it did not take the input: " + e.getMessage());
                }
                if (!e.retryable() || attempt >= config.getAttempts()) {
                    throw e;
                }
                patience.pause(delay(attempt, e));
                if (e.maybeDelivered()) {
                    List<CmaEvent> held = platform(() -> cma.listEvents(thread.sessionId(), since(turn.boundaryAt())));
                    for (CmaEvent event : after(held, turn.boundaryEventId(), turn.boundaryAt())) {
                        if (event instanceof CmaEvent.UserMessage arrived) {
                            return arrived.id();
                        }
                    }
                }
            }
        }
    }

    // ================================================================ the turn

    private void see(Threads.Thread thread, Reading reading, CmaEvent event) {
        if (event instanceof CmaEvent.Error error) {
            // The platform says what it does about an error by itself: it tries again, or the session ends.
            log.warn("session {} of line {} reported {}: {}", thread.sessionId(), thread.mergeKey(), error.errorType(),
                    error.message());
        }
        reading.accept(event);
    }

    /**
     * Answers the tool calls the session waits for. For now there is nothing behind them: a custom
     * tool gets an error, and a call that asks for permission is denied, so that the agent can go on
     * and say so.
     */
    private void answerWaitingCalls(Threads.Thread thread, Reading reading) {
        List<Outgoing> answers = new ArrayList<>();
        for (String id : reading.waiting) {
            if (!reading.answered.add(id)) {
                continue;
            }
            CmaEvent call = reading.calls.get(id);
            if (call instanceof CmaEvent.CustomToolUse) {
                answers.add(new Outgoing.CustomToolResult(id, "This tool is not available.", true));
            } else if (call instanceof CmaEvent.ToolUse) {
                answers.add(new Outgoing.ToolConfirmation(id, false, "This action needs an approval, and nobody can give one here."));
            } else {
                // Without the call there is no telling what kind of answer it wants. The turn's deadline ends the wait.
                log.warn("session {} waits for an answer to {}, a call this turn has not seen", thread.sessionId(), id);
            }
        }
        reading.waiting = List.of();
        if (answers.isEmpty()) {
            return;
        }
        for (int attempt = 1; ; attempt++) {
            try {
                cma.send(thread.sessionId(), answers);
                return;
            } catch (CmaException e) {
                if (e.kind() == CmaException.Kind.INVALID_REQUEST && attempt > 1) {
                    // An earlier attempt got through after all: the calls are answered and no longer wait.
                    return;
                }
                if (!e.retryable() || attempt >= config.getAttempts()) {
                    throw e;
                }
                patience.pause(delay(attempt, e));
            }
        }
    }

    private Settlement finish(ClaimedRun run, Turn turn, Threads.Thread thread, Reading reading) {
        switch (reading.end) {
            case TERMINATED, RETRIES_EXHAUSTED, BUDGET_REACHED ->
                    throw new SessionLost(thread, "the turn ended with " + reading.end.name().toLowerCase());
            default -> {
            }
        }
        String reply = reading.lastText;
        boolean replied = reply != null && !reply.isBlank();
        String outcome = reading.end == End.REFUSAL ? "refused" : replied ? "replied" : "silent";
        tx.executeWithoutResult(status -> {
            if (replied) {
                messages.storeFromAgent(thread.appId(), thread.userId(), thread.conversationId(), "reply:" + run.runId(),
                        "reply", reply, run.runId());
            }
            if (reading.lastEventId != null) {
                threads.sawUpTo(thread, reading.lastEventId, reading.lastEventAt);
            }
            // Last, and conditional on the lease: if this worker is no longer the run's, the reply above is rolled back.
            turns.finish(turn, Turns.DONE, outcome, null);
        });
        hooks.at(Point.RESULT_STORED, run);
        return new Settlement.Done();
    }

    /**
     * Ends a turn that has run out of time. The session is told to stop; whether it does shows at the
     * line's next turn, which brings it to rest first in any case.
     */
    private Settlement giveUp(ClaimedRun run, Turn turn, Threads.Thread thread) {
        try {
            cma.send(thread.sessionId(), List.of(new Outgoing.Interrupt()));
        } catch (CmaException e) {
            log.warn("run {}: the session could not be interrupted at the deadline: {}", run.runId(), e.getMessage());
        }
        String error = "the turn took longer than " + config.getDeadline();
        turns.finish(turn, Turns.FAILED, null, error);
        log.warn("run {} failed: {}", run.runId(), error);
        return new Settlement.Failed(error);
    }

    /** Ends the turn as failed for good. The Inbox does not deliver the run again. */
    private Settlement fail(ClaimedRun run, String error) {
        turns.find(run.runId()).filter(turn -> turn.epoch() == run.epoch() && Turns.OPEN.equals(turn.state()))
                .ifPresent(turn -> turns.finish(turn, Turns.FAILED, null, error));
        log.warn("run {} failed: {}", run.runId(), error);
        return new Settlement.Failed(error);
    }

    private void settle(ClaimedRun run, Settlement settlement) {
        try {
            ApiResult<?> result = switch (settlement) {
                case Settlement.Done done -> inbox.ack(run.runId(), AckRequest.done(run.epoch(), run.lastRunSeq()));
                case Settlement.Failed failed ->
                        inbox.ack(run.runId(), AckRequest.failed(run.epoch(), run.lastRunSeq(), shorten(failed.error())));
                case Settlement.Retry retry -> inbox.nack(run.runId(), new NackRequest(run.epoch(), shorten(retry.reason()),
                        retry.delay() == null ? null : retry.delay().toMillis()));
            };
            if (!result.ok()) {
                log.warn("run {}: the Inbox answered {} {} to {}", run.runId(), result.status(), result.code(),
                        settlement.getClass().getSimpleName());
            }
        } catch (RuntimeException e) {
            // The turn's result is stored. The Inbox will deliver the run again, and it will be settled then.
            log.warn("run {}: the Inbox could not be told the outcome: {}", run.runId(), e.toString());
        }
    }

    // ================================================================ helpers

    /** A call to the platform that is safe to repeat, tried again a few times when it fails for a passing reason. */
    private <T> T platform(Supplier<T> call) {
        for (int attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (CmaException e) {
                if (!e.retryable() || attempt >= config.getAttempts()) {
                    throw e;
                }
                patience.pause(delay(attempt, e));
            }
        }
    }

    /** What the platform asked for, or else the configured delay doubled per attempt made; never more than the maximum. */
    private Duration delay(int attemptsMade, CmaException error) {
        Duration delay = error.retryAfter() != null ? error.retryAfter()
                : config.getRetryDelay().multipliedBy(1L << Math.min(attemptsMade - 1, 16));
        return delay.compareTo(config.getMaxRetryDelay()) > 0 ? config.getMaxRetryDelay() : delay;
    }

    /** Where to start reading so that the event at {@code at} is certainly included. */
    private static Instant since(Instant at) {
        return at == null ? null : at.minusSeconds(1);
    }

    /** The events that come after the event {@code id}, which happened at {@code at}; all of them when there is no such event. */
    private static List<CmaEvent> after(List<CmaEvent> events, String id, Instant at) {
        if (id == null) {
            return events;
        }
        for (int i = 0; i < events.size(); i++) {
            if (id.equals(events.get(i).id())) {
                return events.subList(i + 1, events.size());
            }
        }
        return events.stream().filter(event -> event.processedAt() == null || event.processedAt().isAfter(at)).toList();
    }

    /** Whether the event is one the session has taken up and that can be pointed at: the queued form of a message and an event without an id are not. */
    private static boolean stored(CmaEvent event) {
        return event.processedAt() != null && event.id() != null && !event.id().isEmpty();
    }

    private static String shorten(String text) {
        return text == null || text.length() <= 300 ? text : text.substring(0, 300);
    }

    // ================================================================ reading a turn

    private enum End {
        TURN_OVER,
        REFUSAL,
        RETRIES_EXHAUSTED,
        BUDGET_REACHED,
        TERMINATED
    }

    /** What a turn has seen of its session after the boundary. */
    private static final class Reading {

        /** The id of the turn's input; learned from the session when the answer to the send was lost. */
        private String inputId;
        private boolean inputSeen;
        private boolean inputProcessed;
        private String lastText;
        private final Map<String, CmaEvent> calls = new LinkedHashMap<>();
        private final Set<String> answered = new HashSet<>();
        private List<String> waiting = List.of();
        private End end;
        private String lastEventId;
        private Instant lastEventAt;

        Reading(String inputId) {
            this.inputId = inputId;
        }

        void accept(CmaEvent event) {
            if (stored(event)) {
                lastEventId = event.id();
                lastEventAt = event.processedAt();
            }
            switch (event) {
                case CmaEvent.UserMessage message -> {
                    // Only this turn writes to the session, so a message after the boundary is its input.
                    if (inputId == null) {
                        inputId = message.id();
                    }
                    if (inputId != null && inputId.equals(message.id())) {
                        inputSeen = true;
                        inputProcessed |= message.processedAt() != null;
                    }
                }
                // An answer that is on record was given, by this worker or by one before it. It is not given again.
                case CmaEvent.UserToolConfirmation answer -> answered.add(answer.toolUseId());
                case CmaEvent.UserCustomToolResult answer -> answered.add(answer.customToolUseId());
                case CmaEvent.Running running -> waiting = List.of();
                case CmaEvent.AgentMessage message -> {
                    if (inputProcessed) {
                        lastText = message.text();
                    }
                }
                case CmaEvent.CustomToolUse call -> calls.put(call.id(), call);
                case CmaEvent.ToolUse call -> calls.put(call.id(), call);
                case CmaEvent.Idle idle -> {
                    if (idle.reason() == StopReason.REQUIRES_ACTION) {
                        waiting = idle.awaiting();
                    } else if (inputProcessed) {
                        // An idle before the input was processed belongs to what came before this turn.
                        end = switch (idle.reason()) {
                            case RETRIES_EXHAUSTED -> End.RETRIES_EXHAUSTED;
                            case BUDGET_REACHED -> End.BUDGET_REACHED;
                            case REFUSAL -> End.REFUSAL;
                            default -> End.TURN_OVER;
                        };
                    }
                }
                case CmaEvent.Terminated terminated -> end = End.TERMINATED;
                default -> {
                }
            }
        }
    }
}
