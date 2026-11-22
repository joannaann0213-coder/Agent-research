package dev.agentflow.inbox.server.docs;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.EventStatus;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.ReplayResponse;
import dev.agentflow.inbox.api.RunStatus;
import dev.agentflow.inbox.server.api.OpenApiController;
import dev.agentflow.testkit.openapi.OpenApiLint;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The schemas of the API specification against the wire types in {@code inbox-api}.
 *
 * <p>The integration tests check real traffic against the specification, which covers types, required
 * fields and status codes, but only for what the tests happen to send and receive. This test covers
 * the inventory: every wire type has a schema, every schema lists exactly the fields of its type, and
 * every enum lists exactly the values the code knows. It also checks the specification itself: that
 * it is sound, and that its examples match their own schemas.
 */
class OpenApiSchemaTest {

    private static final String API_PACKAGE = "dev.agentflow.inbox.api";

    /** Schemas that describe JSON the service stores as it is given, not a wire type of their own. */
    private static final Set<String> WITHOUT_A_TYPE = Set.of("InboxSettings", "LaneWindows");

    private static String specification;

    @SuppressWarnings("rawtypes")
    private static Map<String, Schema> schemas;

    @BeforeAll
    static void parse() throws Exception {
        specification = new ClassPathResource(OpenApiController.SPECIFICATION).getContentAsString(StandardCharsets.UTF_8);
        schemas = new OpenAPIV3Parser().readContents(specification).getOpenAPI().getComponents().getSchemas();
    }

    @Test
    void theSpecificationIsSound_andItsExamplesMatchTheirSchemas() {
        OpenApiLint.assertClean(OpenApiController.SPECIFICATION, specification);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void everyObjectSchemaListsExactlyTheFieldsOfItsWireType() throws Exception {
        for (var entry : schemas.entrySet()) {
            if (entry.getValue().getProperties() == null || WITHOUT_A_TYPE.contains(entry.getKey())) {
                continue;
            }
            Class<?> type = Class.forName(API_PACKAGE + "." + entry.getKey());
            assertThat(type.isRecord()).as("%s is a record", type).isTrue();
            assertThat(new TreeSet<>(propertiesOf(entry.getValue()))).as("properties of schema %s", entry.getKey())
                    .isEqualTo(fieldsOf(type));
        }
        assertThat(new TreeSet<>(propertiesOf(property(schemas.get("ReplayResponse"), "skipped").getItems())))
                .as("items of ReplayResponse.skipped").isEqualTo(fieldsOf(ReplayResponse.Skipped.class));
    }

    @Test
    void everyWireTypeHasASchema() throws Exception {
        List<String> records = new ArrayList<>();
        for (Resource resource : new PathMatchingResourcePatternResolver()
                .getResources("classpath*:" + API_PACKAGE.replace('.', '/') + "/*.class")) {
            String simpleName = resource.getFilename().replace(".class", "");
            if (!simpleName.contains("$") && Class.forName(API_PACKAGE + "." + simpleName).isRecord()) {
                records.add(simpleName);
            }
        }

        assertThat(records).as("the wire types were found on the classpath").hasSizeGreaterThan(20);
        assertThat(schemas.keySet()).containsAll(records);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void everyEnumListsExactlyTheValuesTheCodeKnows() throws Exception {
        assertThat(valuesOf(schemas.get("Lane"))).containsExactlyInAnyOrderElementsOf(wire(Lane.values()));
        assertThat(valuesOf(schemas.get("EventSource"))).containsExactlyInAnyOrderElementsOf(wire(EventSource.values()));
        assertThat(valuesOf(schemas.get("EventStatus"))).containsExactlyInAnyOrderElementsOf(wire(EventStatus.values()));
        assertThat(valuesOf(schemas.get("RunStatus"))).containsExactlyInAnyOrderElementsOf(wire(RunStatus.values()));

        // A deferred event that has not fired yet has no row; the API reports it as "scheduled".
        List<String> reported = new ArrayList<>(wire(EventStatus.values()));
        reported.add(EventStatus.SCHEDULED);
        assertThat(valuesOf(property(schemas.get("EventView"), "status")))
                .containsExactlyInAnyOrderElementsOf(reported);

        List<String> codes = new ArrayList<>();
        for (Field field : ProblemCodes.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                codes.add((String) field.get(null));
            }
        }
        assertThat(valuesOf(property(schemas.get("Problem"), "code"))).containsExactlyInAnyOrderElementsOf(codes);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Set<String> propertiesOf(Schema schema) {
        return ((Map<String, Schema>) schema.getProperties()).keySet();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema property(Schema schema, String name) {
        return ((Map<String, Schema>) schema.getProperties()).get(name);
    }

    /** The JSON names of a record's fields: the wire protocol is snake_case throughout. */
    private static Set<String> fieldsOf(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(component -> component.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("rawtypes")
    private static List<String> valuesOf(Schema schema) {
        return ((List<?>) schema.getEnum()).stream().map(Object::toString).toList();
    }

    private static List<String> wire(Enum<?>[] constants) throws Exception {
        List<String> values = new ArrayList<>();
        for (Enum<?> constant : constants) {
            values.add((String) constant.getClass().getMethod("wire").invoke(constant));
        }
        return values;
    }
}
