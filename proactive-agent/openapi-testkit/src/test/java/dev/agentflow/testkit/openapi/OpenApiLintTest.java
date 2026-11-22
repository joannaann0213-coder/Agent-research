package dev.agentflow.testkit.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class OpenApiLintTest {

    @Test
    void aSoundSpecificationHasNoProblems() {
        assertThat(OpenApiLint.problems(OpenApiTrafficTest.read("things.yaml"))).isEmpty();
        OpenApiLint.assertClean("things.yaml", OpenApiTrafficTest.read("things.yaml"));
    }

    @Test
    void structuralProblemsAreAllReported() {
        List<String> problems = OpenApiLint.problems(OpenApiTrafficTest.read("flawed.yaml"));

        // The parser reports the repeated operationId and the undeclared path variable in its own words as well.
        assertThat(problems).hasSize(14).contains(
                "nothing refers to the component #/components/schemas/Orphan",
                "the operationId createThing is used more than once",
                "GET /v1/things/{thing_id} has no summary",
                "GET /v1/things/{thing_id} uses the tag Misc, which is not declared",
                "GET /v1/things/{thing_id} has no success response",
                "GET /v1/things/{thing_id}: the path has the variables [thing_id] but declares the path parameters []",
                "DELETE /v1/boxes/{box_id} has no operationId",
                "DELETE /v1/boxes/{box_id} has no tag",
                "DELETE /v1/boxes/{box_id}: the path parameter box_id is not marked as required",
                "DELETE /v1/boxes/{box_id}: the path has the variables [box_id] but declares the path parameters [box_id, shelf_id]",
                "a security requirement names the scheme Ghost, which is not defined",
                "nothing requires the security scheme Spare");
        assertThatThrownBy(() -> OpenApiLint.assertClean("flawed.yaml", OpenApiTrafficTest.read("flawed.yaml")))
                .isInstanceOf(AssertionError.class).hasMessageContaining("the specification flawed.yaml has 14 problem(s)");
    }

    @Test
    void anExampleThatDoesNotMatchItsSchemaIsReported() {
        String specification = OpenApiTrafficTest.read("flawed.yaml")
                .replace("  - Ghost: []\n", "")
                .replace("    Spare:\n      type: apiKey\n      in: header\n      name: X-Spare\n", "")
                .replace("    Orphan:\n      type: object\n", "")
                .replace("      tags: [Things, Misc]\n      operationId: createThing\n",
                        "      tags: [Things]\n      operationId: getThing\n      summary: Read a thing\n"
                                + "      parameters:\n        - name: thing_id\n          in: path\n          required: true\n"
                                + "          schema:\n            type: string\n")
                .replace("        '404':\n          description: No such thing.\n",
                        "        '200':\n          description: The thing.\n")
                .replace("        in: path\n        schema:\n          type: string\n      - name: shelf_id\n        in: path\n        required: true\n",
                        "        in: path\n        required: true\n")
                .replace("    delete:\n      summary: Remove a box\n",
                        "    delete:\n      tags: [Things]\n      operationId: removeBox\n      summary: Remove a box\n");

        List<String> problems = OpenApiLint.problems(specification);

        assertThat(problems).hasSize(2);
        assertThat(problems.get(0)).startsWith("POST /v1/things: the request example does not match its schema:").contains("size");
        assertThat(problems.get(1)).startsWith("POST /v1/things: the example of response 201 'lamp' does not match its schema:")
                .contains("thing_id");
    }

    @Test
    void aDanglingReferenceAndAParserComplaintAreReported() {
        assertThat(OpenApiLint.problems(OpenApiTrafficTest.read("things.yaml").replace("#/components/schemas/NewThing", "#/components/schemas/Missing")))
                .contains("a reference points at a component that is not defined: #/components/schemas/Missing",
                        "nothing refers to the component #/components/schemas/NewThing");
        assertThat(OpenApiLint.problems("openapi: 3.0.3\npaths: {}\n")).isNotEmpty();
        assertThat(OpenApiLint.problems("not: [valid")).isNotEmpty();
    }
}
