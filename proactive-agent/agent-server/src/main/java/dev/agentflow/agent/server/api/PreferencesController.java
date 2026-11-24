package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.PreferencesRequest;
import dev.agentflow.agent.api.PreferencesView;
import dev.agentflow.agent.server.app.Preferences;
import dev.agentflow.agent.server.auth.Caller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What a user wants of their agents: for now, how much they hear from their main agent without
 * having asked. The user sets it with their own client; the application's back end can set it for
 * them.
 */
@RestController
@RequestMapping("/v1/users/{userId}/preferences")
public class PreferencesController {

    private final Preferences preferences;

    public PreferencesController(Preferences preferences) {
        this.preferences = preferences;
    }

    @GetMapping
    public PreferencesView get(Caller caller, @PathVariable String userId) {
        return preferences.get(caller.requireAppOrUser(userId), userId);
    }

    /** The whole of the user's settings each time: what is left out goes back to the agent's default. */
    @PutMapping
    public PreferencesView set(Caller caller, @PathVariable String userId, @RequestBody(required = false) PreferencesRequest request) {
        String appId = caller.requireAppOrUser(userId);
        if (request == null) {
            throw AgentException.invalid("a request body is required");
        }
        return preferences.set(appId, userId, request.proactivity(),
                caller.kind() == Caller.Kind.USER ? Preferences.BY_USER : Preferences.BY_APP);
    }
}
