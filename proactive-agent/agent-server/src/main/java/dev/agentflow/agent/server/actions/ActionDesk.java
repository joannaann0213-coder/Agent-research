package dev.agentflow.agent.server.actions;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.ActionView;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.server.actions.Actions.Action;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.events.AppEvents;
import dev.agentflow.agent.server.time.AgentClock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.ObjectNode;

/**
 * Where people see what the agent waits for, and decide. The application's back end may decide
 * every action of its users; a user's own client only what is the user's to decide.
 */
@Service
public class ActionDesk {

    public static final int MAX_ANSWER = 4000;
    public static final int MAX_MESSAGE = 1000;

    /** The type of the event that tells a main agent that a question it asked in passing ended without an answer. */
    public static final String UNANSWERED = AppEvents.RESERVED_PREFIX + "question_unanswered";

    private final Actions actions;
    private final Messages messages;
    private final AppEvents appEvents;
    private final Users users;
    private final TransactionTemplate tx;
    private final AgentClock clock;

    public ActionDesk(Actions actions, Messages messages, AppEvents appEvents, Users users, TransactionTemplate tx,
            AgentClock clock) {
        this.actions = actions;
        this.messages = messages;
        this.appEvents = appEvents;
        this.users = users;
        this.tx = tx;
        this.clock = clock;
    }

    public ActionPage pending(Caller caller, String userId, String conversationId) {
        String appId = caller.requireAppOrUser(userId);
        Names.userId(userId);
        if (conversationId != null) {
            Names.localId(conversationId, "conversation_id");
        }
        String approver = caller.kind() == Caller.Kind.USER ? Actions.BY_USER : null;
        return new ActionPage(actions.pending(appId, userId, approver, conversationId, 100).stream().map(Action::view).toList());
    }

    public ActionView get(Caller caller, String userId, String actionId) {
        String appId = caller.requireAppOrUser(userId);
        Action action = visible(caller, appId, userId, actionId);
        if (action.pending() && !action.expiresAt().isAfter(clock.now())) {
            // Its time is up; the turn that waits learns it when the Inbox wakes it. Say so now already.
            Action due = action;
            tx.executeWithoutResult(status -> {
                users.lock(appId, userId);
                expire(due);
            });
            action = actions.find(appId, userId, actionId).orElseThrow();
        }
        return action.view();
    }

    /**
     * Ends the questions that were asked in passing and that nobody answered in time. No turn waits
     * for such a question, so nothing else would end it. One pass of the sweep; takes each user's
     * lock itself.
     *
     * @return how many it ended
     */
    public int expireUnanswered(int limit) {
        int ended = 0;
        for (Action candidate : actions.inPassingDue(limit)) {
            boolean done = Boolean.TRUE.equals(tx.execute(status -> {
                users.lock(candidate.appId(), candidate.userId());
                // It may have been answered since it was found.
                return actions.find(candidate.appId(), candidate.userId(), candidate.actionId())
                        .filter(action -> action.pending() && !action.expiresAt().isAfter(clock.now()))
                        .map(this::expire).orElse(false);
            }));
            if (done) {
                ended++;
            }
        }
        return ended;
    }

    /**
     * Ends an action whose time is up. A turn that waits for it learns of that when the Inbox wakes
     * it; for a question asked in passing no turn waits, so the agent that asked is told. In the
     * caller's transaction, under the user's lock.
     */
    private boolean expire(Action action) {
        boolean ended = actions.settle(action, Actions.EXPIRED, null, Actions.EXPIRED_NOTE, Actions.BY_SERVICE);
        if (ended && action.inPassing()) {
            unanswered(action, "expired", null);
        }
        return ended;
    }

    /** Tells the main agent that a question it asked in passing got no answer: an event of its line, which it reads in the background. */
    private void unanswered(Action action, String outcome, String reason) {
        ObjectNode data = Json.STORED.createObjectNode();
        data.put("from", "the service that runs you").put("question", action.question()).put("outcome", outcome);
        if (reason != null) {
            data.put("reason", reason);
        }
        appEvents.fromService(action.appId(), action.userId(), action.mergeKey(), action.conversationId(),
                "unanswered:" + action.actionId(), UNANSWERED, "question", data);
    }

