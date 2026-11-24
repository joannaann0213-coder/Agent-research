package dev.agentflow.agent.server.chat;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.profile.AgentProfiles;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Decides on which line of work something that the application has for an agent is handled.
 *
 * <p>For an ordinary agent it is the line of the subject: one session per subject, which remembers
 * the subject's earlier events. For a main agent it is the line of the conversation that the agent
 * serves: whatever happens for the user reaches the one session that also talks to them, so the
 * main agent knows of all of it, and decides itself what to hand to a worker.
 *
 * <p>In a conversation that a main agent serves, nothing works for the user that the main agent
 * does not know of. What the application has there for another agent is therefore not a line of its
 * own either: it is a task of the main agent, with that agent as its worker ({@link Hub}).
 */
@Service
public class Lines {

    private final Threads threads;
    private final AgentProfiles profiles;

    public Lines(Threads threads, AgentProfiles profiles) {
        this.threads = threads;
        this.profiles = profiles;
    }

    /** Where something of the application's goes. */
    public sealed interface Route {
    }

    /** It is handled on this line, by the line's agent. */
    public record Direct(Threads.Thread line) implements Route {
    }

    /**
     * The conversation is served by a main agent. What is for the main agent itself is handled on
     * its line; what is for another agent becomes a task of the main agent.
     *
     * @param line the main agent's line
     */
    public record Hub(Threads.Thread line) implements Route {
    }

    /**
     * Where an event of the application goes. A conversation that a main agent serves, or would
     * serve because nobody has written to it yet and the application's default agent is a main
     * agent, is started if need be. In the caller's transaction, under the user's lock.
     */
    public Route route(String appId, String userId, String subject, String agentKey, String conversationId, String keeps) {
        Optional<Threads.Thread> conversation = threads.find(appId, userId, Threads.chatKey(conversationId));
        String server = conversation.map(Threads.Thread::agentKey).orElse(Messages.DEFAULT_AGENT);
        boolean mainServes = profiles.configIfThere(appId, server).map(config -> config.coordinator() != null).orElse(false);
        if (!mainServes) {
            return new Direct(background(appId, userId, subject, agentKey, conversationId, keeps));
        }
        return new Hub(conversation.orElseGet(() -> threads.ensureChat(appId, userId, conversationId, server)));
    }

    /**
     * The line for an event or a schedule of the application, started if this is the first use of
     * it. In the caller's transaction, under the user's lock.
     *
     * @param agentKey the agent the caller named, or {@code null}
     * @param conversationId the conversation in which the user is told, which for a main agent is
     *     also where it works
     * @param keeps how the refusal ends when the subject already has another agent than the one named
     */
    public Threads.Thread background(String appId, String userId, String subject, String agentKey, String conversationId,
            String keeps) {
        Optional<Threads.Thread> ofSubject = threads.find(appId, userId, Threads.taskKey(subject));
        if (ofSubject.isPresent() && agentKey != null && !ofSubject.get().agentKey().equals(agentKey)) {
            throw AgentException.conflict("subject " + subject + " is handled by agent " + ofSubject.get().agentKey() + keeps);
        }
        String agent = ofSubject.map(Threads.Thread::agentKey).orElse(agentKey == null ? Messages.DEFAULT_AGENT : agentKey);
        Optional<AgentProfileRequest> config = profiles.configIfThere(appId, agent);
        if (config.isEmpty()) {
            throw AgentException.invalid("this application has no agent " + agent + "; create it with PUT /v1/agents/" + agent);
        }
        if (config.get().coordinator() == null) {
            return ofSubject.orElseGet(() -> threads.ensureTask(appId, userId, subject, agent));
        }
        Optional<Threads.Thread> conversation = threads.find(appId, userId, Threads.chatKey(conversationId));
        if (conversation.isPresent() && !conversation.get().agentKey().equals(agent)) {
            throw AgentException.conflict("conversation " + conversationId + " is served by agent "
                    + conversation.get().agentKey() + "; what is for the main agent " + agent
                    + " goes to a conversation that it serves");
        }
        return conversation.orElseGet(() -> threads.ensureChat(appId, userId, conversationId, agent));
    }
}
