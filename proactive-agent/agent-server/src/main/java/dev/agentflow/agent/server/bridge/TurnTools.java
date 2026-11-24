package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.server.actions.Actions;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.bridge.Turns.Turn;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.events.AppEvents.AppEvent;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.schedules.Schedules;
import dev.agentflow.agent.server.tasks.Tasks;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.agent.server.tools.PlatformTools;
import dev.agentflow.inbox.api.ClaimedRun;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Answers the agent's calls of the tools that this service gives it: the ones every agent has, and
 * the ones a main agent hands work to its workers with. A call can come twice, when a run is
 * delivered again or its session is replaced; what a tool does to the outside is therefore keyed by
 * the run and the call's place in it, and happens once.
 */
@Component
class TurnTools {

    /** How much one answer of read_events holds, in characters; what is left over comes with the next call. */
    private static final int RESULT_BUDGET = 48_000;

    static final int MAX_NOTIFICATIONS_READ = 20;

    static final Duration QUOTA_WINDOW = Duration.ofHours(24);

    /**
     * @param error whether the agent is told that the call failed
     * @param syncedTo for a conversation: the position of the last notification the answer told the session about
     */
    record Answer(String text, boolean error, Long syncedTo) {

        static Answer ok(String text) {
            return new Answer(text, false, null);
        }

        static Answer failed(String text) {
            return new Answer(text, true, null);
        }
    }

    private final Messages messages;
    private final Threads threads;
    private final Users users;
    private final Turns turns;
    private final TransactionTemplate tx;
    private final AgentClock clock;
    private final AgentProperties.Notify limits;
    private final AgentProperties.Followups followupLimits;
    private final AgentProperties.Tasks taskLimits;
    private final Schedules schedules;
    private final Tasks tasks;
    private final Actions actions;
    private final AgentProfiles profiles;

    TurnTools(Messages messages, Threads threads, Users users, Turns turns, Schedules schedules, Tasks tasks, Actions actions,
            AgentProfiles profiles, TransactionTemplate tx, AgentClock clock, AgentProperties properties) {
        this.messages = messages;
        this.threads = threads;
        this.users = users;
        this.turns = turns;
        this.tx = tx;
        this.clock = clock;
        this.limits = properties.getNotify();
        this.followupLimits = properties.getFollowups();
        this.taskLimits = properties.getTasks();
        this.schedules = schedules;
        this.tasks = tasks;
        this.actions = actions;
        this.profiles = profiles;
    }

    /** @param ordinal the call's place among the turn's calls of the same tool, from 1 */
    Answer call(ClaimedRun run, Turn turn, Threads.Thread line, Work work, CmaEvent.CustomToolUse call, int ordinal) {
        return switch (call.name()) {
            case PlatformTools.READ_EVENTS -> readEvents(line, work, call.input());
            case PlatformTools.NOTIFY_USER -> notifyUser(run, turn, line, work, call.input(), ordinal);
            case PlatformTools.SCHEDULE_FOLLOWUP -> scheduleFollowup(run, turn, line, work, call.input(), ordinal);
            case PlatformTools.DELEGATE_TASK -> delegateTask(run, turn, line, work, call.input(), ordinal);
            case PlatformTools.START_TASK -> startTask(turn, line, work, call.input());
            case PlatformTools.MESSAGE_WORKER -> messageWorker(run, turn, line, call.input(), ordinal);
            case PlatformTools.LIST_TASKS -> listTasks(line);
            case PlatformTools.CLOSE_TASK -> closeTask(turn, line, call.input());
            default -> Answer.failed("This tool is not available.");
        };
    }

    // ================================================================ read_events

    private Answer readEvents(Threads.Thread line, Work work, JsonNode input) {
        return switch (work) {
            case Work.Task task -> waitingEvents(task, input);
            case Work.Chat chat -> unreadNotifications(line);
            case Work.Job job -> handedOver(line, job);
        };
    }