    /**
     * Decides an action. Deciding it again the same way is harmless; deciding it otherwise, or
     * deciding one that has ended without a decision, is a conflict.
     */
    public ActionView resolve(Caller caller, String userId, String actionId, ResolveActionRequest request) {
        String appId = caller.requireAppOrUser(userId);
        Names.userId(userId);
        if (request == null) {
            throw AgentException.invalid("a request body is required");
        }
        String by = caller.kind() == Caller.Kind.USER ? Actions.BY_USER : Actions.BY_APP;
        Resolution resolution = tx.execute(status -> {
            if (users.find(appId, userId).isEmpty()) {
                throw AgentException.notFound("no action " + actionId + " for this user");
            }
            users.lock(appId, userId);
            Action action = visible(caller, appId, userId, actionId);
            Decision decision = decisionFor(action, request);
            if (action.pending() && !action.expiresAt().isAfter(clock.now())) {
                expire(action);
                action = actions.find(appId, userId, actionId).orElseThrow();
            }
            if (action.pending()) {
                actions.settle(action, decision.status(), decision.answer(), decision.note(), by);
                if (action.inPassing()) {
                    // No turn waits for it. The answer is the user's next message; a refusal is told to the agent.
                    if (Actions.ANSWERED.equals(decision.status())) {
                        messages.answerInPassing(appId, userId, action.conversationId(), "answer:" + actionId, decision.answer(),
                                actionId);
                    } else {
                        unanswered(action, "declined", decision.note());
                    }
                } else {
                    if (Actions.ANSWERED.equals(decision.status())) {
                        // The answer is part of the conversation, whichever way it was given.
                        messages.storeAnswer(appId, userId, action.conversationId(), "answer:" + actionId, decision.answer(),
                                action.mergeKey(), actionId);
                    }
                    actions.ring(appId, userId, action.mergeKey(), action.runId());
                }
                return new Resolution(actions.find(appId, userId, actionId).orElseThrow().view(), null);
            }
            if (action.status().equals(decision.status()) && Objects.equals(action.answer(), decision.answer())) {
                return new Resolution(action.view(), null);
            }
            return new Resolution(null, "this action is " + action.status() + " already"
                    + (action.note() == null ? "" : ": " + action.note()));
        });
        // Refused only now, after the commit: an expiry found on the way stays on record.
        if (resolution.conflict() != null) {
            throw AgentException.conflict(resolution.conflict());
        }
        return resolution.view();
    }

    private record Resolution(ActionView view, String conflict) {
    }

    private record Decision(String status, String answer, String note) {
    }

    private static Decision decisionFor(Action action, ResolveActionRequest request) {
        String message = request.message();
        if (message != null && (message.isBlank() || message.length() > MAX_MESSAGE)) {
            throw AgentException.invalid("message has 1 to " + MAX_MESSAGE + " characters");
        }
        if (message != null && !"deny".equals(request.decision())) {
            throw AgentException.invalid("message goes with a denial: it tells the agent why");
        }
        if (Actions.APPROVAL.equals(action.kind())) {
            if (request.answer() != null) {
                throw AgentException.invalid("an approval takes a decision, not an answer");
            }
            return switch (request.decision() == null ? "" : request.decision()) {
                case "approve" -> new Decision(Actions.APPROVED, null, null);
                case "deny" -> new Decision(Actions.DENIED, null, message);
                default -> throw AgentException.invalid("decision must be approve or deny");
            };
        }
        if ("deny".equals(request.decision())) {
            if (request.answer() != null) {
                throw AgentException.invalid("a question is either answered or declined, not both");
            }
            return new Decision(Actions.DENIED, null, message);
        }
        if (request.decision() != null) {
            throw AgentException.invalid("a question takes an answer; decision can only be deny, to decline it");
        }
        if (request.answer() == null || request.answer().isBlank() || request.answer().length() > MAX_ANSWER) {
            throw AgentException.invalid("answer is required and has at most " + MAX_ANSWER + " characters");
        }
        return new Decision(Actions.ANSWERED, request.answer(), null);
    }

    /** The action, if the caller may see it. A user's client does not see what the application decides. */
    private Action visible(Caller caller, String appId, String userId, String actionId) {
        return actions.find(appId, userId, actionId)
                .filter(action -> caller.kind() != Caller.Kind.USER || Actions.BY_USER.equals(action.approver()))
                .orElseThrow(() -> AgentException.notFound("no action " + actionId + " for this user"));
    }
}
