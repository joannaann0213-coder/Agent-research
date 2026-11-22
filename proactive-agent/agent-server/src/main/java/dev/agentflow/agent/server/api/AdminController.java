package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.AppView;
import dev.agentflow.agent.api.CreateAppRequest;
import dev.agentflow.agent.api.CreateKeyRequest;
import dev.agentflow.agent.api.CreatedKey;
import dev.agentflow.agent.api.KeyView;
import dev.agentflow.agent.api.UpdateAppRequest;
import dev.agentflow.agent.server.app.Apps;
import dev.agentflow.agent.server.auth.ApiKeys;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operator API: the applications that may use the service, and their API keys. */
@RestController
@RequestMapping("/v1/admin/apps")
public class AdminController {

    private final Apps apps;
    private final ApiKeys keys;

    public AdminController(Apps apps, ApiKeys keys) {
        this.apps = apps;
        this.keys = keys;
    }

    @PostMapping
    public ResponseEntity<AppView> create(@RequestBody CreateAppRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apps.create(request));
    }

    @GetMapping
    public List<AppView> list() {
        return apps.list();
    }

    @GetMapping("/{appId}")
    public AppView get(@PathVariable String appId) {
        return apps.get(appId);
    }

    @PatchMapping("/{appId}")
    public AppView update(@PathVariable String appId, @RequestBody UpdateAppRequest request) {
        return apps.update(appId, request);
    }

    /** The key is in the response and nowhere else afterwards. */
    @PostMapping("/{appId}/keys")
    public ResponseEntity<CreatedKey> createKey(@PathVariable String appId, @RequestBody CreateKeyRequest request) {
        apps.get(appId);
        return ResponseEntity.status(HttpStatus.CREATED).body(keys.create(appId, request.label()));
    }

    @GetMapping("/{appId}/keys")
    public List<KeyView> listKeys(@PathVariable String appId) {
        apps.get(appId);
        return keys.list(appId);
    }

    @DeleteMapping("/{appId}/keys/{keyId}")
    public ResponseEntity<Void> revokeKey(@PathVariable String appId, @PathVariable String keyId) {
        if (!keys.revoke(appId, keyId)) {
            throw AgentException.notFound("application " + appId + " has no key " + keyId);
        }
        return ResponseEntity.noContent().build();
    }
}