    /**
     * The events of the run and, for a main agent, what its workers have for it, as many as fit, from
     * where the previous answer stopped: the events first, then the workers' reports.
     */
    private static Answer waitingEvents(Work.Task task, JsonNode input) {
        List<AppEvent> all = task.events();
        List<Tasks.Report> reports = task.reports();
        int total = all.size() + reports.size();
        int from = 0;
        String after = input == null ? null : input.path("after").asString(null);
        if (after != null) {
            try {
                from = Integer.parseInt(after);
            } catch (NumberFormatException e) {
                from = -1;
            }
            if (from < 0 || from > total) {
                return Answer.failed("after must be the value of next in the previous result.");
            }
        }
        ObjectNode result = Json.STORED.createObjectNode();
        ArrayNode events = result.putArray("events");
        ArrayNode reported = reports.isEmpty() ? null : result.putArray("reports");
        int size = 0;
        int next = from;
        for (; next < total; next++) {
            ObjectNode one = Json.STORED.createObjectNode();
            if (next < all.size()) {
                AppEvent event = all.get(next);
                one.put("event_id", event.eventId()).put("type", event.type()).put("subject", event.subject())
                        .put("received_at", event.createdAt().toString());
                one.set("data", event.data());
            } else {
                Tasks.Report report = reports.get(next - all.size());
                one.put("task_id", report.task().taskId()).put("role", report.task().role())
                        .put("title", report.task().title()).put("from", fromWhom(report.note()))
                        .put("kind", report.note().kind()).put("text", report.note().text())
                        .put("sent_at", report.note().createdAt().toString());
            }
            int length = one.toString().length();
            if (next > from && size + length > RESULT_BUDGET) {
                break;
            }
            if (next < all.size()) {
                events.add(one);
            } else {
                reported.add(one);
            }
            size += length;
        }
        if (next < total) {
            result.put("next", Integer.toString(next));
        }
        if (from == 0 && !task.followups().isEmpty()) {
            ArrayNode followups = result.putArray("followups");
            for (Work.Due item : task.followups()) {
                followups.addObject().put("note", item.schedule().note()).put("set_at", item.schedule().createdAt().toString())
                        .put("due_at", item.dueAt().toString());
            }
        }
        return Answer.ok(result.toString());
    }

    /**
     * What other lines of work exchanged with the user in this conversation since this one's session
     * last looked: notifications and questions of work in the background, and the user's answers.
     */
    private Answer unreadNotifications(Threads.Thread line) {
        long synced = threads.find(line.appId(), line.userId(), line.mergeKey()).map(Threads.Thread::syncedSeq)
                .orElse(line.syncedSeq());
        List<MessageView> unread = messages.outsideAfter(line.appId(), line.userId(), line.conversationId(), line.mergeKey(),
                synced, MAX_NOTIFICATIONS_READ);
        ObjectNode result = Json.STORED.createObjectNode();
        ArrayNode exchanged = result.putArray("messages");
        for (MessageView message : unread) {
            exchanged.addObject().put("sent_at", message.createdAt().toString())
                    .put("from", message.role().equals("user") ? "the user" : "you, in the background")
                    .put("kind", message.role().equals("user") ? "answer" : message.kind())
                    .put("text", message.text());
        }
        return new Answer(result.toString(), false, unread.isEmpty() ? null : unread.getLast().seq());
    }

