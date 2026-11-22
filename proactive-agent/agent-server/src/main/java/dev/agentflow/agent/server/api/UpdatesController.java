package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.StreamTicket;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.UpdatePage;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.delivery.Outbound;
import dev.agentflow.agent.server.delivery.StreamHub;
import dev.agentflow.agent.server.delivery.StreamTickets;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** What happened for a user, as a page to poll or as a live stream. */
@RestController
@RequestMapping("/v1/users/{userId}")
public class UpdatesController {

    private static final Duration LOOK_AGAIN = Duration.ofSeconds(1);

    private final Outbound outbound;
    private final StreamHub hub;
    private final StreamTickets tickets;
    private final AgentProperties.Stream config;

    public UpdatesController(Outbound outbound, StreamHub hub, StreamTickets tickets, AgentProperties properties) {
        this.outbound = outbound;
        this.hub = hub;
        this.tickets = tickets;
        this.config = properties.getStream();
    }

    @GetMapping("/updates")
    public UpdatePage updates(Caller caller, @PathVariable String userId,
            @RequestParam(name = "after", defaultValue = "0") long after,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        String appId = caller.requireAppOrUser(userId);
        Names.userId(userId);
        List<Update> updates = outbound.after(appId, userId, Math.max(0, after), Math.max(1, Math.min(limit, 500)));
        return new UpdatePage(updates, updates.isEmpty() ? Math.max(0, after) : updates.getLast().seq());
    }

    @PostMapping("/stream-tickets")
    public ResponseEntity<StreamTicket> ticket(Caller caller, @PathVariable String userId) {
        String appId = caller.requireAppOrUser(userId);
        Names.userId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets.issue(appId, userId));
    }

    /**
     * The user's updates as server-sent events: each one with its {@code seq} as the event's id and
     * its type as the event's name. It starts after {@code Last-Event-ID}, else after {@code after},
     * else with what happens from now on. A connection ends after a while; the client connects again
     * with the id of the last event it got, and misses nothing.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Caller caller, @PathVariable String userId,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId,
            @RequestParam(name = "after", required = false) Long after) {
        String appId = caller.requireAppOrUser(userId);
        Names.userId(userId);
        long from;
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                from = Long.parseLong(lastEventId.trim());
            } catch (NumberFormatException e) {
                throw AgentException.invalid("Last-Event-ID is the id of an event of this stream: a number");
            }
        } else {
            from = after != null ? Math.max(0, after) : outbound.last(appId, userId);
        }
        SseEmitter emitter = new SseEmitter(config.getMaxDuration().toMillis() + 10_000);
        AtomicBoolean open = new AtomicBoolean(true);
        emitter.onCompletion(() -> open.set(false));
        emitter.onTimeout(() -> open.set(false));
        emitter.onError(error -> open.set(false));
        Thread.ofVirtual().name("stream-" + appId + "-" + userId).start(() -> pump(emitter, open, appId, userId, from));
        return emitter;
    }

    private void pump(SseEmitter emitter, AtomicBoolean open, String appId, String userId, long from) {
        long cursor = from;
        long endAt = System.nanoTime() + config.getMaxDuration().toNanos();
        long beatAt = System.nanoTime() + config.getHeartbeat().toNanos();
        try {
            emitter.send(SseEmitter.event().reconnectTime(3000).comment("stream of " + userId + " after " + from));
            while (open.get() && System.nanoTime() < endAt) {
                long version = hub.version(appId, userId);
                List<Update> updates = outbound.after(appId, userId, cursor, 100);
                for (Update update : updates) {
                    emitter.send(SseEmitter.event().id(Long.toString(update.seq())).name(update.type())
                            .data(AgentJson.mapper().writeValueAsString(update), MediaType.APPLICATION_JSON));
                    cursor = update.seq();
                }
                if (System.nanoTime() > beatAt) {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                    beatAt = System.nanoTime() + config.getHeartbeat().toNanos();
                }
                if (updates.isEmpty()) {
                    hub.await(appId, userId, version, LOOK_AGAIN);
                }
            }
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            // The client went away.
            emitter.completeWithError(e);
        }
    }
}
