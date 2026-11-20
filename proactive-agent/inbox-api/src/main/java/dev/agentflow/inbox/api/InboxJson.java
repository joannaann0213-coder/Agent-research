package dev.agentflow.inbox.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON conventions of the wire protocol: snake_case property names, ISO-8601 instants, absent
 * instead of null. Clients tolerate unknown properties so that the server can add fields.
 */
public final class InboxJson {

    private static final JsonMapper CLIENT = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private InboxJson() {
    }

    /** Shared, thread-safe mapper for clients. */
    public static JsonMapper mapper() {
        return CLIENT;
    }
}
