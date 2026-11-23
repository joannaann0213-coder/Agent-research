package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.EventTail;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.SessionInfo;
import dev.agentflow.agent.cma.SessionSpec;
import dev.agentflow.agent.server.actions.Actions;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.bridge.Turns.StaleTurnException;
import dev.agentflow.agent.server.bridge.Turns.Turn;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.delivery.Outbound;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.server.events.AppEvents;
import dev.agentflow.agent.server.events.AppEvents.AppEvent;
import dev.agentflow.agent.server.credentials.Credentials;
import dev.agentflow.agent.server.memory.Memories;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.schedules.Schedules;
import dev.agentflow.agent.server.tasks.Tasks;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.agent.server.tools.PlatformTools;
import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.RunStatus;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.api.SuspendRequest;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Turns one run of the Event Inbox into one turn of a session on the agent platform, and what the
 * agent does in it into what the user gets: its answer, when the user said something; a
 * notification or nothing at all, when the turn is about something that happened in the
 * application's systems. A worker's turn on a task that a main agent delegated gives the user
 * nothing: what the worker writes goes to the main agent, as input of its line.
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
 *       Tool calls that wait for an answer get one on the way; the tools of this service are run here.
 *   <li>Store the result and end the turn in one transaction, then tell the Inbox.
 * </ol>
 *
 * <p>A call to the platform that fails for a passing reason is tried again a few times here, while
 * the lease is kept alive; after that the run goes back to the Inbox, which delivers it again later.
 * The one call that is never simply repeated is the send of the input: when its answer is lost, the
 * session is read to see whether the input arrived.
 *
 * <p>A tool call that needs a person, an approval or a question to the user, is put on record as an
 * action. When a turn has nothing left to do but wait for people, its run is suspended in the Inbox,
 * and is woken when somebody decides, when the first action expires, or when new input arrives on
 * the line. A turn that has taken its session keeps it until it ends: a later run of the line waits
 * for it, and withdraws what it waits for, so that it finishes.
 */
@Component
public class TurnRunner {

    private static final Logger log = LoggerFactory.getLogger(TurnRunner.class);

    private static final int TRANSCRIPT_CHARS_PER_MESSAGE = 2000;
    private static final int NOTE_CHARS = 2000;

    private sealed interface Settlement {
        record Done() implements Settlement {
        }

        record Failed(String error) implements Settlement {
        }

        /** @param delay how long the Inbox holds the run back; {@code null} for its own growing delay */
        record Retry(String reason, Duration delay) implements Settlement {
        }

        /** The run is suspended in the Inbox, waiting for people. Nothing more to tell it. */
        record Parked() implements Settlement {
        }
    }

    /** What putting a call before a person came to: its action, or, on a closing turn, why it was not put. */
    private record Put(Actions.Action action, String closing) {
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
    private final AppEvents appEvents;
    private final Schedules schedules;
    private final Actions actions;
    private final Users users;
    private final Outbound outbound;
    private final Credentials credentials;
    private final Memories memories;
    private final Tasks tasks;
    private final TurnTools tools;
    private final AgentProfiles profiles;
    private final TransactionTemplate tx;
    private final AgentClock clock;
    private final Patience patience;
    private final TurnHooks hooks;
    private final AgentProperties.Turn config;
    private final AgentProperties.Notify notify;
    private final Duration workerDeadline;
    private final Duration actionTtl;

    TurnRunner(ManagedAgents cma, InboxClient inbox, Turns turns, Threads threads, Messages messages, AppEvents appEvents,
            Schedules schedules, Actions actions, Users users, Outbound outbound, Credentials credentials, Memories memories,
            Tasks tasks, TurnTools tools, AgentProfiles profiles, TransactionTemplate tx, AgentClock clock, Patience patience,
            TurnHooks hooks, AgentProperties properties) {
        this.cma = cma;
        this.inbox = inbox;
        this.turns = turns;
        this.threads = threads;
        this.messages = messages;
        this.appEvents = appEvents;
        this.schedules = schedules;
        this.actions = actions;
        this.users = users;
        this.outbound = outbound;
        this.credentials = credentials;
        this.memories = memories;
        this.tasks = tasks;
        this.tools = tools;
        this.profiles = profiles;
        this.tx = tx;
        this.clock = clock;
        this.patience = patience;
        this.hooks = hooks;
        this.config = properties.getTurn();
        this.notify = properties.getNotify();
        this.workerDeadline = properties.getTasks().getTurnDeadline();
        this.actionTtl = properties.getActions().getTtl();
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
        boolean resumed = ClaimedRun.KIND_RESUME.equals(run.kind());
        Work fresh = resumed ? null : workOf(run);
        if (!resumed && fresh == null && !run.events().isEmpty() && run.events().stream()
                .allMatch(event -> Actions.DOORBELL.equals(event.type()) || Schedules.TIMER_EVENT.equals(event.type()))) {
            // Rings that came after the turn they were to wake had ended, or a schedule that was cancelled
            // after it fell due. There is nothing to do.
            return new Settlement.Done();
        }
        Turn turn = turns.open(run, deadlineOf(run)).orElseThrow(() -> new StaleTurnException(run.runId(), run.epoch()));
        if (Turns.DONE.equals(turn.state())) {
            return new Settlement.Done();
        }
        if (Turns.FAILED.equals(turn.state())) {
            return new Settlement.Failed(turn.error());
        }

        // A resumed run is handed only what woke it. What the turn works on is on record since its first delivery.
        Work work = turn.work() != null ? load(run, turn.work()) : fresh;
        if (work == null || run.mergeKey() == null
                || threads.find(run.tenantId(), run.inboxId(), run.mergeKey()).isEmpty()) {
            return fail(run, "the run holds nothing this service can work on");
        }
        if (work instanceof Work.Chat chat && chat.messages().isEmpty()) {
            // The user stopped the reply before the turn began.
            turns.finish(turn, Turns.DONE, "stopped", null);
            return new Settlement.Done();
        }
        if (work instanceof Work.Job job && !turn.boundarySet()
                && tasks.closed(run.tenantId(), run.inboxId(), job.task().taskId())) {
            // The task was closed before the worker got to this. Its session is not touched, or not even made.
            turns.finish(turn, Turns.DONE, "closed", null);
            return new Settlement.Done();
        }
        if (turn.work() == null) {
            turn = turns.work(turn, stored(work));
        }
        Optional<Settlement> earlierFirst = waitForTheLine(run, turn);
        if (earlierFirst.isPresent()) {
            return earlierFirst.get();
        }
        if (!turn.boundarySet()) {
            renewIfOutdated(run);
        }
        for (int session = 1; ; session++) {
            Threads.Thread thread = threads.find(run.tenantId(), run.inboxId(), run.mergeKey()).orElseThrow();
            try {
                return inSession(run, lease, turn, thread, work);
            } catch (SessionLost lost) {
                log.warn("run {}: {}", run.runId(), lost.getMessage());
                threads.dropSession(lost.thread);
                // What the turn waited for people to decide was about calls of that session. The turn starts over.
                actions.withdrawOfRun(run.runId(), "the agent's session was replaced and the turn started over");
                if (session == 2) {
                    return new Settlement.Retry(lost.getMessage(), config.getNackDelay());
                }
            }
        }
    }