    /**
     * What a worker's turn is given: what the main agent sent, and the worker's own notes for the
     * follow-ups that are due. A session that replaces another one knows nothing of the task so far,
     * so it is also given what was said about the task before, the latest of it when it is long.
     */
    private Answer handedOver(Threads.Thread line, Work.Job job) {
        ObjectNode result = Json.STORED.createObjectNode();
        result.putObject("task").put("task_id", job.task().taskId()).put("title", job.task().title())
                .put("role", job.task().role());
        if (line.generation() > 1 && line.lastEventId() == null) {
            int first = job.notes().isEmpty() ? Integer.MAX_VALUE : job.notes().getFirst().seq();
            List<Tasks.Note> before = tasks.notes(job.task()).stream().filter(note -> note.seq() < first).toList();
            List<Tasks.Note> kept = new ArrayList<>();
            int size = 0;
            for (int i = before.size() - 1; i >= 0 && size + before.get(i).text().length() <= RESULT_BUDGET / 2; i--) {
                kept.addFirst(before.get(i));
                size += before.get(i).text().length();
            }
            if (!before.isEmpty() && (kept.isEmpty() || kept.getFirst().seq() != before.getFirst().seq())) {
                // What the task is, in the main agent's first words, is kept whatever else had to go.
                Tasks.Note assignment = before.getFirst();
                kept.addFirst(new Tasks.Note(assignment.taskId(), assignment.seq(), assignment.direction(), assignment.kind(),
                        Tasks.excerpt(assignment.text()), assignment.runId(), assignment.createdAt(), assignment.readAt()));
            }
            ArrayNode earlier = result.putArray("earlier");
            kept.forEach(note -> said(earlier.addObject(), note));
        }
        ArrayNode handed = result.putArray("messages");
        job.notes().forEach(note -> said(handed.addObject(), note));
        if (!job.due().isEmpty()) {
            ArrayNode followups = result.putArray("followups");
            for (Work.Due item : job.due()) {
                followups.addObject().put("note", item.schedule().note()).put("set_at", item.schedule().createdAt().toString())
                        .put("due_at", item.dueAt().toString());
            }
        }
        return Answer.ok(result.toString());
    }

    private static void said(ObjectNode one, Tasks.Note note) {
        one.put("from", fromWhom(note)).put("kind", note.kind());
        if (Tasks.EVENT.equals(note.kind())) {
            // An event of the application's systems, as it is: information, like any event.
            one.set("event", Json.STORED.readTree(note.text()));
        } else if (Tasks.SCHEDULE.equals(note.kind())) {
            // What the application scheduled, with its instructions: the notice of the turn gave them too.
            one.set("schedule", Json.STORED.readTree(note.text()));
        } else {
            one.put("text", note.text());
        }
        one.put("sent_at", note.createdAt().toString());
    }

    /** Who a note of a task is from, in words for an agent. */
    private static String fromWhom(Tasks.Note note) {
        return switch (note.from()) {
            case "main" -> "the main agent";
            case "worker" -> "the worker";
            case "app" -> "the application";
            default -> "the service";
        };
    }

    // ================================================================ a main agent's tasks

    /** The main agent's line, if {@code line} is one: a conversation whose agent has workers. Otherwise why the tools do not work here. */
    private String notAMainAgent(Threads.Thread line) {
        if (!line.chat()) {
            return "you can only hand work to workers from the conversation you serve as the user's main agent.";
        }
        return workersOf(line).isEmpty() ? "this agent has no workers." : null;
    }

    private List<WorkerConfig> workersOf(Threads.Thread line) {
        CoordinatorConfig coordinator = profiles.config(line.appId(), line.agentKey()).coordinator();
        return PlatformTools.delegates(coordinator) ? coordinator.workers() : List.of();
    }

