package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.bridge.Turns.Turn;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.events.AppEvents.AppEvent;
import dev.agentflow.agent.server.schedules.Schedules;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.agent.server.tools.PlatformTools;
import dev.agentflow.inbox.api.ClaimedRun;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Answers the agent's calls of the tools that this service gives it. A call can come twice, when a
 * run is delivered again or its session is replaced; what a tool does to the outside is therefore
 * keyed by the run and the call's place in it, and happens once.
 */
@Component
class TurnTools {

    /** How much one answer of read_events holds, in characters; what is left over comes with the next call. */
    private static final int RESULT_BUDGET = 48_000;

    static final int MAX_NOTIFICATIONS_READ = 20;

    private static final Duration QUOTA_WINDOW = Duration.ofHours(24);

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
    private final Schedules schedules;

    TurnTools(Messages messages, Threads threads, Users users, Turns turns, Schedules schedules, TransactionTemplate tx,
            AgentClock clock, AgentProperties properties) {
        this.messages = messages;
        this.threads = threads;
        this.users = users;
        this.turns = turns;
        this.tx = tx;
        this.clock = clock;
        this.limits = properties.getNotify();
        this.followupLimits = properties.getFollowups();
        this.schedules = schedules;
    }

    /** @param ordinal the call's place among the turn's calls of the same tool, from 1 */
    Answer call(ClaimedRun run, Turn turn, Threads.Thread line, Work work, CmaEvent.CustomToolUse call, int ordinal) {
        return switch (call.name()) {
            case PlatformTools.READ_EVENTS -> readEvents(line, work, call.input());
            case PlatformTools.NOTIFY_USER -> notifyUser(run, turn, line, work, call.input(), ordinal);
            case PlatformTools.SCHEDULE_FOLLOWUP -> scheduleFollowup(run, turn, line, work, call.input(), ordinal);
            default -> Answer.failed("This tool is not available.");
        };
    }

    // ================================================================ read_events

    private Answer readEvents(Threads.Thread line, Work work, JsonNode input) {
        return switch (work) {
            case Work.Task task -> waitingEvents(task, input);
            case Work.Chat chat -> unreadNotifications(line);
        };
    }

    /** The events of the run, as many as fit, from where the previous answer stopped. */
    private static Answer waitingEvents(Work.Task task, JsonNode input) {
        List<AppEvent> all = task.events();
        int from = 0;
        String after = input == null ? null : input.path("after").asString(null);
        if (after != null) {
            try {
                from = Integer.parseInt(after);
            } catch (NumberFormatException e) {
                from = -1;
            }
            if (from < 0 || from > all.size()) {
                return Answer.failed("after must be the value of next in the previous result.");
            }
        }
        ObjectNode result = Json.STORED.createObjectNode();
        ArrayNode events = result.putArray("events");
        int size = 0;
        int next = from;
        for (; next < all.size(); next++) {
            AppEvent event = all.get(next);
            ObjectNode one = Json.STORED.createObjectNode();
            one.put("event_id", event.eventId()).put("type", event.type()).put("subject", event.subject())
                    .put("received_at", event.createdAt().toString());
            one.set("data", event.data());
            int length = one.toString().length();
            if (next > from && size + length > RESULT_BUDGET) {
                break;
            }
            events.add(one);
            size += length;
        }
        if (next < all.size()) {
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
        int chain = (work instanceof Work.Task task ? task.chain() : 0) + 1;
        if (chain > followupLimits.getMaxChain()) {
            return Answer.failed("Not scheduled: this would be follow-up number " + chain + " in a row, and "
                    + followupLimits.getMaxChain() + " is the limit. Tell the user what there is to tell, or end the turn.");
        }
        String conversationId = work instanceof Work.Task task ? task.conversationId() : line.conversationId();
        // The same call of the same run always has this id, in whatever session and at whatever attempt it is made.
        Schedules.Followup followup = new Schedules.Followup("fu-" + run.runId() + "-" + ordinal, line.mergeKey(), line.agentKey(),
                conversationId, fireAt, note, chain);
        String refused = tx.execute(status -> {
            turns.stillHeld(turn);
            users.lock(line.appId(), line.userId());
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
            turns.stillHeld(turn);
            users.lock(line.appId(), line.userId());
            if (messages.find(line.appId(), line.userId(), conversationId, messageId).isPresent()) {
                return Answer.ok("Sent.");
            }
            long recent = messages.notificationsSince(line.appId(), line.userId(), clock.now().minus(QUOTA_WINDOW));
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
