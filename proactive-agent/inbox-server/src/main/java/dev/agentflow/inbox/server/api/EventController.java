package dev.agentflow.inbox.server.api;

import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.auth.Role;
import dev.agentflow.inbox.server.query.EventQueries;
import dev.agentflow.inbox.server.write.EventWriter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Producer-facing API: append an event, read one back with its history. */
@RestController
@RequestMapping("/v1/inbox/{inboxId}/events")
public class EventController {

    private final EventWriter writer;
    private final EventQueries queries;

    public EventController(EventWriter writer, EventQueries queries) {
        this.writer = writer;
        this.queries = queries;
    }

    /** 202 means "accepted", not "processed": the event is durable and will be delivered at least once. */
    @PostMapping
    public ResponseEntity<WriteEventResponse> write(AuthenticatedClient client, @PathVariable String inboxId,
            @RequestParam(name = "tenant_id", required = false) String tenantId,
            @RequestBody WriteEventRequest request) {
        if (!client.has(Role.PRODUCER)) {
            throw InboxException.forbidden("only a producer may write events");
        }
        return ResponseEntity.accepted().body(writer.write(client, Tenants.resolve(client, tenantId), inboxId, request));
    }

    @GetMapping("/{eventId}")
    public EventDetail get(AuthenticatedClient client, @PathVariable String inboxId, @PathVariable String eventId,
            @RequestParam(name = "tenant_id", required = false) String tenantId) {
        // A producer sees the events it wrote; an operator sees every event of the tenant.
        String onlyClient = client.has(Role.OPERATOR) ? null : client.clientId();
        return queries.detail(Tenants.resolve(client, tenantId), inboxId, eventId, onlyClient);
    }
}