    private Answer delegateTask(ClaimedRun run, Turn turn, Threads.Thread line, Work work, JsonNode input, int ordinal) {
        String refusal = notAMainAgent(line);
        if (refusal != null) {
            return Answer.failed("Not delegated: " + refusal);
        }
        CoordinatorConfig coordinator = profiles.config(line.appId(), line.agentKey()).coordinator();
        String role = input == null ? null : input.path("role").asString(null);
        Optional<WorkerConfig> worker = coordinator.workers().stream().filter(candidate -> candidate.role().equals(role)).findFirst();
        if (worker.isEmpty()) {
            return Answer.failed("Not delegated: role is required, one of "
                    + coordinator.workers().stream().map(WorkerConfig::role).toList() + ".");
        }
        String title = input.path("title").asString(null);
        if (title == null || title.isBlank() || title.length() > Tasks.MAX_TITLE) {
            return Answer.failed("Not delegated: title is required, at most " + Tasks.MAX_TITLE + " characters: a short name for the task.");
        }
        String instructions = input.path("instructions").asString(null);
        if (instructions == null || instructions.isBlank() || instructions.length() > taskLimits.getMaxInstructions()) {
            return Answer.failed("Not delegated: instructions are required, at most " + taskLimits.getMaxInstructions()
                    + " characters: what the worker is to do, and everything it needs to know to do it.");
        }
        if (profiles.configIfThere(line.appId(), worker.get().agent()).filter(config -> config.coordinator() == null).isEmpty()) {
            return Answer.failed("Not delegated: the agent behind the role " + role + " is not available as a worker any more.");
        }
        JsonNode given = input.path("summary");
        String summary = given.isMissingNode() || given.isNull() ? null : given.asString("").strip();
        if (summary != null && (summary.isEmpty() || summary.length() > Tasks.MAX_SUMMARY)) {
            return Answer.failed("Not delegated: summary has at most " + Tasks.MAX_SUMMARY + " characters: what you want to do and "
                    + "why, written for the user.");
        }
        boolean askFirst = input.path("ask_first").asBoolean(false);
        // A turn that the user began is the user's word; anything else is the main agent's own initiative.
        boolean userTurn = work instanceof Work.Chat;
        int maxActive = coordinator.maxActiveTasks() == null ? Tasks.DEFAULT_MAX_ACTIVE : coordinator.maxActiveTasks();
        // The same call of the same run always names the same task, in whatever session and at whatever attempt it is made.
        Tasks.Delegated done = tx.execute(status -> {
            users.lock(line.appId(), line.userId());
            turns.stillHeld(turn);
            return tasks.delegate(line, run.runId(), ordinal, worker.get(), title.strip(), instructions, summary, userTurn,
                    askFirst, maxActive);
        });
        if (done.refused() != null) {
            return Answer.failed(done.refused());
        }
        ObjectNode result = Json.STORED.createObjectNode();
        result.put("task_id", done.task().taskId()).put("role", done.task().role()).put("status", done.task().status());
        if (done.task().proposed()) {
            boolean oneByOne = WorkerConfig.ALWAYS_ASK.equals(worker.get().startRule());
            if (done.held() != null) {
                result.put("held", done.held());
            }
            result.put("next", "Not started: the user has to confirm it first. Your summary of it has been put before them as a "
                    + "message in the conversation, so do not send it again. You are told if they decline. "
                    + (oneByOne ? "Tasks of this role are confirmed in the application, one by one. "
                            : "If they tell you in the conversation to go ahead, start it with start_task. ")
                    + "Once it has started, the worker's report wakes you. There is nothing to wait for in this turn.");
        } else {
            if (Tasks.BY_GRANT.equals(done.task().authorizedBy())) {
                result.put("started", "The user has allowed you to start tasks of this role without asking.");
            }
            result.put("next", "The worker's report wakes you. Until then there is nothing to wait for in this turn.");
        }
        return Answer.ok(result.toString());
    }

    /**
     * Starts a task that waits for the user's confirmation, because the user told the main agent to go
     * ahead. That is the user's word only in a turn that the user began, and not for a role whose
     * tasks the application wants confirmed one by one.
     */
    private Answer startTask(Turn turn, Threads.Thread line, Work work, JsonNode input) {
        String refusal = notAMainAgent(line);
        if (refusal != null) {
            return Answer.failed("Not started: " + refusal);
        }
        String taskId = input == null ? null : input.path("task_id").asString(null);
        if (taskId == null || taskId.isBlank()) {
            return Answer.failed("Not started: task_id is required, as delegate_task or list_tasks named the task.");
        }
        if (!(work instanceof Work.Chat)) {
            return Answer.failed("Not started: only the user can confirm a task, in the application or by telling you in the "
                    + "conversation to go ahead. The user did not begin this turn.");
        }
        List<WorkerConfig> workers = workersOf(line);
        return tx.execute(status -> {
            users.lock(line.appId(), line.userId());
            turns.stillHeld(turn);
            Optional<Tasks.Task> task = ownTask(line, taskId);
            if (task.isEmpty()) {
                return Answer.failed("Not started: you have no task " + taskId + ". list_tasks shows the open ones.");
            }
            boolean oneByOne = workers.stream().filter(worker -> worker.role().equals(task.get().role()))
                    .anyMatch(worker -> WorkerConfig.ALWAYS_ASK.equals(worker.startRule()));
            if (oneByOne && task.get().proposed()) {
                return Answer.failed("Not started: tasks of the role " + task.get().role() + " are confirmed by the user in the "
                        + "application, one by one. Tell them that it waits for them there.");
            }
            return switch (tasks.startIn(task.get(), Tasks.BY_CONVERSATION)) {
                case STARTED -> Answer.ok("Started. The worker's report wakes you.");
                case RUNNING -> Answer.ok("It had started already. The worker's report wakes you.");
                case CLOSED -> Answer.failed("Not started: the task is closed. Delegate it anew if it is still wanted.");
                case EXPIRED -> Answer.failed("Not started: the task waited too long for a confirmation and was closed. "
                        + "Delegate it anew if it is still wanted.");
            };
        });
    }

