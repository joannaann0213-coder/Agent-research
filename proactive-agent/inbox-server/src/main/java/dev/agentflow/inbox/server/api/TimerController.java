package dev.agentflow.inbox.server.api;

import dev.agentflow.inbox.api.CreateTimerRequest;
import dev.agentflow.inbox.api.TimerView;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.auth.Role;
import dev.agentflow.inbox.server.timer.TimerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Timers: one-off and cron. A due timer writes a {@code timer.due} event into the inbox. */
@RestController
@RequestMapping("/v1/inbox/{inboxId}/timers")
public class TimerController {

    private final TimerService timers;

    public TimerController(TimerService timers) {
        this.timers = timers;
    }

    /** 201 for a new timer; 200 when the {@code timer_key} was already scheduled. */
    @PostMapping
    public ResponseEntity<TimerView> create(AuthenticatedClient client, @PathVariable String inboxId,
            @RequestParam(name = "tenant_id", required = false) String tenantId,
            @RequestBody CreateTimerRequest request) {
        if (!client.has(Role.PRODUCER)) {
            throw InboxException.forbidden("only a producer may create timers");
        }
        TimerService.Created result = timers.create(client, Tenants.resolve(client, tenantId), inboxId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.timer());
    }

    @GetMapping("/{timerId}")
    public TimerView get(AuthenticatedClient client, @PathVariable String inboxId, @PathVariable String timerId,
            @RequestParam(name = "tenant_id", required = false) String tenantId) {
        // A producer sees the timers it created; an operator sees every timer of the tenant.
        String onlyClient = client.has(Role.OPERATOR) ? null : client.clientId();
        return timers.get(Tenants.resolve(client, tenantId), inboxId, timerId, onlyClient);
    }

    /** A producer cancels its own timers. An operator may cancel any timer; that is written to the audit log. */
    @DeleteMapping("/{timerId}")
    public ResponseEntity<Void> cancel(AuthenticatedClient client, @PathVariable String inboxId,
            @PathVariable String timerId, @RequestParam(name = "tenant_id", required = false) String tenantId) {
        timers.cancel(client, Tenants.resolve(client, tenantId), inboxId, timerId);
        return ResponseEntity.noContent().build();
    }
}
