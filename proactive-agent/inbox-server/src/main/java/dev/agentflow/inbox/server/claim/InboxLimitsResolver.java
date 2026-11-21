package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.server.config.InboxProperties;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    /** Keys a lane may carry in the settings, and the keys of the batch caps. */
    private static final Set<String> LANE_KEYS = Set.of("quiet_window_ms", "max_wait_ms");
    private static final Set<String> BATCH_KEYS = Set.of("max_events", "max_bytes");
    /** Longest quiet window or maximum wait an inbox may set: one day. */
    static final long MAX_WINDOW_MS = Duration.ofDays(1).toMillis();

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

    /**
     * Refuses settings with a key this service does not read, or a value it cannot use. The settings
     * are free-form JSON on the wire, so without this check a typo such as {@code quiet_windw_ms}
     * would be stored and silently do nothing.
     *
     * @throws IllegalArgumentException naming the first offending key
     */
    public void validate(JsonNode settings) {
        requireObject("settings", settings);
        for (String key : settings.propertyNames()) {
            JsonNode value = settings.get(key);
            switch (key) {
                case "lanes" -> {
                    requireObject("lanes", value);
                    for (String laneName : value.propertyNames()) {
                        String path = "lanes." + laneName;
                        if (!isLane(laneName)) {
                            throw new IllegalArgumentException("unknown lane '" + path + "'");
                        }
                        JsonNode lane = value.get(laneName);
                        requireObject(path, lane);
                        for (String field : lane.propertyNames()) {
                            requireKnown(path, field, LANE_KEYS);
                            requireInteger(path + "." + field, lane.get(field), 0, MAX_WINDOW_MS);
                        }
                    }
                }
                case "batch" -> {
                    requireObject("batch", value);
                    for (String field : value.propertyNames()) {
                        requireKnown("batch", field, BATCH_KEYS);
                        requireInteger("batch." + field, value.get(field), 1, Integer.MAX_VALUE);
                    }
                }
                default -> throw new IllegalArgumentException(
                        "unknown key '" + key + "': settings take 'lanes' and 'batch'");
            }
        }
    }

    private static boolean isLane(String name) {
        for (Lane lane : Lane.values()) {
            if (lane.wire().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void requireObject(String path, JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("'" + path + "' must be a JSON object");
        }
    }

    private static void requireKnown(String path, String field, Set<String> known) {
        if (!known.contains(field)) {
            throw new IllegalArgumentException("unknown key '" + path + "." + field + "': the keys there are "
                    + String.join(" and ", new TreeSet<>(known)));
        }
    }

    private static void requireInteger(String path, JsonNode node, long min, long max) {
        if (!node.isIntegralNumber() || !node.canConvertToLong() || node.longValue() < min || node.longValue() > max) {
            throw new IllegalArgumentException("'" + path + "' must be an integer from " + min + " to " + max);
        }
    }

    private static Duration millis(JsonNode node, Duration fallback) {
        return node.isNumber() ? Duration.ofMillis(node.longValue()) : fallback;
    }
}
