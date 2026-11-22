package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.agentflow.agent.server.api.OpenApiController;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.testkit.openapi.SpringMappings;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The published specification, {@code GET /openapi.yaml}, against the service.
 *
 * <p>Every integration test already checks its own requests and responses against the specification
 * (see {@link AgentTestBase}). This class adds what only shows at the end: the specification and the
 * controllers list the same operations, and the suite as a whole has exercised every operation and
 * every status the specification lists. It therefore runs last.
 */
@Order(Integer.MAX_VALUE)
class OpenApiContractIT extends AgentTestBase {

    /** Statuses produced by layers that all operations share; one test per operation would add nothing. */
    /** Statuses that every operation shares and that a few tests show, rather than a test per operation. */
    private static final Set<Integer> SHARED = Set.of(400, 401, 403, 429);

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void theSpecificationAndTheControllersListTheSameOperations() {
        OPENAPI.assertSameOperationsAs(SpringMappings.under("/v1/", mappings));
    }

    @Test
    void theSpecificationIsServedWithoutACredential() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(baseUri().resolve("/openapi.yaml")).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/yaml");
        assertThat(response.body())
                .startsWith("openapi: 3.0.3")
                .isEqualTo(new ClassPathResource(OpenApiController.SPECIFICATION).getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void theSuiteExercisesEveryOperationAndEveryStatusTheSpecificationLists() throws Exception {
        Files.writeString(Path.of("target", "openapi-coverage.txt"), OPENAPI.coverageReport());
        assumeTrue(OPENAPI.exchanges() > 0, "coverage is only meaningful after the other integration tests have run");

        OPENAPI.assertCovered(SHARED);
    }
}
