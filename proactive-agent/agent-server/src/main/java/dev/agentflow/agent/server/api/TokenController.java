package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.IssueTokenRequest;
import dev.agentflow.agent.api.IssuedToken;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.auth.UserTokens;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tokens that let an end user's client call the Agent API itself. Only the application's back end
 * may issue or revoke them: it is the one that knows who the user is.
 */
@RestController
@RequestMapping("/v1/users/{userId}/tokens")
public class TokenController {

    private final UserTokens tokens;

    public TokenController(UserTokens tokens) {
        this.tokens = tokens;
    }

    @PostMapping
    public ResponseEntity<IssuedToken> issue(Caller caller, @PathVariable String userId,
            @RequestBody(required = false) IssueTokenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tokens.issue(caller.requireApp(), userId, request == null ? null : request.ttlSeconds()));
    }

    /** Revokes every token of the user that is out, for example when the user signs out everywhere. */
    @DeleteMapping
    public ResponseEntity<Void> revokeAll(Caller caller, @PathVariable String userId) {
        tokens.revokeAll(caller.requireApp(), userId);
        return ResponseEntity.noContent().build();
    }
}
