package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.StopResult;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.chat.Conversations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A conversation as a whole. */
@RestController
@RequestMapping("/v1/users/{userId}/conversations/{conversationId}")
public class ConversationController {

    private final Conversations conversations;

    public ConversationController(Conversations conversations) {
        this.conversations = conversations;
    }

    /** 202: the stop is on record; the turn under way, if any, stops a moment later. */
    @PostMapping("/stop")
    public ResponseEntity<StopResult> stop(Caller caller, @PathVariable String userId, @PathVariable String conversationId) {
        return ResponseEntity.accepted().body(conversations.stop(caller.requireAppOrUser(userId), userId, conversationId));
    }
}
