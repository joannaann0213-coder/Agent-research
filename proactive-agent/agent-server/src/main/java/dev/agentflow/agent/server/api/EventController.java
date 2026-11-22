package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.events.AppEvents;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the application's own systems tell the agent about a user. Only the application's back end
 * may call this: the agent takes an event as the application's word, which a user's client cannot
 * give.
 */
@RestController
@RequestMapping("/v1/users/{userId}/events")
public class EventController {

    private final AppEvents events;

    public EventController(AppEvents events) {
        this.events = events;
    }

    /** 202 means "stored and on its way to the agent". Whether the user hears about it is the agent's decision. */
    @PostMapping
    public ResponseEntity<EventAccepted> post(Caller caller, @PathVariable String userId, @RequestBody PostEventRequest request) {
        return ResponseEntity.accepted().body(events.post(caller.requireApp(), userId, request));
    }
}
