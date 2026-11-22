package dev.agentflow.agent.server.bridge;

import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.bridge.Turns.Turn;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.events.AppEvents.AppEvent;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.agent.server.tools.PlatformTools;
import dev.agentflow.inbox.api.ClaimedRun;
import java.time.Duration;
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

    TurnTools(Messages messages, Threads threads, Users users, Turns turns, TransactionTemplate tx, AgentClock clock,
            AgentProperties properties) {
        this.messages = messages;
        this.threads = threads;
        this.users = users;
        this.turns = turns;
        this.tx = tx;
        this.clock = clock;
        this.limits = properties.getNotify();
    }

    /** @param ordinal the call's place among the turn's calls of the same tool, from 1 */
    Answer call(ClaimedRun run, Turn turn, Threads.Thread line, Work work, CmaEvent.CustomToolUse call, int ordinal) {
        return switch (call.name()) {
            case PlatformTools.READ_EVENTS -> readEvents(line, work, call.input());
            case PlatformTools.NOTIFY_USER -> notifyUser(run, turn, line, work, call.input(), ordinal);
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
        return Answer.ok(result.toString());
    }

    /** What the user was told in this conversation by other lines of work, since this one's session last looked. */
    private Answer unreadNotifications(Threads.Thread line) {
        long synced = threads.find(line.appId(), line.userId(), line.mergeKey()).map(Threads.Thread::syncedSeq)
                .orElse(line.syncedSeq());
        List<MessageView> unread = messages.notificationsAfter(line.appId(), line.userId(), line.conversationId(), synced,
                MAX_NOTIFICATIONS_READ);
        ObjectNode result = Json.STORED.createObjectNode();
        ArrayNode notifications = result.putArray("notifications");
        for (MessageView notification : unread) {
            notifications.addObject().put("sent_at", notification.createdAt().toString()).put("text", notification.text());
        }
        return new Answer(result.toString(), false, unread.isEmpty() ? null : unread.getLast().seq());
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
            messages.storeFromAgent(line.appId(), line.userId(), conversationId, messageId, "notification", text, run.runId());
            return Answer.ok("Sent.");
        });
    }
}