    /**
     * Whether the run has to let an earlier turn of its line finish first. A turn that has taken the
     * session keeps it until it ends; a later one does not touch it before. When the earlier turn
     * waits for people, this run's input means that it waits no longer: what it waits for is
     * withdrawn, and it is woken to finish.
     */
    private Optional<Settlement> waitForTheLine(ClaimedRun run, Turn turn) {
        if (turn.boundarySet()) {
            return Optional.empty();
        }
        for (Turn earlier : turns.openOn(run.tenantId(), run.inboxId(), run.mergeKey())) {
            if (earlier.runId().equals(run.runId()) || !earlier.boundarySet()) {
                continue;
            }
            ApiResult<RunView> found = inbox.getRun(earlier.runId());
            if (!found.ok() && found.status() != 404) {
                return Optional.of(new Settlement.Retry("the Inbox could not say how run " + earlier.runId() + " stands",
                        config.getLineBusyDelay()));
            }
            RunStatus status = found.ok() ? RunStatus.fromWire(found.body().status()) : null;
            if (status == null || !status.isOpen()) {
                // Its run ended without it, so nobody will finish that turn. Its session is brought to rest below.
                String why = status == null ? "its run is gone" : "its run ended as " + found.body().status();
                tx.executeWithoutResult(transaction -> {
                    users.lock(run.tenantId(), run.inboxId());
                    actions.withdrawOfRun(earlier.runId(), "the turn that waited for this ended");
                    if (turns.abandon(earlier.runId(), "a later turn of the line found that " + why)) {
                        outbound.record(run.tenantId(), run.inboxId(), Update.RUN_FAILED,
                                Outbound.failure(earlier.runId(), run.mergeKey(), "the run ended without finishing its turn: " + why));
                        tasks.turnFailed(run.tenantId(), run.inboxId(), run.mergeKey(), earlier.runId(),
                                "the run ended without finishing its turn: " + why);
                    }
                });
                continue;
            }
            if (status == RunStatus.SUSPENDED) {
                // On a conversation's line it is not always the user who is next: for a main agent it is often
                // an event or a worker's report, and the agent is told which.
                String note = run.mergeKey().startsWith(Threads.CHAT_PREFIX) && run.lane() != Lane.INTERACTIVE
                        ? "something new came in for you before this was decided; it comes in the next turn"
                        : Actions.supersededNote(run.mergeKey());
                tx.executeWithoutResult(transaction -> {
                    users.lock(run.tenantId(), run.inboxId());
                    actions.withdrawOfRun(earlier.runId(), note);
                    actions.close(earlier.runId(), note);
                    actions.ring(run.tenantId(), run.inboxId(), run.mergeKey(), earlier.runId());
                });
            }
            Duration delay = config.getLineBusyDelay();
            if (status == RunStatus.QUEUED && found.body().nextAttemptAt() != null) {
                Duration untilItsTurn = Duration.between(clock.now(), found.body().nextAttemptAt()).plusSeconds(1);
                delay = untilItsTurn.compareTo(delay) > 0 ? untilItsTurn : delay;
            }
            log.info("run {} waits for run {} of its line to finish", run.runId(), earlier.runId());
            return Optional.of(new Settlement.Retry("run " + earlier.runId() + " of the line finishes first", delay));
        }
        return Optional.empty();
    }

