package dev.agentflow.agent.server.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * The mapper for JSON this service stores for itself, in jsonb columns: snake_case, absent instead
 * of null, and tolerant of fields it does not know, so that a row written by a newer version can be
 * read by an older one during a rollout.
 */
public final class Json {

    public static final JsonMapper STORED = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private Json() {
    }
}
