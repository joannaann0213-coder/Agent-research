package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.HmacSigner;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.Problem;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Every error is a problem document with a stable {@code code}, including the ones the web framework
 * raises before a controller runs. A caller should never have to parse two error formats.
 */
class ErrorShapeIT extends IntegrationTestBase {

    private static final String EVENTS = "/v1/inbox/u42/events";
    private static final String EVENT = "{\"source\":\"biz_webhook\",\"type\":\"order.paid.v1\",\"payload\":{}}";

    @Test
    void aBodyThatIsNotSentAsJsonIs415() throws Exception {
        HttpResponse<String> response = signed("POST", EVENTS, EVENT, "Content-Type", "text/plain");

        assertProblem(response, 415, ProblemCodes.INVALID_REQUEST);
        assertThat(count("SELECT count(*) FROM event")).isZero();
    }

    @Test
    void aCallerThatAcceptsNoJsonGets406_andTheErrorIsStillAProblemDocument() throws Exception {
        HttpResponse<String> response = signed("GET", EVENTS, null, "Accept", "text/csv");

        assertProblem(response, 406, ProblemCodes.INVALID_REQUEST);
    }

    @Test
    void anUnknownPathIs404_andAWrongMethodIs405() throws Exception {
        assertProblem(signed("GET", "/v1/nothing/here", null), 404, ProblemCodes.NOT_FOUND);
        assertProblem(signed("DELETE", EVENTS, null), 405, ProblemCodes.INVALID_REQUEST);
    }

    @Test
    void aValueOfTheWrongTypeInTheQueryIs400() throws Exception {
        assertProblem(signed("GET", EVENTS + "?limit=many", null), 400, ProblemCodes.INVALID_REQUEST);
        assertProblem(signed("GET", EVENTS + "?from=yesterday", null), 400, ProblemCodes.INVALID_REQUEST);
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        Problem problem = InboxJson.mapper().readValue(response.body(), Problem.class);
        assertThat(problem.status()).isEqualTo(status);
        assertThat(problem.code()).isEqualTo(code);
        assertThat(problem.detail()).isNotBlank();
    }

    /** A correctly signed request from producer {@code acme-bot}, with extra headers as name and value pairs. */
    private HttpResponse<String> signed(String method, String pathWithQuery, String body, String... headers)
            throws Exception {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        long now = clock.now().getEpochSecond();
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri().resolve(pathWithQuery))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(bytes))
                .header(HmacSigner.HEADER_CLIENT_ID, "acme-bot")
                .header(HmacSigner.HEADER_TIMESTAMP, Long.toString(now))
                .header(HmacSigner.HEADER_SIGNATURE, HmacSigner.sign("acme-secret", now, method, pathWithQuery, bytes));
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