    private Answer messageWorker(ClaimedRun run, Turn turn, Threads.Thread line, JsonNode input, int ordinal) {
        String refusal = notAMainAgent(line);
        if (refusal != null) {
            return Answer.failed("Not sent: " + refusal);
        }
        String taskId = input == null ? null : input.path("task_id").asString(null);
        String text = input == null ? null : input.path("text").asString(null);
        if (taskId == null || taskId.isBlank()) {
            return Answer.failed("Not sent: task_id is required, as delegate_task or list_tasks named the task.");
        }
        if (text == null || text.isBlank() || text.length() > taskLimits.getMaxInstructions()) {
            return Answer.failed("Not sent: text is required, at most " + taskLimits.getMaxInstructions() + " characters.");
        }
        return tx.execute(status -> {
            users.lock(line.appId(), line.userId());
            turns.stillHeld(turn);
            Optional<Tasks.Task> task = ownTask(line, taskId);
            if (task.isEmpty()) {
                return Answer.failed("Not sent: you have no task " + taskId + ". list_tasks shows the open ones.");
            }
            if (task.get().closed()) {
                return Answer.failed("Not sent: the task is closed. Delegate a new task for what is left to do.");
            }
            if (task.get().proposed()) {
                // What the user confirms is what was proposed to them: it is not changed behind the proposal.
                return Answer.failed("Not sent: the task has not started, because the user has not confirmed it yet. If it "
                        + "should be different, close it and delegate it anew.");
            }
            tasks.message(task.get(), run.runId(), ordinal, text);
            return Answer.ok("Sent. The worker's next report wakes you.");
        });
    }

    private Answer listTasks(Threads.Thread line) {
        String refusal = notAMainAgent(line);
        if (refusal != null) {
            return Answer.failed("Not listed: " + refusal);
        }
        ObjectNode result = Json.STORED.createObjectNode();
        ArrayNode open = result.putArray("tasks");
        for (Tasks.Task task : tasks.open(line)) {
            ObjectNode one = open.addObject();
            one.put("task_id", task.taskId()).put("role", task.role()).put("title", task.title()).put("status", task.status())
                    .put("started_by", task.ofApplication() ? "the application" : "you")
                    .put("delegated_at", task.createdAt().toString());
            if (task.subject() != null) {
                one.put("subject", task.subject());
            }
            // What the worker waits for people to decide: the main agent is who the user asks about it.
            List<Actions.Action> waiting = actions.pendingOfLine(task.appId(), task.userId(), task.line());
            if (!waiting.isEmpty()) {
                ArrayNode waitsFor = one.putArray("waits_for");
                waiting.forEach(action -> waitsFor.add((Actions.BY_USER.equals(action.approver()) ? "the user's" : "the application's")
                        + " approval of " + action.toolServer() + "." + action.toolName()));
            }
            tasks.lastForMain(task).ifPresent(note -> one.putObject("latest").put("from", fromWhom(note))
                    .put("kind", note.kind()).put("text", Tasks.excerpt(note.text())).put("sent_at", note.createdAt().toString()));
        }
        // What is still to come for the workers without the main agent's doing: the application's schedules for them.
        List<Schedules.Schedule> standing = schedules.forWorkers(line.appId(), line.userId(), line.mergeKey());
        if (!standing.isEmpty()) {
            ArrayNode scheduled = result.putArray("scheduled_by_the_application");
            for (Schedules.Schedule schedule : standing) {
                ObjectNode one = scheduled.addObject();
                one.put("name", schedule.name()).put("role", tasks.roleOf(line, schedule.agentKey())).put("subject", schedule.subject());
                if (schedule.cron() == null) {
                    one.put("at", schedule.fireAt().toString());
                } else {
                    one.put("cron", schedule.cron()).put("time_zone", schedule.timeZone());
                }
                if (schedule.lastFiredAt() != null) {
                    one.put("last_due_at", schedule.lastFiredAt().toString());
                }
                one.put("instructions", Tasks.excerpt(schedule.instructions()));
            }
        }
        return Answer.ok(result.toString());
    }

