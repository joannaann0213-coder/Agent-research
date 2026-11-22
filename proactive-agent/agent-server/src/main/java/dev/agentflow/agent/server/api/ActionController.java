package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.ActionView;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.server.actions.ActionDesk;
import dev.agentflow.agent.server.auth.Caller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** What the agent waits for people to decide: approvals of tool calls, and questions to the user. */
@RestController
@RequestMapping("/v1/users/{userId}/actions")
public class ActionController {

    private final ActionDesk desk;

    public ActionController(ActionDesk desk) {
        this.desk = desk;
    }

    @GetMapping
    public ActionPage pending(Caller caller, @PathVariable String userId,
            @RequestParam(name = "conversation_id", required = false) String conversationId) {
        return desk.pending(caller, userId, conversationId);
    }

    @GetMapping("/{actionId}")
    public ActionView get(Caller caller, @PathVariable String userId, @PathVariable String actionId) {
        return desk.get(caller, userId, actionId);
    }

    @PostMapping("/{actionId}/resolve")
    public ActionView resolve(Caller caller, @PathVariable String userId, @PathVariable String actionId,
            @RequestBody ResolveActionRequest request) {
        return desk.resolve(caller, userId, actionId, request);
    }
}
