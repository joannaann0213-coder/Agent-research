package dev.agentflow.testkit.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenApiTrafficTest {

    private static final String THING = "{\"thing_id\":\"t1\",\"name\":\"lamp\",\"made_at\":\"2026-11-20T10:00:00Z\"}";

    private OpenApiTraffic traffic;

    @BeforeEach
    void load() {
        traffic = OpenApiTraffic.forSpec("things.yaml", read("things.yaml"));
    }

    @Test
    void anExchangeThatMatchesTheSpecificationIsQuiet() {
        traffic.record(json("POST", "/v1/things", null, "{\"name\":\"lamp\",\"size\":2}", 201, THING));
        traffic.record(json("GET", "/v1/things/t1", "view=full", null, 200, THING));
        traffic.record(problem("GET", "/v1/things/nope", null, 404, "{\"status\":404,\"code\":\"not_found\"}"));

        traffic.assertNoViolations();
        assertThat(traffic.exchanges()).isEqualTo(3);
    }

    @Test
    void aResponseFieldTheSchemaDoesNotListIsAViolation() {
        traffic.record(json("GET", "/v1/things/t1", null, null, 200, "{\"thing_id\":\"t1\",\"name\":\"lamp\",\"colour\":\"red\"}"));

        assertThat(traffic.drainViolations()).singleElement().asString()
                .contains("GET /v1/things/t1 -> 200", "[getThing]", "response:", "colour");
    }

    @Test
    void aResponseThatLacksARequiredFieldOrHasAWrongTypeIsAViolation() {
        traffic.record(json("GET", "/v1/things/t1", null, null, 200, "{\"thing_id\":\"t1\"}"));
        traffic.record(json("GET", "/v1/things/t2", null, null, 200, "{\"thing_id\":\"t2\",\"name\":\"lamp\",\"size\":\"big\"}"));
        traffic.record(json("GET", "/v1/things/t3", null, null, 200, "{\"thing_id\":\"t3\",\"name\":\"lamp\",\"made_at\":\"yesterday\"}"));

        List<String> violations = traffic.drainViolations();
        assertThat(violations).hasSize(3);
        assertThat(violations.get(0)).contains("name");
        assertThat(violations.get(1)).contains("size");
        assertThat(violations.get(2)).contains("made_at");
    }

    @Test
    void anErrorBodyIsCheckedToo() {
        traffic.record(problem("GET", "/v1/things/nope", null, 404, "{\"status\":404}"));

        assertThat(traffic.drainViolations()).singleElement().asString().contains("code");
    }

    @Test
    void aStatusTheOperationDoesNotListIsAViolation_evenWhenDefaultWouldCoverIt() {
        traffic.record(problem("GET", "/v1/things/t1", null, 409, "{\"status\":409,\"code\":\"conflict\"}"));

        assertThat(traffic.drainViolations()).singleElement().asString()
                .contains("status 409 is not listed for this operation");
    }

    @Test
    void contentNegotiationFailuresMayFallUnderDefault() {
        traffic.record(problem("POST", "/v1/things", null, 415, "{\"status\":415,\"code\":\"invalid_request\"}"));

        traffic.assertNoViolations();
    }

    @Test
    void aRequestThatBreaksTheSpecificationIsAViolationOnlyWhenTheServerAcceptedIt() {
        String badBody = "{\"name\":\"lamp\",\"size\":0}";
        traffic.record(problem("POST", "/v1/things", null, 400, "{\"status\":400,\"code\":\"invalid_request\"}", badBody));
        traffic.assertNoViolations();

        traffic.record(json("POST", "/v1/things", null, badBody, 201, THING));
        assertThat(traffic.drainViolations()).singleElement().asString().contains("request:", "size");

        traffic.record(json("POST", "/v1/things", null, "{\"name\":\"lamp\",\"weight\":3}", 201, THING));
        assertThat(traffic.drainViolations()).singleElement().asString().contains("request:", "weight");

        traffic.record(json("GET", "/v1/things/t1", "view=sideways", null, 200, THING));
        assertThat(traffic.drainViolations()).singleElement().asString().contains("request:", "view");
    }

    @Test
    void aRequestTheSpecificationDoesNotKnowMustBeRefused() {
        traffic.record(problem("GET", "/v1/widgets", null, 404, "{\"status\":404,\"code\":\"not_found\"}"));
        traffic.record(problem("DELETE", "/v1/things/t1", null, 405, "{\"status\":405,\"code\":\"invalid_request\"}"));
        traffic.assertNoViolations();

        traffic.record(json("GET", "/v1/widgets", null, null, 200, "[]"));
        traffic.record(json("DELETE", "/v1/things/t1", null, null, 204, null));
        assertThat(traffic.drainViolations()).hasSize(2)
                .allSatisfy(violation -> assertThat(violation).contains("has no operation for this request"));
    }

    @Test
    void theMostSpecificPathWins() {
        traffic.record(json("GET", "/v1/things/latest", null, null, 204, null));

        traffic.assertNoViolations();
        assertThat(traffic.coverageReport()).containsPattern("GET /v1/things/latest\\s+listed \\[200, 204]\\s+observed \\[204]");
    }

    @Test
    void violationsAreReportedOnce_andTheAssertionListsThemAll() {
        traffic.record(json("GET", "/v1/things/t1", null, null, 200, "{}"));
        traffic.record(json("GET", "/v1/things/t2", null, null, 200, "{}"));

        assertThatThrownBy(traffic::assertNoViolations).isInstanceOf(AssertionError.class)
                .hasMessageContaining("2 exchange(s) do not match the specification things.yaml")
                .hasMessageContaining("/v1/things/t1").hasMessageContaining("/v1/things/t2");
        traffic.assertNoViolations();
    }

    @Test
    void coverageListsEveryListedStatusThatWasNeverObserved() {
        assertThat(traffic.coverageGaps(Set.of())).hasSize(4);

        traffic.record(json("POST", "/v1/things", null, "{\"name\":\"lamp\"}", 201, THING));
        traffic.record(json("GET", "/v1/things/t1", null, null, 200, THING));
        traffic.record(problem("GET", "/v1/things/nope", null, 404, "{\"status\":404,\"code\":\"not_found\"}"));
        traffic.record(json("GET", "/v1/things/latest", null, null, 200, THING));
        traffic.record(json("GET", "/v1/things/latest", null, null, 204, null));
        traffic.record(new Exchange("GET", "/v1/things/t1/changes", null, Map.of(), new byte[0], 200,
                Map.of("Content-Type", List.of("text/event-stream")), null));
        traffic.assertNoViolations();

        assertThat(traffic.coverageGaps(Set.of())).containsExactly("POST /v1/things [createThing]: never observed [400, 409]");
        assertThat(traffic.coverageGaps(Set.of(400))).containsExactly("POST /v1/things [createThing]: never observed [409]");
        assertThatThrownBy(() -> traffic.assertCovered(Set.of(400))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("never observed [409]").hasMessageContaining("coverage of things.yaml");

        traffic.record(problem("POST", "/v1/things", null, 409, "{\"status\":409,\"code\":\"conflict\"}", "{\"name\":\"lamp\"}"));
        traffic.assertCovered(Set.of(400));
    }

    @Test
    void anExemptionNeverCoversASuccessStatus() {
        assertThat(traffic.coverageGaps(Set.of(200, 201, 204, 400, 404, 409)))
                .containsExactly("POST /v1/things [createThing]: never observed [201]",
                        "GET /v1/things/latest [latestThing]: never observed [200, 204]",
                        "GET /v1/things/{thing_id} [getThing]: never observed [200]",
                        "GET /v1/things/{thing_id}/changes [streamChanges]: never observed [200]");
    }

    @Test
    void theServerAndTheSpecificationMustDescribeTheSameOperations() {
        Set<String> documented = traffic.operations();
        assertThat(documented).containsExactly("GET /v1/things/latest", "GET /v1/things/{}", "GET /v1/things/{}/changes",
                "POST /v1/things");

        traffic.assertSameOperationsAs(documented);
        assertThatThrownBy(() -> traffic.assertSameOperationsAs(
                Set.of("GET /v1/things/latest", "GET /v1/things/{}", "POST /v1/things", "DELETE /v1/things/{}")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("mapped by the server but not in the specification: [DELETE /v1/things/{}]")
                .hasMessageContaining("in the specification but not mapped by the server: [GET /v1/things/{}/changes]");
    }

    @Test
    void aStreamingOperationIsRecognisedFromItsResponseType() {
        assertThat(traffic.streams("GET", "/v1/things/t1/changes")).isTrue();
        assertThat(traffic.streams("GET", "/v1/things/t1")).isFalse();
        assertThat(traffic.streams("GET", "/v1/unknown")).isFalse();
    }

    @Test
    void aSpecificationThatDoesNotParseIsRefused() {
        assertThatThrownBy(() -> OpenApiTraffic.forSpec("broken", "openapi: 3.0.3\ninfo: {title: x, version: '1'}\npaths:\n  /a:\n    get:\n      responses:\n        '200':\n          $ref: '#/components/responses/Missing'\n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refers to components it does not define: [#/components/responses/Missing]");
        assertThatThrownBy(() -> OpenApiTraffic.forSpec("broken", "openapi: 3.0.3\npaths: {}\n"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("does not parse cleanly");
    }

    static Exchange json(String method, String path, String query, String requestBody, int status, String responseBody) {
        return exchange(method, path, query, requestBody, status, "application/json", responseBody);
    }

    static Exchange problem(String method, String path, String query, int status, String responseBody) {
        return exchange(method, path, query, null, status, "application/problem+json", responseBody);
    }

    static Exchange problem(String method, String path, String query, int status, String responseBody, String requestBody) {
        return exchange(method, path, query, requestBody, status, "application/problem+json", responseBody);
    }

    private static Exchange exchange(String method, String path, String query, String requestBody, int status,
            String responseType, String responseBody) {
        Map<String, List<String>> requestHeaders = requestBody == null
                ? Map.of() : Map.of("Content-Type", List.of("application/json"));
        Map<String, List<String>> responseHeaders = responseBody == null
                ? Map.of() : Map.of("Content-Type", List.of(responseType));
        return new Exchange(method, path, query, requestHeaders,
                requestBody == null ? new byte[0] : requestBody.getBytes(StandardCharsets.UTF_8), status, responseHeaders,
                responseBody == null ? new byte[0] : responseBody.getBytes(StandardCharsets.UTF_8));
    }

    static String read(String resource) {
        try (var in = OpenApiTrafficTest.class.getClassLoader().getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
