package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.McpCredentialPage;
import dev.agentflow.agent.api.McpCredentialRequest;
import dev.agentflow.agent.api.McpCredentialView;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.credentials.Credentials;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credentials of MCP servers: the application's, set by its back end, and each user's own, set by
 * the back end or by the user's client (to connect the user's own account).
 */
@RestController
public class CredentialController {

    private final Credentials credentials;

    public CredentialController(Credentials credentials) {
        this.credentials = credentials;
    }

    @PutMapping("/v1/credentials/{name}")
    public McpCredentialView putForApp(Caller caller, @PathVariable String name, @RequestBody McpCredentialRequest request) {
        return credentials.put(caller.requireApp(), null, name, request);
    }

    @GetMapping("/v1/credentials")
    public McpCredentialPage listForApp(Caller caller) {
        return new McpCredentialPage(credentials.list(caller.requireApp(), null));
    }

    @DeleteMapping("/v1/credentials/{name}")
    public ResponseEntity<Void> deleteForApp(Caller caller, @PathVariable String name) {
        credentials.delete(caller.requireApp(), null, name);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/v1/users/{userId}/credentials/{name}")
    public McpCredentialView putForUser(Caller caller, @PathVariable String userId, @PathVariable String name,
            @RequestBody McpCredentialRequest request) {
        return credentials.put(caller.requireAppOrUser(userId), userId, name, request);
    }

    @GetMapping("/v1/users/{userId}/credentials")
    public McpCredentialPage listForUser(Caller caller, @PathVariable String userId) {
        return new McpCredentialPage(credentials.list(caller.requireAppOrUser(userId), userId));
    }

    @DeleteMapping("/v1/users/{userId}/credentials/{name}")
    public ResponseEntity<Void> deleteForUser(Caller caller, @PathVariable String userId, @PathVariable String name) {
        credentials.delete(caller.requireAppOrUser(userId), userId, name);
        return ResponseEntity.noContent().build();
    }
}
