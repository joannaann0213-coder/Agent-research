package dev.agentflow.inbox.server.api;

import dev.agentflow.inbox.api.AuditEntry;
import dev.agentflow.inbox.api.DeadLetterRequest;
import dev.agentflow.inbox.api.InboxConfigRequest;
import dev.agentflow.inbox.api.InboxStatus;
import dev.agentflow.inbox.api.ReplayRequest;
import dev.agentflow.inbox.api.ReplayResponse;
import dev.agentflow.inbox.server.admin.AuditLog;
import dev.agentflow.inbox.server.admin.InboxAdminService;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.replay.ReplayService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operator API. Everything here that changes state is written to the audit log. */
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final ReplayService replay;
    private final InboxAdminService inboxes;
    private final AuditLog audit;

    public AdminController(ReplayService replay, InboxAdminService inboxes, AuditLog audit) {
        this.replay = replay;
        this.inboxes = inboxes;
        this.audit = audit;
    }

    @PostMapping("/replay")
    public ReplayResponse replay(AuthenticatedClient client, @RequestBody ReplayRequest request) {
        return replay.replay(client, Tenants.resolve(client, request.tenantId()), request);
    }

    @PostMapping("/events/{eventId}/dead")
    public ResponseEntity<Void> deadLetter(AuthenticatedClient client, @PathVariable String eventId,
            @RequestBody DeadLetterRequest request) {
        replay.deadLetter(client, Tenants.resolve(client, request.tenantId()), eventId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/inboxes/{inboxId}/config")
    public InboxStatus configure(AuthenticatedClient client, @PathVariable String inboxId,
            @RequestBody InboxConfigRequest request) {
        return inboxes.configure(client, Tenants.resolve(client, request.tenantId()), inboxId, request);
    }

    @GetMapping("/inboxes/{inboxId}")
    public InboxStatus status(AuthenticatedClient client, @PathVariable String inboxId,
            @RequestParam(name = "tenant_id", required = false) String tenantId) {
        return inboxes.status(Tenants.resolve(client, tenantId), inboxId);
    }

    @GetMapping("/audit")
    public List<AuditEntry> audit(AuthenticatedClient client,
            @RequestParam(name = "tenant_id", required = false) String tenantId,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return audit.list(Tenants.filter(client, tenantId), Math.max(1, Math.min(limit, 1000)));
    }
}
