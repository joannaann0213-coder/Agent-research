package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.WebhookRequest;
import dev.agentflow.agent.api.WebhookView;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.delivery.Webhooks;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The application's webhook. Only its back end may see or change it. */
@RestController
@RequestMapping("/v1/webhook")
public class WebhookController {

    private final Webhooks webhooks;

    public WebhookController(Webhooks webhooks) {
        this.webhooks = webhooks;
    }

    @PutMapping
    public WebhookView put(Caller caller, @RequestBody WebhookRequest request) {
        return webhooks.put(caller.requireApp(), request);
    }

    @GetMapping
    public WebhookView get(Caller caller) {
        return webhooks.get(caller.requireApp())
                .orElseThrow(() -> AgentException.notFound("this application has no webhook; set one up with PUT /v1/webhook"));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(Caller caller) {
        webhooks.delete(caller.requireApp());
        return ResponseEntity.noContent().build();
    }
}
