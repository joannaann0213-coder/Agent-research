package dev.agentflow.agent.server.delivery;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sends webhook deliveries as they fall due. */
@Component
public class DeliveryLoop {

    private final WebhookDispatcher dispatcher;

    public DeliveryLoop(WebhookDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${agent.webhooks.interval:1s}")
    void tick() {
        dispatcher.runOnce();
    }
}
