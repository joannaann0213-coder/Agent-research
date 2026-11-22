package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.CmaEvent.Permission;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.EventTail;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.SessionSpec;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.bridge.Turns.StaleTurnException;
import dev.agentflow.agent.server.bridge.Turns.Turn;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.profile.AgentProfiles;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns one run of the Event Inbox into one turn of a session on the agent platform, and the
 * session's answer into the agent's reply.
 *
 * <p>The run is the unit of work and of retry. Whatever a turn has done is on record (in its
 * {@code turn} row and in the session itself), so a run that is delivered again continues where it
 * stopped: it does not send its input twice and does not store its reply twice.
 *
 * <ol>
 *   <li>Take the turn, under the epoch of the lease. A worker of an earlier lease can no longer write.
 *   <li>Make sure the line has a session.
 *   <li>Fix the boundary: the last event the session held before this turn. Anything a later
 *       attempt finds after it is this turn's own.
 *   <li>Open the stream, then read what the session holds after the boundary. If the input is not
 *       there, send it.
 *   <li>Read until the session is idle after having processed the input. Tool calls that wait for an
 *       answer get one on the way.
 *   <li>Store the reply and end the turn in one transaction, then tell the Inbox.
 * </ol>
 */
@Component
public class TurnRunner {

    private static final Logger log = LoggerFactory.getLogger(TurnRunner.class);

    private sealed interface Settlement {
        record Done() implements Settlement {
        }

        record Failed(String error) implements Settlement {
        }

        record Retry(String reason, Duration delay) implements Settlement {
        }
    }

    private final ManagedAgents cma;
    private final InboxClient inbox;
    private final Turns turns;
    private final Threads threads;
    private final Messages messages;
    private final AgentProfiles profiles;
    private final TransactionTemplate tx;
    private final AgentProperties.Turn config;

    public TurnRunner(ManagedAgents cma, InboxClient inbox, Turns turns, Threads threads, Messages messages,
            AgentProfiles profiles, TransactionTemplate tx, AgentProperties properties) {
        this.cma = cma;
        this.inbox = inbox;
        this.turns = turns;
        this.threads = threads;
        this.messages = messages;
        this.profiles = profiles;
        this.tx = tx;
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
            log.warn("run {}: the agent platform failed ({} {}): {}", run.runId(), e.kind(), e.status(), e.getMessage());
            settlement = e.retryable()
                    ? new Settlement.Retry("agent platform: " + e.kind(), e.retryAfter())
                    : new Settlement.Retry("agent platform refused: " + e.getMessage(), null);
        } catch (AgentException e) {
            settlement = e.status() == HttpStatus.SERVICE_UNAVAILABLE
                    ? new Settlement.Retry(e.getMessage(), null)
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

        String appId = run.tenantId();
        String userId = run.inboxId();
        List<MessageView> inputs = inputs(run);
        Optional<Threads.Thread> line = run.mergeKey() == null ? Optional.empty()
                : threads.find(appId, userId, run.mergeKey());
        if (inputs.isEmpty() || line.isEmpty()) {
            return fail(run, "the run holds nothing this service can work on");
        }
        Threads.Thread thread = withSession(line.get());
        turn = turns.inSession(turn, thread.generation(), thread.sessionId());
        if (!turn.boundarySet()) {
            turn = fixBoundary(turn, thread);
        }

        try (EventTail tail = EventTail.open(cma, thread.sessionId(), since(turn.boundaryAt()))) {
            Reading reading = new Reading(turn.inputEventId());
            afterBoundary(tail.stored(), turn).forEach(reading::accept);
            if (!reading.inputSeen) {
                // A failure here leaves the question whether the input arrived. It is not answered by
                // sending again: the next attempt reads the session after the boundary and sees.
                List<CmaEvent> stored = cma.send(thread.sessionId(), List.of(new Outgoing.UserMessage(text(inputs))));
                String inputId = stored.isEmpty() ? null : stored.getFirst().id();
                turns.input(turn, inputId);
                reading.inputId = inputId;
            }
            while (reading.end == null) {
                if (lease.lost()) {
                    throw new StaleTurnException(run.runId(), run.epoch());
                }
                answerWaitingCalls(thread, reading);
                tail.poll(config.getPoll()).ifPresent(reading::accept);
            }
            return finish(run, turn, thread, reading);
        }
    }

    // ================================================================ the steps

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

    /** Several messages that arrived close together are one turn's input, in order. */
    private static String text(List<MessageView> inputs) {
        StringBuilder text = new StringBuilder();
        for (MessageView input : inputs) {
            text.append(text.isEmpty() ? "" : "\n\n").append(input.text());
        }
        return text.toString();
    }

