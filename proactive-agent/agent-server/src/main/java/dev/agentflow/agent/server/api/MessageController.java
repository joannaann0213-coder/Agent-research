package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessagePage;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.chat.Messages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A user's conversation with the agent. The application's back end may call this, and so may the user's own client. */
@RestController
@RequestMapping("/v1/users/{userId}/messages")
public class MessageController {

    private final Messages messages;

    public MessageController(Messages messages) {
        this.messages = messages;
    }

    /** 202 means "stored and on its way to the agent", not "answered". The answer shows up among the messages. */
    @PostMapping
    public ResponseEntity<MessageAccepted> post(Caller caller, @PathVariable String userId,
            @RequestBody PostMessageRequest request) {
        return ResponseEntity.accepted().body(messages.post(caller.requireAppOrUser(userId), userId, request));
    }

    @GetMapping
    public MessagePage list(Caller caller, @PathVariable String userId,
            @RequestParam(name = "conversation_id", required = false) String conversationId,
            @RequestParam(name = "after", defaultValue = "0") long after,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return messages.list(caller.requireAppOrUser(userId), userId, conversationId, Math.max(0, after),
                Math.max(1, Math.min(limit, 200)));
    }
}
