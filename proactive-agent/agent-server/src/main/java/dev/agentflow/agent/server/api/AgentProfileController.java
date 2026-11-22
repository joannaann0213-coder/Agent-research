package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.profile.AgentProfiles;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The agents of an application: what each one is for, which model it runs on, what it may use. */
@RestController
@RequestMapping("/v1/agents")
public class AgentProfileController {

    private final AgentProfiles profiles;

    public AgentProfileController(AgentProfiles profiles) {
        this.profiles = profiles;
    }

    /** 201 for a new agent, 200 when it existed. Sending the same configuration again changes nothing. */
    @PutMapping("/{agentKey}")
    public ResponseEntity<AgentProfileView> save(Caller caller, @PathVariable String agentKey,
            @RequestBody AgentProfileRequest request) {
        AgentProfiles.Saved saved = profiles.save(caller.requireApp(), agentKey, request);
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.profile());
    }

    @GetMapping("/{agentKey}")
    public AgentProfileView get(Caller caller, @PathVariable String agentKey) {
        return profiles.get(caller.requireApp(), agentKey);
    }

    @GetMapping
    public List<AgentProfileView> list(Caller caller) {
        return profiles.list(caller.requireApp());
    }
}