    private Threads.Thread withSession(Threads.Thread thread) {
        if (thread.sessionId() != null) {
            return thread;
        }
        AgentProfiles.Ready profile = profiles.ready(thread.appId(), thread.agentKey());
        String sessionId = cma.createSession(new SessionSpec(profile.agent(), profile.environmentId(),
                thread.appId() + " / " + thread.userId() + " / " + thread.mergeKey(),
                Map.of("app", thread.appId(), "user", thread.userId(), "line", thread.mergeKey(),
                        "generation", Integer.toString(thread.generation() + 1)),
                List.of()));
        return threads.startSession(thread, sessionId, profile.agent());
    }

    private Turn fixBoundary(Turn turn, Threads.Thread thread) {
        List<CmaEvent> recent = cma.listEvents(thread.sessionId(), since(thread.lastEventAt()));
        CmaEvent last = null;
        for (CmaEvent event : recent) {
            if (event.processedAt() != null && !event.id().isEmpty()) {
                last = event;
            }
        }
        return turns.boundary(turn, last == null ? null : last.id(), last == null ? null : last.processedAt());
    }

    /** Where to start reading so that the event at {@code at} is certainly included. */
    private static Instant since(Instant at) {
        return at == null ? null : at.minusSeconds(1);
    }

    private static List<CmaEvent> afterBoundary(List<CmaEvent> stored, Turn turn) {
        if (turn.boundaryEventId() == null) {
            return stored;
        }
        for (int i = 0; i < stored.size(); i++) {
            if (turn.boundaryEventId().equals(stored.get(i).id())) {
                return stored.subList(i + 1, stored.size());
            }
        }
        return stored.stream()
                .filter(event -> event.processedAt() == null || event.processedAt().isAfter(turn.boundaryAt()))
                .toList();
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
            }
        }
        reading.waiting = List.of();
        if (!answers.isEmpty()) {
            cma.send(thread.sessionId(), answers);
        }
    }

    private Settlement finish(ClaimedRun run, Turn turn, Threads.Thread thread, Reading reading) {
        if (reading.end != End.TURN_OVER) {
            return fail(run, "the session ended the turn with " + reading.end.name().toLowerCase());
        }
        String reply = reading.lastText;
        boolean replied = reply != null && !reply.isBlank();
        tx.executeWithoutResult(status -> {
            if (replied) {
                messages.storeFromAgent(thread.appId(), thread.userId(), thread.conversationId(), "reply:" + run.runId(),
                        "reply", reply, run.runId());
            }
            if (reading.lastEventId != null) {
                threads.sawUpTo(thread, reading.lastEventId, reading.lastEventAt);
            }
            // Last, and conditional on the lease: if this worker is no longer the run's, the reply above is rolled back.
            turns.finish(turn, Turns.DONE, replied ? "replied" : "silent", null);
        });
        return new Settlement.Done();
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

    private static String shorten(String text) {
        return text == null || text.length() <= 300 ? text : text.substring(0, 300);
    }

    // ================================================================ reading a turn

    private enum End {
        TURN_OVER,
        RETRIES_EXHAUSTED,
        BUDGET_REACHED,
        REFUSAL,
        TERMINATED,
        UNKNOWN
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
            if (event.processedAt() != null && !event.id().isEmpty()) {
                lastEventId = event.id();
                lastEventAt = event.processedAt();
            }
            switch (event) {
                case CmaEvent.UserMessage message -> {
                    // Only this turn writes to the session, so a message after the boundary is its input.
                    if (inputId == null) {
                        inputId = message.id();
                    }
                    if (message.id().equals(inputId)) {
                        inputSeen = true;
                        inputProcessed |= message.processedAt() != null;
                    }
                }
                case CmaEvent.AgentMessage message -> {
                    if (inputProcessed) {
                        lastText = message.text();
                    }
                }
                case CmaEvent.CustomToolUse call -> calls.put(call.id(), call);
                case CmaEvent.ToolUse call -> {
                    if (call.permission() == Permission.ASK) {
                        calls.put(call.id(), call);
                    }
                }
                case CmaEvent.Idle idle -> {
                    if (idle.reason() == StopReason.REQUIRES_ACTION) {
                        waiting = idle.awaiting();
                    } else if (inputProcessed) {
                        // An idle before the input was processed belongs to what came before this turn.
                        end = switch (idle.reason()) {
                            case END_TURN -> End.TURN_OVER;
                            case RETRIES_EXHAUSTED -> End.RETRIES_EXHAUSTED;
                            case BUDGET_REACHED -> End.BUDGET_REACHED;
                            case REFUSAL -> End.REFUSAL;
                            default -> End.UNKNOWN;
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
