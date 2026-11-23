package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.GrantPage;
import dev.agentflow.agent.api.GrantView;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.tasks.Grants;
import dev.agentflow.agent.server.tasks.Tasks;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A user's standing authorisations: which roles their main agent may start tasks for on its own
 * initiative, without asking each time. The user gives one and takes it back, with their own
 * client; the application's back end can do both for them. An agent cannot.
 */
@RestController
@RequestMapping("/v1/users/{userId}/grants")
public class GrantController {

    private final Grants grants;

    public GrantController(Grants grants) {
        this.grants = grants;
    }

    @GetMapping
    public GrantPage list(Caller caller, @PathVariable String userId) {
        return grants.list(caller.requireAppOrUser(userId), userId);
    }

    /** Giving an authorisation that is there already changes nothing. */
    @PutMapping("/{agentKey}/{role}")
    public GrantView grant(Caller caller, @PathVariable String userId, @PathVariable String agentKey, @PathVariable String role) {
        String appId = caller.requireAppOrUser(userId);
        return grants.grant(appId, userId, agentKey, role, caller.kind() == Caller.Kind.USER ? Tasks.BY_USER : Tasks.BY_APP);
    }

    /** Taking back an authorisation that is not there changes nothing. */
    @DeleteMapping("/{agentKey}/{role}")
    public ResponseEntity<Void> revoke(Caller caller, @PathVariable String userId, @PathVariable String agentKey,
            @PathVariable String role) {
        grants.revoke(caller.requireAppOrUser(userId), userId, agentKey, role);
        return ResponseEntity.noContent().build();
    }
}
