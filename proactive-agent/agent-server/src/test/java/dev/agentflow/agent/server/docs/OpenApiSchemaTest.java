package dev.agentflow.agent.server.docs;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.server.api.OpenApiController;
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
 * The schemas of the API specification against the wire types in {@code agent-api}.
 *
 * <p>The integration tests check real traffic against the specification, which covers types, required
 * fields and status codes, but only for what the tests happen to send and receive. This test covers
 * the inventory: every wire type has a schema, every schema lists exactly the fields of its type, and
 * the error codes are the ones the code knows. It also checks the specification itself: that it is
 * sound, and that its examples match their own schemas.
 */
class OpenApiSchemaTest {

    private static final String API_PACKAGE = "dev.agentflow.agent.api";

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    void everyObjectSchemaListsExactlyTheFieldsOfItsWireType() throws Exception {
        for (var entry : schemas.entrySet()) {
            if (entry.getValue().getProperties() == null) {
                continue;
            }
            Class<?> type = Class.forName(API_PACKAGE + "." + entry.getKey());
            assertThat(type.isRecord()).as("%s is a record", type).isTrue();
            assertThat(new TreeSet<>(((Map<String, Schema>) entry.getValue().getProperties()).keySet()))
                    .as("properties of schema %s", entry.getKey()).isEqualTo(fieldsOf(type));
        }
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

        assertThat(records).as("the wire types were found on the classpath").hasSizeGreaterThan(5);
        assertThat(schemas.keySet()).containsAll(records);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theErrorCodesAreTheOnesTheCodeKnows() throws Exception {
        List<String> codes = new ArrayList<>();
        for (Field field : ProblemCodes.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                codes.add((String) field.get(null));
            }
        }
        Schema code = ((Map<String, Schema>) schemas.get("Problem").getProperties()).get("code");
        assertThat(((List<?>) code.getEnum()).stream().map(Object::toString).toList()).containsExactlyInAnyOrderElementsOf(codes);
    }

    /** The JSON names of a record's fields: the wire protocol is snake_case throughout. */
    private static Set<String> fieldsOf(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(component -> component.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