    private Answer closeTask(Turn turn, Threads.Thread line, JsonNode input) {
        String refusal = notAMainAgent(line);
        if (refusal != null) {
            return Answer.failed("Not closed: " + refusal);
        }
        String taskId = input == null ? null : input.path("task_id").asString(null);
        String reason = input == null ? null : input.path("reason").asString(null);
        if (taskId == null || taskId.isBlank()) {
            return Answer.failed("Not closed: task_id is required, as delegate_task or list_tasks named the task.");
        }
        if (reason != null && (reason.isBlank() || reason.length() > Tasks.MAX_REASON)) {
            return Answer.failed("Not closed: reason has at most " + Tasks.MAX_REASON + " characters.");
        }
        Optional<Tasks.Task> closed = tx.execute(status -> {
            users.lock(line.appId(), line.userId());
            turns.stillHeld(turn);
            Optional<Tasks.Task> task = ownTask(line, taskId);
            if (task.isPresent()) {
                tasks.closeIn(task.get(), Tasks.BY_AGENT, reason);
            }
            return task;
        });
        if (closed.isEmpty()) {
            return Answer.failed("Not closed: you have no task " + taskId + ". list_tasks shows the open ones.");
        }
        tasks.afterClose(closed.get());
        return Answer.ok("Closed.");
    }

    /** The task {@code taskId}, if this main agent's line delegated it. */
    private Optional<Tasks.Task> ownTask(Threads.Thread line, String taskId) {
        return tasks.find(line.appId(), line.userId(), taskId).filter(task -> task.hubKey().equals(line.mergeKey()));
    }

    // ================================================================ schedule_followup

