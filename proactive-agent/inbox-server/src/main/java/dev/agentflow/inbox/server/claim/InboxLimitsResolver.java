package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.server.config.InboxProperties;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Merges the service defaults with one inbox's overrides. The {@code settings} column looks like
 * <pre>{@code
 * {"lanes": {"background": {"quiet_window_ms": 30000, "max_wait_ms": 60000}},
 *  "batch": {"max_events": 20, "max_bytes": 262144}}
 * }</pre>
 */
@Component
public class InboxLimitsResolver {

    private final InboxProperties properties;
    private final JsonMapper json;

    public InboxLimitsResolver(InboxProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    public InboxLimits resolve(Integer capInteractive, Integer capBackground, Integer capBatch, Integer maxActiveRuns,
            String settingsJson) {
        JsonNode settings = settingsJson == null ? json.createObjectNode() : json.readTree(settingsJson);
        Map<Lane, Integer> caps = new EnumMap<>(Lane.class);
        Map<Lane, Duration> quiet = new EnumMap<>(Lane.class);
        Map<Lane, Duration> wait = new EnumMap<>(Lane.class);
        Integer[] capOverrides = {capInteractive, capBackground, capBatch};
        for (Lane lane : Lane.values()) {
            InboxProperties.LaneSettings defaults = properties.getLanes().of(lane);
            Integer cap = capOverrides[lane.ordinal()];
            caps.put(lane, cap != null ? cap : defaults.getCap());

            JsonNode laneNode = settings.path("lanes").path(lane.wire());
            Duration quietWindow = millis(laneNode.path("quiet_window_ms"), defaults.getQuietWindow());
            quiet.put(lane, quietWindow);
            Duration fallbackWait = laneNode.path("quiet_window_ms").isNumber()
                    ? quietWindow.multipliedBy(4)
                    : defaults.effectiveMaxWait();
            wait.put(lane, millis(laneNode.path("max_wait_ms"), fallbackWait));
        }
        JsonNode batch = settings.path("batch");
        int maxEvents = batch.path("max_events").isNumber()
                ? batch.path("max_events").intValue() : properties.getBatch().getMaxEvents();
        int maxBytes = batch.path("max_bytes").isNumber()
                ? batch.path("max_bytes").intValue() : properties.getBatch().getMaxBytes();
        return new InboxLimits(caps, maxActiveRuns, quiet, wait, Math.max(1, maxEvents), Math.max(1, maxBytes));
    }

    private static Duration millis(JsonNode node, Duration fallback) {
        return node.isNumber() ? Duration.ofMillis(node.longValue()) : fallback;
    }
}