    private Settlement inSession(ClaimedRun run, RunLease lease, Turn opened, Threads.Thread line, Work work) {
        Threads.Thread thread = withSession(run, line);
        Turn turn = turns.inSession(opened, thread.generation(), thread.sessionId());
        try {
            if (!turn.boundarySet()) {
                turn = fixBoundary(turn, thread);
                hooks.at(Point.BOUNDARY_FIXED, run);
            }
            Instant deadline = turn.deadlineAt();
            Instant from = since(turn.boundaryAt());
            try (EventTail tail = platform(() -> EventTail.open(cma, thread.sessionId(), from))) {
                Reading reading = new Reading(turn.inputEventId());
                after(tail.stored(), turn.boundaryEventId(), turn.boundaryAt()).forEach(event -> see(thread, reading, event));
                if (!reading.inputSeen) {
                    if (stopped(thread, work)) {
                        turns.finish(turn, Turns.DONE, work instanceof Work.Job ? "closed" : "stopped", null);
                        return new Settlement.Done();
                    }
                    String inputId = sendInput(turn, thread, input(thread, work));
                    hooks.at(Point.INPUT_SENT, run);
                    turns.input(turn, inputId);
                    reading.inputId = inputId;
                }
                hooks.at(Point.INPUT_RECORDED, run);
                if (work instanceof Work.Job job) {
                    tasks.working(job.task());
                }
                while (reading.end == null) {
                    if (lease.lost()) {
                        throw new StaleTurnException(run.runId(), run.epoch());
                    }
                    if (!clock.now().isBefore(deadline)) {
                        return giveUp(run, turn, thread);
                    }
                    if (!reading.stopping && stopped(thread, work)) {
                        reading.stopping = true;
                        stop(run, thread);
                    }
                    if (!reading.stopping && answerWaitingCalls(run, turn, thread, work, reading)) {
                        return park(run, turn);
                    }
                    patience.next(tail, config.getPoll()).ifPresent(event -> see(thread, reading, event));
                }
                return finish(run, turn, thread, work, reading);
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
        List<String> vaults = credentials.vaultsFor(thread.appId(), thread.userId());
        String memory = Memories.access(profile.config().memory(), !thread.chat());
        List<SessionSpec.MemoryMount> mounts = memories.mounts(thread.appId(), thread.userId(), thread.agentKey(), memory);
        String sessionId = platform(() -> cma.createSession(new SessionSpec(profile.agent(), profile.environmentId(),
                thread.appId() + " / " + thread.userId() + " / " + thread.mergeKey(),
                Map.of("app", thread.appId(), "user", thread.userId(), "line", thread.mergeKey(),
                        "generation", Integer.toString(thread.generation() + 1)),
                vaults, profile.config().sessionBudgetUsd(), mounts)));
        hooks.at(Point.SESSION_CREATED, run);
        return threads.startSession(thread, sessionId, profile.agent(), vaults, memory);
    }

    /**
     * Gives the line a new session when its session is behind: the agent was configured anew since
     * it was created, or credentials came that need a vault it does not have. A session cannot take
     * either on. It happens only between turns, never in the middle of one; a new conversation session
     * is told what was said before.
     */
    private void renewIfOutdated(ClaimedRun run) {
        Threads.Thread thread = threads.find(run.tenantId(), run.inboxId(), run.mergeKey()).orElseThrow();
        if (thread.sessionId() == null) {
            return;
        }
        AgentProfiles.Ready profile = profiles.ready(thread.appId(), thread.agentKey());
        String vaults = String.join(",", credentials.vaultsFor(thread.appId(), thread.userId()));
        String why = !profile.agent().equals(thread.sessionAgent()) ? "the agent was configured anew"
                : !vaults.equals(thread.vaultIds()) ? "its credentials changed"
                : !Memories.access(profile.config().memory(), !thread.chat()).equals(thread.memory()) ? "its memory was turned on or off"
                : null;
        if (why != null) {
            log.info("line {} of {}/{} starts a new session: {}", thread.mergeKey(), thread.appId(), thread.userId(), why);
            threads.dropSession(thread);
        }
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

    /**
     * What the run's events announce: messages of the user, or events of the application. The Inbox
     * only holds their ids; what they say is read here.
     *
     * @return {@code null} when the run announces nothing that this service knows
     */
    private Work workOf(ClaimedRun run) {
        List<MessageView> said = new ArrayList<>();
        List<AppEvent> happened = new ArrayList<>();
        List<Work.Due> due = new ArrayList<>();
        List<Tasks.Report> reported = new ArrayList<>();
        List<Tasks.Note> told = new ArrayList<>();
        for (ClaimedEvent event : run.events()) {
            if (Messages.EVENT_TYPE.equals(event.type())) {
                String conversationId = event.payload().path("conversation_id").asString(null);
                String messageId = event.payload().path("message_id").asString(null);
                if (conversationId != null && messageId != null) {
                    messages.find(run.tenantId(), run.inboxId(), conversationId, messageId).ifPresent(said::add);
                }
            } else if (AppEvents.EVENT_TYPE.equals(event.type())) {
                String eventId = event.payload().path("event_id").asString(null);
                if (eventId != null) {
                    appEvents.find(run.tenantId(), run.inboxId(), eventId).ifPresent(happened::add);
                }
            } else if (Schedules.TIMER_EVENT.equals(event.type())) {
                // A timer of a schedule. One that was cancelled after it fell due is left out.
                String scheduleId = event.payload().path("payload").path("schedule_id").asString(null);
                if (scheduleId != null) {
                    Instant dueAt = parseInstant(event.payload().path("fire_at").asString(null), event.occurredAt());
                    boolean late = event.payload().path("catch_up").asBoolean(false);
                    schedules.find(run.tenantId(), run.inboxId(), scheduleId)
                            .filter(schedule -> Schedules.ACTIVE.equals(schedule.state()))
                            .ifPresent(schedule -> due.add(new Work.Due(schedule, dueAt, late)));
                }
            } else if (Tasks.REPORT_EVENT.equals(event.type()) || Tasks.MESSAGE_EVENT.equals(event.type())) {
                // A note of a delegated task: for the main agent on its line, for the worker on the task's.
                String taskId = event.payload().path("task_id").asString(null);
                int seq = event.payload().path("seq").asInt(0);
                Optional<Tasks.Task> task = taskId == null ? Optional.empty() : tasks.find(run.tenantId(), run.inboxId(), taskId);
                Optional<Tasks.Note> note = task.flatMap(found -> tasks.note(run.tenantId(), run.inboxId(), taskId, seq));
                if (note.isPresent() && Tasks.REPORT_EVENT.equals(event.type())) {
                    reported.add(new Tasks.Report(task.get(), note.get()));
                } else if (note.isPresent()) {
                    told.add(note.get());
                }
            }
        }
        Optional<Tasks.Task> delegated = tasks.ofLine(run.tenantId(), run.inboxId(), run.mergeKey());
        if (delegated.isPresent()) {
            // The line of a task: a worker's turn, on what the main agent sent or on its own follow-up.
            return told.isEmpty() && due.isEmpty() ? null : new Work.Job(delegated.get(), told, due);
        }
        if (!said.isEmpty()) {
            // Messages the user stopped before this turn began are not answered.
            long stoppedThrough = run.mergeKey() == null ? 0 : threads.stoppedThrough(run.tenantId(), run.inboxId(), run.mergeKey());
            return new Work.Chat(said.stream().filter(message -> message.seq() > stoppedThrough)
                    .sorted(Comparator.comparingLong(MessageView::seq)).toList());
        }
        if (happened.isEmpty() && due.isEmpty() && reported.isEmpty()) {
            return null;
        }
        // A main agent's turn that happens anyway also hears what the service did to its tasks without waking it.
        reported.addAll(tasks.toldQuietly(run.tenantId(), run.inboxId(), run.mergeKey()));
        return new Work.Task(happened, due, reported);
    }

    private static Instant parseInstant(String text, Instant otherwise) {
        try {
            return text == null ? otherwise : Instant.parse(text);
        } catch (java.time.format.DateTimeParseException e) {
            return otherwise;
        }
    }

    /**
     * Whether what this turn works for is no longer wanted: the user stopped the reply that a
     * conversation's turn is for, or the task that a worker's turn is on was closed.
     */
    private boolean stopped(Threads.Thread thread, Work work) {
        if (work instanceof Work.Job job) {
            return tasks.closed(thread.appId(), thread.userId(), job.task().taskId());
        }
        return work instanceof Work.Chat chat && !chat.messages().isEmpty()
                && threads.stoppedThrough(thread.appId(), thread.userId(), thread.mergeKey()) >= chat.messages().getLast().seq();
    }

    /** Interrupts the session for a stop. When that fails, the turn reads on to its end all the same, and its reply is dropped. */
    private void stop(ClaimedRun run, Threads.Thread thread) {
        log.info("run {}: {}", run.runId(), thread.work() ? "the task was closed" : "the user stopped the reply");
        try {
            platform(() -> cma.send(thread.sessionId(), List.of(new Outgoing.Interrupt())));
        } catch (CmaException e) {
            log.warn("run {}: the session could not be interrupted for a stop: {}", run.runId(), e.getMessage());
        }
    }

    /**
     * The notice that starts a turn in the background. What happened in the application's systems,
     * and the agent's notes to itself, are read with read_events. The instructions of a schedule the
     * application set up are the application's, and are given here.
     */
    private static String background(Work.Task task) {
        StringBuilder text = new StringBuilder("[From the service that runs you, not from the user. The user is not here and "
                + "sees nothing of what you write in this turn.]\n");
        int events = task.events().size();
        int followups = task.followups().size();
        int reports = task.reports().size();
        if (reports > 0) {
            // A main agent's turn: what its workers have for it, next to whatever else is waiting.
            List<String> waiting = new ArrayList<>();
            if (events > 0) {
                waiting.add(events == 1 ? "an event from the application's systems is waiting for you"
                        : events + " events from the application's systems are waiting for you");
            }
            if (followups > 0) {
                waiting.add(followups == 1 ? "a follow-up you set for yourself is due"
                        : followups + " follow-ups you set for yourself are due");
            }
            waiting.add(reports == 1 ? "a worker has something for you on a task you delegated"
                    : "workers have " + reports + " things for you on tasks you delegated");
            String all = String.join(", and ", waiting);
            text.append(Character.toUpperCase(all.charAt(0))).append(all, 1, all.length())
                    .append(". Call read_events to get ").append(waiting.size() == 1 && reports == 1 ? "it" : "them")
                    .append(", then decide what to do.");
        } else if (events > 0 && followups == 0) {
            text.append(events == 1 ? "An event from the application's systems is" : events + " events from the application's systems are")
                    .append(" waiting for you. Call read_events to get ").append(events == 1 ? "it" : "them")
                    .append(", then decide what to do.");
        } else if (events == 0 && followups > 0) {
            text.append(followups == 1 ? "A follow-up you set for yourself is due" : followups + " follow-ups you set for yourself are due")
                    .append(". Call read_events to see ").append(followups == 1 ? "your note" : "your notes")
                    .append(", then decide what to do.");
        } else if (events > 0) {
            text.append(events == 1 ? "An event from the application's systems is" : events + " events from the application's systems are")
                    .append(" waiting for you, and ").append(followups == 1 ? "a follow-up you set for yourself is due"
                            : followups + " follow-ups you set for yourself are due")
                    .append(". Call read_events to get them, then decide what to do.");
        }
        for (Work.Due item : task.scheduled()) {
            text.append(text.charAt(text.length() - 1) == '\n' ? "" : "\n")
                    .append("It is time for \"").append(item.schedule().name()).append("\", which the application scheduled")
                    .append(item.late() ? " for " + item.dueAt() + "; it comes late, because the service was not running then"
                            : "")
                    .append(". The application's instructions for it: ").append(item.schedule().instructions());
        }
        return text.append("\nIf the user needs to know or to do something, tell them with notify_user. If not, end the turn "
                + "without it.").toString();
    }

    /** What the turn works on, for its row: the ids of the messages or events, not what they say. */
    private static JsonNode stored(Work work) {
        ObjectNode stored = Json.STORED.createObjectNode();
        switch (work) {
            case Work.Chat chat -> {
                stored.put("kind", "chat");
                ArrayNode list = stored.putArray("messages");
                chat.messages().forEach(message -> list.addObject()
                        .put("conversation_id", message.conversationId()).put("message_id", message.messageId()));
            }
            case Work.Task task -> {
                stored.put("kind", "task");
                ArrayNode list = stored.putArray("events");
                task.events().forEach(event -> list.add(event.eventId()));
                storeDue(stored, task.due());
                ArrayNode reports = stored.putArray("reports");
                task.reports().forEach(report -> reports.addObject().put("task_id", report.task().taskId())
                        .put("seq", report.note().seq()));
            }
            case Work.Job job -> {
                stored.put("kind", "job").put("task_id", job.task().taskId());
                ArrayNode notes = stored.putArray("notes");
                job.notes().forEach(note -> notes.add(note.seq()));
                storeDue(stored, job.due());
            }
        }
        return stored;
    }

    private static void storeDue(ObjectNode stored, List<Work.Due> items) {
        ArrayNode due = stored.putArray("due");
        items.forEach(item -> due.addObject().put("schedule_id", item.schedule().scheduleId())
                .put("due_at", item.dueAt().toString()).put("late", item.late()));
    }

    /** @return {@code null} when what the turn worked on is no longer there */
    private Work load(ClaimedRun run, JsonNode stored) {
        if ("chat".equals(stored.path("kind").asString())) {
            List<MessageView> said = new ArrayList<>();
            for (JsonNode message : stored.path("messages")) {
                messages.find(run.tenantId(), run.inboxId(), message.path("conversation_id").asString(),
                        message.path("message_id").asString()).ifPresent(said::add);
            }
            return said.isEmpty() ? null : new Work.Chat(said);
        }
        List<Work.Due> due = new ArrayList<>();
        for (JsonNode item : stored.path("due")) {
            schedules.find(run.tenantId(), run.inboxId(), item.path("schedule_id").asString()).ifPresent(schedule ->
                    due.add(new Work.Due(schedule, Instant.parse(item.path("due_at").asString()), item.path("late").asBoolean())));
        }
        if ("job".equals(stored.path("kind").asString())) {
            Optional<Tasks.Task> task = tasks.find(run.tenantId(), run.inboxId(), stored.path("task_id").asString());
            if (task.isEmpty()) {
                return null;
            }
            List<Tasks.Note> told = new ArrayList<>();
            for (JsonNode seq : stored.path("notes")) {
                tasks.note(run.tenantId(), run.inboxId(), task.get().taskId(), seq.asInt()).ifPresent(told::add);
            }
            return told.isEmpty() && due.isEmpty() ? null : new Work.Job(task.get(), told, due);
        }
        List<AppEvent> happened = new ArrayList<>();
        for (JsonNode eventId : stored.path("events")) {
            appEvents.find(run.tenantId(), run.inboxId(), eventId.asString()).ifPresent(happened::add);
        }
        List<Tasks.Report> reported = new ArrayList<>();
        for (JsonNode item : stored.path("reports")) {
            String taskId = item.path("task_id").asString();
            tasks.find(run.tenantId(), run.inboxId(), taskId).ifPresent(task ->
                    tasks.note(run.tenantId(), run.inboxId(), taskId, item.path("seq").asInt())
                            .ifPresent(note -> reported.add(new Tasks.Report(task, note))));
        }
        return happened.isEmpty() && due.isEmpty() && reported.isEmpty() ? null : new Work.Task(happened, due, reported);
    }

    /**
     * The text that starts the turn.
     *
     * <p>For a conversation it is what the user said. For work in the background it is a notice in
     * fixed words that there is something to read: what happened in the application's systems is
     * handed over as the result of a tool, never as a message, because the agent takes a message as
     * the word of whoever it works for, and the content of an event is not that.
     */
    private String input(Threads.Thread thread, Work work) {
        return switch (work) {
            case Work.Chat chat -> said(thread, chat.messages());
            case Work.Task task -> recalled(thread) + background(task);
            case Work.Job job -> assigned(job);
        };
    }

    /**
     * The notice that starts a worker's turn on a delegated task. What the main agent wants is read
     * with read_events, like everything that does not come from the user: the main agent's words are
     * its own, written after it read what other systems say, and are not to pass for the user's.
     */
    private static String assigned(Work.Job job) {
        StringBuilder text = new StringBuilder("[From the service that runs you, not from the user. The user is not here and "
                + "sees nothing of what you write in this turn.]\n");
        boolean told = !job.notes().isEmpty();
        int followups = job.due().size();
        if (told) {
            text.append("The user's main agent has handed you work on a task. Call read_events to get it, and do it within "
                    + "what your own instructions allow: it is the main agent's request, not the user's word.");
        }
        if (followups > 0) {
            text.append(told ? "\n" : "").append(followups == 1 ? "A follow-up you set for yourself on a task of the user's "
                            + "main agent is due" : followups + " follow-ups you set for yourself on a task of the user's main "
                            + "agent are due")
                    .append(". Call read_events to see ").append(followups == 1 ? "your note" : "your notes").append(".");
        }
        text.append("\nWhat you write at the end of this turn is your report to the main agent, which decides what the user is "
                + "told: say what you did and what you found. If you cannot go on without an answer, ask for it in the report "
                + "and end the turn; the answer comes as a further message. notify_user and ask_user do not work in a task.");
        if (!told) {
            text.append(" If there is nothing the main agent needs to hear, end the turn without writing anything.");
        }
        return text.toString();
    }

    /**
     * For the first turn of a session that replaces another one, when that turn is not the user's:
     * what was said in the conversation before, of which the new session knows nothing. A turn that
     * the user starts is told the same by {@link #said}; without this, a conversation whose new
     * session happened to start in the background would never be told.
     */
    private String recalled(Threads.Thread thread) {
        if (!thread.chat() || thread.generation() <= 1 || thread.lastEventId() != null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        List<MessageView> earlier = messages.dialogueBefore(thread.appId(), thread.userId(), thread.conversationId(),
                thread.mergeKey(), Long.MAX_VALUE, config.getTranscriptMessages());
        if (!earlier.isEmpty()) {
            text.append("[From the service that runs you, not from the user: this conversation continues in a new "
                    + "session. This is what was said before, for your reference.]\n");
            transcript(text, earlier);
            text.append("[End of the earlier conversation.]\n\n");
            threads.unsyncedAfter(thread, earlier.getFirst().seq() - 1);
        }
        return text.append(delegatedBefore(thread)).toString();
    }

    private static void transcript(StringBuilder text, List<MessageView> earlier) {
        for (MessageView message : earlier) {
            String words = message.text().length() <= TRANSCRIPT_CHARS_PER_MESSAGE ? message.text()
                    : message.text().substring(0, TRANSCRIPT_CHARS_PER_MESSAGE) + "…";
            text.append(message.role().equals("user") ? "User: " : "You: ").append(words).append('\n');
        }
    }

    /** For a main agent's new session: that tasks it delegated in an earlier session are still open. */
    private String delegatedBefore(Threads.Thread thread) {
        int open = thread.chat() ? tasks.open(thread).size() : 0;
        if (open == 0 || !PlatformTools.delegates(profiles.config(thread.appId(), thread.agentKey()).coordinator())) {
            return "";
        }
        return "[From the service that runs you, not from the user: this session is new and knows nothing of the tasks you "
                + "delegated before. " + (open == 1 ? "One of them is" : open + " of them are")
                + " still open. Call list_tasks to see " + (open == 1 ? "it" : "them") + ".]\n\n";
    }

    /**
     * The user's messages, in order. Before them, where it applies, two notes from the service: in a
     * session that replaces another one, what was said before, because the new session knows none of
     * it; and that work in the background has exchanged messages with the user in this conversation,
     * which this session has not read.
     */
    private String said(Threads.Thread thread, List<MessageView> inputs) {
        StringBuilder text = new StringBuilder();
        long synced = thread.syncedSeq();
        if (thread.generation() > 1 && thread.lastEventId() == null) {
            List<MessageView> earlier = messages.dialogueBefore(thread.appId(), thread.userId(), thread.conversationId(),
                    thread.mergeKey(), inputs.getFirst().seq(), config.getTranscriptMessages());
            if (!earlier.isEmpty()) {
                text.append("[From the service that runs you, not from the user: this conversation continues in a new "
                        + "session. This is what was said before, for your reference.]\n");
                transcript(text, earlier);
                text.append("[End of the earlier conversation. The user's new message follows.]\n\n");
                // The new session has read nothing from the background. What came since the earliest thing it was told is offered to it.
                synced = Math.min(synced, earlier.getFirst().seq() - 1);
                threads.unsyncedAfter(thread, synced);
            }
            text.append(delegatedBefore(thread));
        }
        int unread = messages.outsideAfter(thread.appId(), thread.userId(), thread.conversationId(), thread.mergeKey(), synced,
                TurnTools.MAX_NOTIFICATIONS_READ).size();
        if (unread > 0) {
            text.append("[From the service that runs you, not from the user: since this session last looked, your work in "
                    + "the background has exchanged ").append(unread == 1 ? "a message" : unread + " messages")
                    .append(" with the user in this conversation. Call read_events to see ").append(unread == 1 ? "it" : "them")
                    .append(" before you answer.]\n\n");
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
     * Answers the tool calls the session waits for. The tools of this service are run here. A call
     * that needs a person waits for them: the first time it is seen it is put on record as an action,
     * and once the action has an outcome the session is told.
     *
     * @return whether the turn has nothing left to do but wait for people
     */
    private boolean answerWaitingCalls(ClaimedRun run, Turn turn, Threads.Thread thread, Work work, Reading reading) {
        List<String> outstanding = reading.outstanding();
        if (outstanding.isEmpty()) {
            return false;
        }
        actions.expireDue(run.runId());
        List<Outgoing> answers = new ArrayList<>();
        boolean waitsForPeople = false;
        for (String id : outstanding) {
            CmaEvent call = reading.calls.get(id);
            Outgoing answer;
            if (call instanceof CmaEvent.CustomToolUse custom && PlatformTools.ASK_USER.equals(custom.name())) {
                answer = work instanceof Work.Job
                        // A worker has one counterpart, the main agent, which is also who knows what the user already said.
                        ? new Outgoing.CustomToolResult(id, "Not asked: in a task you ask the main agent, not the user. Put "
                                + "your question into your report and end the turn; the answer comes as a further message.", true)
                        : asksInPassing(thread) ? askedInPassing(run, turn, thread, work, custom, reading.ordinal(custom))
                        : fromPerson(run, turn, thread, work, custom);
            } else if (call instanceof CmaEvent.CustomToolUse custom) {
                TurnTools.Answer result = tools.call(run, turn, thread, work, custom, reading.ordinal(custom));
                if (result.syncedTo() != null) {
                    reading.syncedTo = result.syncedTo();
                }
                answer = new Outgoing.CustomToolResult(id, result.text(), result.error());
            } else if (call instanceof CmaEvent.ToolUse use) {
                answer = use.mcpServer() != null ? fromPerson(run, turn, thread, work, use)
                        // A built-in tool that the platform would not judge alone: not something to put before an end user.
                        : new Outgoing.ToolConfirmation(id, false,
                                "Not approved: this service puts no built-in tool before anybody for approval.");
            } else {
                // Without the call there is no telling what kind of answer it wants. The turn's deadline ends the wait.
                log.warn("session {} waits for an answer to {}, a call this turn has not seen", thread.sessionId(), id);
                continue;
            }
            if (answer == null) {
                waitsForPeople = true;
            } else {
                reading.answered.add(id);
                answers.add(answer);
            }
        }
        if (answers.isEmpty()) {
            return waitsForPeople;
        }
        hooks.at(Point.TOOLS_RUN, run);
        for (int attempt = 1; ; attempt++) {
            try {
                cma.send(thread.sessionId(), answers);
                hooks.at(Point.ANSWERS_SENT, run);
                return false;
            } catch (CmaException e) {
                if (e.kind() == CmaException.Kind.INVALID_REQUEST && attempt > 1) {
                    // An earlier attempt got through after all: the calls are answered and no longer wait.
                    hooks.at(Point.ANSWERS_SENT, run);
                    return false;
                }
                if (!e.retryable() || attempt >= config.getAttempts()) {
                    throw e;
                }
                patience.pause(delay(attempt, e));
            }
        }
    }

    /** How long a turn of the run may take: a worker's turn on a task longer than any other, because nobody waits for it. */
    private Duration deadlineOf(ClaimedRun run) {
        return run.mergeKey() != null && run.mergeKey().startsWith(Threads.WORK_PREFIX) ? workerDeadline : config.getDeadline();
    }

    /** Whether the line is a main agent's: there a question does not make the turn wait. */
    private boolean asksInPassing(Threads.Thread thread) {
        return thread.chat() && profiles.config(thread.appId(), thread.agentKey()).coordinator() != null;
    }

    /**
     * Puts a main agent's question before the user without waiting for the answer. Everything that
     * concerns the user comes in on the main agent's line, so a turn that waited there would be ended
     * by the next event or report, and the question with it. The question is put on record and into
     * the conversation, and the call is answered at once: the user's answer is their next message,
     * which is a turn of its own. One question waits at a time, and in the background a question
     * counts against what the user is sent in a day, as a notification does.
     */
    private Outgoing askedInPassing(ClaimedRun run, Turn turn, Threads.Thread thread, Work work, CmaEvent.CustomToolUse call,
            int ordinal) {
        Actions.Call parsed;
        try {
            parsed = callOf(thread, work, call);
        } catch (IllegalArgumentException wrong) {
            return new Outgoing.CustomToolResult(call.id(), wrong.getMessage(), true);
        }
        // A main agent asks in the conversation it serves, whatever the turn is about.
        Actions.Call spec = new Actions.Call(parsed.kind(), parsed.approver(), thread.conversationId(), parsed.sessionId(),
                parsed.toolUseId(), null, null, null, parsed.question(), parsed.options());
        boolean unasked = !(work instanceof Work.Chat);
        String refusal = tx.execute(status -> {
            users.lock(thread.appId(), thread.userId());
            turns.stillHeld(turn);
            if (actions.ofCall(run.runId(), call.id()).isPresent()) {
                // An earlier delivery of the run asked it already.
                return null;
            }
            if (ordinal > 1) {
                return "Not asked: you have already asked the user a question in this turn. Put what you need to know "
                        + "into one question.";
            }
            if (unasked) {
                Instant since = clock.now().minus(TurnTools.QUOTA_WINDOW);
                long sent = messages.notificationsSince(thread.appId(), thread.userId(), since)
                        + actions.unaskedSince(thread.appId(), thread.userId(), since);
                if (sent >= notify.getPerUserPerDay()) {
                    return "Not asked: this user has been sent " + sent + " notifications and questions in the last 24 "
                            + "hours, which is the limit. Do not try again in this turn.";
                }
            }
            actions.withdrawInPassing(thread.appId(), thread.userId(), thread.mergeKey(), "the agent asked a newer question");
            Actions.Action asked = actions.ensure(thread.appId(), thread.userId(), thread.mergeKey(), run.runId(), spec, actionTtl,
                    true, unasked).action();
            messages.storeFromAgent(thread.appId(), thread.userId(), spec.conversationId(), thread.mergeKey(),
                    "ask:" + asked.actionId(), "question", spec.question(), run.runId(), asked.actionId());
            return null;
        });
        return refusal != null ? new Outgoing.CustomToolResult(call.id(), refusal, true)
                : new Outgoing.CustomToolResult(call.id(), "Asked. The user sees your question in the conversation. Their "
                        + "answer does not come back to this call: it comes as their next message, in a turn of its own, "
                        + "and you are told if they decline or do not answer in time. Do not wait and do not ask again: "
                        + "end this turn when you have nothing else to do.", false);
    }

    /**
     * The answer to a call that needs a person, once there is one; {@code null} while nobody has
     * decided. The first time the call is seen it is put on record as an action, unless the turn is
     * closing: then the call is not put before anybody, and gets its answer at once.
     */
    private Outgoing fromPerson(ClaimedRun run, Turn turn, Threads.Thread thread, Work work, CmaEvent call) {
        Optional<Actions.Action> known = actions.ofCall(run.runId(), call.id());
        if (known.isPresent()) {
            return answerFrom(call, known.get());
        }
        Actions.Call spec;
        try {
            spec = callOf(thread, work, call);
        } catch (IllegalArgumentException wrong) {
            // A question that was not put right. The agent is told, and can ask again.
            return new Outgoing.CustomToolResult(call.id(), wrong.getMessage(), true);
        }
        if (spec == null) {
            return new Outgoing.ToolConfirmation(call.id(), false, "Not approved: the tool's server is no longer configured.");
        }
        Put put = tx.execute(status -> {
            users.lock(thread.appId(), thread.userId());
            turns.stillHeld(turn);
            String closing = turns.closing(run.runId());
            if (closing != null) {
                return new Put(null, closing);
            }
            Actions.Ensured ensured = actions.ensure(thread.appId(), thread.userId(), thread.mergeKey(), run.runId(), spec, actionTtl);
            if (ensured.created() && Actions.QUESTION.equals(spec.kind())) {
                // The question is part of the conversation; the user may answer it by writing to it.
                messages.storeFromAgent(thread.appId(), thread.userId(), spec.conversationId(), thread.mergeKey(),
                        "ask:" + ensured.action().actionId(), "question", spec.question(), run.runId(), ensured.action().actionId());
            }
            return new Put(ensured.action(), null);
        });
        if (put.closing() != null) {
            return call instanceof CmaEvent.CustomToolUse
                    ? new Outgoing.CustomToolResult(call.id(), "No answer: " + put.closing() + ".", true)
                    : new Outgoing.ToolConfirmation(call.id(), false, "Not approved: " + put.closing() + ".");
        }
        return answerFrom(call, put.action());
    }

    /** What a call that needs a person is about, for its action; {@code null} for a tool of a server the agent no longer has. */
    private Actions.Call callOf(Threads.Thread thread, Work work, CmaEvent call) {
        String conversationId = work instanceof Work.Task task ? task.conversationId() : thread.conversationId();
        if (call instanceof CmaEvent.CustomToolUse ask) {
            JsonNode input = ask.input();
            String question = input == null ? null : input.path("question").asString(null);
            if (question == null || question.isBlank()) {
                throw new IllegalArgumentException("Not asked: question is required, the question for the user.");
            }
            if (question.length() > PlatformTools.MAX_QUESTION) {
                throw new IllegalArgumentException("Not asked: a question has at most " + PlatformTools.MAX_QUESTION + " characters.");
            }
            List<String> options = null;
            JsonNode given = input.path("options");
            if (!given.isMissingNode() && !given.isNull()) {
                if (!given.isArray() || given.size() > PlatformTools.MAX_OPTIONS) {
                    throw new IllegalArgumentException("Not asked: options is a list of at most " + PlatformTools.MAX_OPTIONS + " answers.");
                }
                options = new ArrayList<>();
                for (JsonNode option : given) {
                    String text = option.isString() ? option.asString() : null;
                    if (text == null || text.isBlank() || text.length() > PlatformTools.MAX_OPTION) {
                        throw new IllegalArgumentException("Not asked: each option is a text of at most " + PlatformTools.MAX_OPTION
                                + " characters.");
                    }
                    options.add(text);
                }
            }
            return new Actions.Call(Actions.QUESTION, Actions.BY_USER, conversationId, thread.sessionId(), call.id(), null, null,
                    null, question, options);
        }
        CmaEvent.ToolUse use = (CmaEvent.ToolUse) call;
        Optional<McpServerConfig> server = profiles.config(thread.appId(), thread.agentKey()).mcpServers().stream()
                .filter(candidate -> candidate.name().equals(use.mcpServer())).findFirst();
        return server.map(found -> new Actions.Call(Actions.APPROVAL, found.approver(), conversationId, thread.sessionId(),
                call.id(), use.mcpServer(), use.name(), use.input(), null, null)).orElse(null);
    }

    /** What the session is told about a call whose action has an outcome; {@code null} while it has none. */
    private static Outgoing answerFrom(CmaEvent call, Actions.Action action) {
        boolean question = Actions.QUESTION.equals(action.kind());
        String by = Actions.BY_USER.equals(action.resolvedBy()) ? "the user" : "the application";
        return switch (action.status()) {
            case Actions.PENDING -> null;
            case Actions.APPROVED -> new Outgoing.ToolConfirmation(call.id(), true, null);
            case Actions.ANSWERED -> new Outgoing.CustomToolResult(call.id(), action.answer(), false);
            case Actions.DENIED -> question
                    ? new Outgoing.CustomToolResult(call.id(), "Not answered: " + by + " declined the question"
                            + (action.note() == null ? "." : ": " + action.note()), true)
                    : new Outgoing.ToolConfirmation(call.id(), false, "Denied by " + by
                            + (action.note() == null ? "." : ": " + action.note()));
            case Actions.EXPIRED -> question
                    ? new Outgoing.CustomToolResult(call.id(), "No answer: the user did not answer in time.", true)
                    : new Outgoing.ToolConfirmation(call.id(), false, "Not approved: nobody decided in time.");
            default -> question
                    ? new Outgoing.CustomToolResult(call.id(), "No answer: " + action.note() + ".", true)
                    : new Outgoing.ToolConfirmation(call.id(), false, "Not approved: " + action.note() + ".");
        };
    }

    /**
     * Suspends the run in the Inbox until somebody decides, or until the first of its actions
     * expires. Whatever decides an action rings, which wakes the run; it then reads the outcome from
     * the actions. A ring that comes before the run is suspended is not lost: the Inbox keeps it.
     */
    private Settlement park(ClaimedRun run, Turn turn) {
        hooks.at(Point.PARKING, run);
        Instant now = clock.now();
        Instant wake = actions.earliestExpiry(run.runId()).orElse(now.plus(actionTtl));
        long seconds = Math.max(1, Duration.between(now, wake).toSeconds() + 1);
        turns.park(turn);
        ApiResult<RunOutcome> suspended = inbox.suspend(run.runId(), new SuspendRequest(run.epoch(),
                Actions.resumeKey(run.runId()), seconds, SuspendRequest.RESUME, run.lastRunSeq()));
        if (suspended.status() == 404 || suspended.status() == 409) {
            throw new StaleTurnException(run.runId(), run.epoch());
        }
        if (!suspended.ok()) {
            throw new IllegalStateException("the Inbox did not suspend run " + run.runId() + ": " + suspended.status() + " "
                    + suspended.code());
        }
        log.info("run {} waits for people, until {} at the latest", run.runId(), wake);
        return new Settlement.Parked();
    }

    private Settlement finish(ClaimedRun run, Turn turn, Threads.Thread thread, Work work, Reading reading) {
        switch (reading.end) {
            case TERMINATED, RETRIES_EXHAUSTED, BUDGET_REACHED ->
                    throw new SessionLost(thread, "the turn ended with " + reading.end.name().toLowerCase());
            default -> {
            }
        }
        String last = reading.lastText;
        boolean wrote = last != null && !last.isBlank();
        boolean refused = reading.end == End.REFUSAL;
        tx.executeWithoutResult(status -> {
            // The user's row first, as everywhere that records updates for the user.
            users.lock(thread.appId(), thread.userId());
            String outcome;
            String note = null;
            switch (work) {
                case Work.Chat chat when reading.stopping -> {
                    // The user stopped the reply: what the agent wrote is not given to them.
                    outcome = "stopped";
                }
                case Work.Chat chat -> {
                    // What the agent wrote last is its answer to the user.
                    if (wrote) {
                        messages.storeFromAgent(thread.appId(), thread.userId(), thread.conversationId(), thread.mergeKey(),
                                "reply:" + run.runId(), "reply", last, run.runId(), null);
                    }
                    if (reading.syncedTo != null) {
                        threads.synced(thread, reading.syncedTo);
                    }
                    outcome = refused ? "refused" : wrote ? "replied" : "silent";
                }
                case Work.Task task -> {
                    // Nobody reads what the agent wrote last. The user has what it sent with notify_user, if anything.
                    boolean notified = messages.notificationsOf(thread.appId(), thread.userId(), run.runId()) > 0;
                    outcome = notified ? "notified" : refused ? "refused" : "silent";
                    note = !wrote ? null : last.length() <= NOTE_CHARS ? last : last.substring(0, NOTE_CHARS) + "…";
                    schedules.fired(thread.appId(), thread.userId(),
                            task.due().stream().map(item -> item.schedule().scheduleId()).toList());
                    tasks.read(task.reports());
                }
                case Work.Job job -> {
                    // What the worker wrote last is its report to the main agent, whose line it wakes. A turn that
                    // the main agent started always reports, so that the main agent never waits for nothing; one
                    // that the worker's own follow-up started may have nothing to say. A closed task gets no report.
                    boolean asked = !job.notes().isEmpty();
                    String report = reading.stopping ? null
                            : wrote ? last
                            : refused ? "The worker declined to go on with the task."
                            : asked ? "The worker ended its turn without a report."
                            : null;
                    tasks.turnEnded(job.task(), run.runId(), job.notes().stream().map(Tasks.Note::seq).toList(), report);
                    outcome = reading.stopping ? "closed" : report != null ? "reported" : "silent";
                    schedules.fired(thread.appId(), thread.userId(),
                            job.due().stream().map(item -> item.schedule().scheduleId()).toList());
                }
            }
            if (reading.lastEventId != null) {
                threads.sawUpTo(thread, reading.lastEventId, reading.lastEventAt);
            }
            // Last, and conditional on the lease: if this worker is no longer the run's, what it wrote above is rolled back.
            turns.finish(turn, Turns.DONE, outcome, null, note);
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
        String error = "the turn took longer than " + deadlineOf(run);
        tx.executeWithoutResult(status -> {
            users.lock(thread.appId(), thread.userId());
            turns.finish(turn, Turns.FAILED, null, error);
            outbound.record(thread.appId(), thread.userId(), Update.RUN_FAILED, Outbound.failure(run.runId(), run.mergeKey(), error));
            tasks.turnFailed(thread.appId(), thread.userId(), run.mergeKey(), run.runId(), error);
        });
        actions.withdrawOfRun(run.runId(), "the turn that waited for this ran out of time");
        log.warn("run {} failed: {}", run.runId(), error);
        return new Settlement.Failed(error);
    }

    /** Ends the turn as failed for good. The Inbox does not deliver the run again. */
    private Settlement fail(ClaimedRun run, String error) {
        try {
            tx.executeWithoutResult(status -> {
                users.lock(run.tenantId(), run.inboxId());
                turns.find(run.runId()).filter(turn -> turn.epoch() == run.epoch() && Turns.OPEN.equals(turn.state()))
                        .ifPresent(turn -> turns.finish(turn, Turns.FAILED, null, error));
                outbound.record(run.tenantId(), run.inboxId(), Update.RUN_FAILED, Outbound.failure(run.runId(), run.mergeKey(), error));
                tasks.turnFailed(run.tenantId(), run.inboxId(), run.mergeKey(), run.runId(), error);
            });
        } catch (RuntimeException e) {
            log.warn("run {}: its failure could not be recorded: {}", run.runId(), e.toString());
        }
        actions.withdrawOfRun(run.runId(), "the turn that waited for this failed");
        log.warn("run {} failed: {}", run.runId(), error);
        return new Settlement.Failed(error);
    }

    private void settle(ClaimedRun run, Settlement settlement) {
        if (settlement instanceof Settlement.Parked) {
            return;
        }
        try {
            ApiResult<?> result = switch (settlement) {
                case Settlement.Done done -> inbox.ack(run.runId(), AckRequest.done(run.epoch(), run.lastRunSeq()));
                case Settlement.Failed failed ->
                        inbox.ack(run.runId(), AckRequest.failed(run.epoch(), run.lastRunSeq(), shorten(failed.error())));
                case Settlement.Retry retry -> inbox.nack(run.runId(), new NackRequest(run.epoch(), shorten(retry.reason()),
                        retry.delay() == null ? null : retry.delay().toMillis()));
                case Settlement.Parked parked -> throw new IllegalStateException("a parked run is settled already");
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
        /** The calls the session waits for, as its last idle said; empty again once it runs. */
        private List<String> waiting = List.of();
        private End end;
        private String lastEventId;
        private Instant lastEventAt;
        /** The position of the last notification that an answer of read_events told the session about, in this delivery. */
        private Long syncedTo;
        /** The user stopped the reply; the session has been told to stop. */
        private boolean stopping;

        Reading(String inputId) {
            this.inputId = inputId;
        }

        /** The calls the session waits for that have no answer yet. */
        List<String> outstanding() {
            return waiting.stream().filter(id -> !answered.contains(id)).toList();
        }

        /**
         * The call's place among the turn's calls of the same tool, from 1. It is the same whenever the
         * turn is read again, and it starts over in a session that replaces this one: so it names the
         * call, together with the run, wherever the call is made.
         */
        int ordinal(CmaEvent.CustomToolUse call) {
            int place = 0;
            for (CmaEvent other : calls.values()) {
                if (other instanceof CmaEvent.CustomToolUse custom && custom.name().equals(call.name())) {
                    place++;
                    if (custom.id().equals(call.id())) {
                        return place;
                    }
                }
            }
            throw new IllegalStateException("call " + call.id() + " is not one of the turn's");
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