    private Answer scheduleFollowup(ClaimedRun run, Turn turn, Threads.Thread line, Work work, JsonNode input, int ordinal) {
        String note = input == null ? null : input.path("note").asString(null);
        if (note == null || note.isBlank() || note.length() > PlatformTools.MAX_NOTE) {
            return Answer.failed("Not scheduled: note is required, at most " + PlatformTools.MAX_NOTE
                    + " characters: what you are to do then.");
        }
        JsonNode minutes = input.path("in_minutes");
        JsonNode at = input.path("at");
        boolean byMinutes = !minutes.isMissingNode() && !minutes.isNull();
        boolean byTime = !at.isMissingNode() && !at.isNull();
        if (byMinutes == byTime) {
            return Answer.failed("Not scheduled: give either in_minutes or at, exactly one of them.");
        }
        Instant now = clock.now();
        Instant fireAt;
        if (byMinutes) {
            if (!minutes.canConvertToLong() || minutes.asLong() < 1) {
                return Answer.failed("Not scheduled: in_minutes is a whole number of minutes, at least 1.");
            }
            fireAt = now.plus(Duration.ofMinutes(Math.min(minutes.asLong(), followupLimits.getMaxDelay().toMinutes() + 1)));
        } else {
            try {
                fireAt = java.time.OffsetDateTime.parse(at.asString("")).toInstant();
            } catch (DateTimeParseException e) {
                return Answer.failed("Not scheduled: at is a moment in ISO-8601, for example 2026-11-23T09:00:00Z.");
            }
            if (fireAt.isBefore(now.plus(Duration.ofMinutes(1)))) {
                return Answer.failed("Not scheduled: at is at least a minute from now (it is " + now + ").");
            }
        }
        if (fireAt.isAfter(now.plus(followupLimits.getMaxDelay()))) {
            return Answer.failed("Not scheduled: a follow-up is at most " + followupLimits.getMaxDelay().toDays()
                    + " days ahead.");
        }
        int chain = switch (work) {
            case Work.Task task -> task.chain();
            case Work.Job job -> job.chain();
            case Work.Chat chat -> 0;
        } + 1;
        if (chain > followupLimits.getMaxChain()) {
            return Answer.failed("Not scheduled: this would be follow-up number " + chain + " in a row, and "
                    + followupLimits.getMaxChain() + " is the limit. Tell the user what there is to tell, or end the turn.");
        }
        String conversationId = work instanceof Work.Task task ? task.conversationId() : line.conversationId();
        // The same call of the same run always has this id, in whatever session and at whatever attempt it is made.
        Schedules.Followup followup = new Schedules.Followup("fu-" + run.runId() + "-" + ordinal, line.mergeKey(), line.agentKey(),
                conversationId, fireAt, note, chain);
        String refused = tx.execute(status -> {
            users.lock(line.appId(), line.userId());
            turns.stillHeld(turn);
            if (schedules.find(line.appId(), line.userId(), followup.scheduleId()).isPresent()) {
                return null;
            }
            if (schedules.pendingFollowups(line.appId(), line.userId(), line.mergeKey()) >= followupLimits.getMaxPending()) {
                return "Not scheduled: " + followupLimits.getMaxPending() + " follow-ups already wait here, which is the limit.";
            }
            schedules.storeFollowup(line.appId(), line.userId(), followup);
            return null;
        });
        if (refused != null) {
            return Answer.failed(refused);
        }
        try {
            Schedules.Schedule stored = schedules.withTimer(schedules.find(line.appId(), line.userId(), followup.scheduleId()).orElseThrow());
            return Answer.ok("Scheduled for " + stored.fireAt() + ".");
        } catch (AgentException e) {
            return Answer.failed("Not scheduled: " + e.getMessage() + ".");
        }
    }

    // ================================================================ notify_user

    private Answer notifyUser(ClaimedRun run, Turn turn, Threads.Thread line, Work work, JsonNode input, int ordinal) {
        if (work instanceof Work.Job) {
            return Answer.failed("Not sent: in a task you report to the main agent, which decides what the user is told. "
                    + "Write what the user should know into your report at the end of this turn.");
        }
        if (!(work instanceof Work.Task task)) {
            return Answer.failed("You are in a conversation with the user: what you write at the end of this turn is your "
                    + "answer to them. notify_user is for work in the background.");
        }
        String text = input == null ? null : input.path("text").asString(null);
        if (text == null || text.isBlank()) {
            return Answer.failed("text is required: the message for the user.");
        }
        if (text.length() > limits.getMaxText()) {
            return Answer.failed("Not sent: a notification has at most " + limits.getMaxText() + " characters.");
        }
        if (ordinal > limits.getPerRun()) {
            return Answer.failed("Not sent: one turn in the background can call notify_user " + limits.getPerRun()
                    + " times at most. Do not try again.");
        }
        // The same call of the same run always has this id, in whatever session and at whatever attempt it is made.
        String messageId = "notify:" + run.runId() + ":" + ordinal;
        String conversationId = task.conversationId();
        return tx.execute(status -> {
            users.lock(line.appId(), line.userId());
            turns.stillHeld(turn);
            if (messages.find(line.appId(), line.userId(), conversationId, messageId).isPresent()) {
                return Answer.ok("Sent.");
            }
            // What a main agent asked the user unasked counts as well: it reaches them the same way.
            Instant since = clock.now().minus(QUOTA_WINDOW);
            long recent = messages.notificationsSince(line.appId(), line.userId(), since)
                    + actions.unaskedSince(line.appId(), line.userId(), since);
            if (recent >= limits.getPerUserPerDay()) {
                return Answer.failed("Not sent: this user has been sent " + recent + " notifications in the last 24 hours, "
                        + "which is the limit. Do not try again in this turn.");
            }
            messages.storeFromAgent(line.appId(), line.userId(), conversationId, line.mergeKey(), messageId, "notification", text,
                    run.runId(), null);
            return Answer.ok("Sent.");
        });
    }
}
