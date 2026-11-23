package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.CreateAppRequest;
import dev.agentflow.agent.server.support.AgentTestBase;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/** A web page of an allowed origin calls the API from the browser (agent-server-test.yml allows https://app.example). */
class CorsIT extends AgentTestBase {

    private static final String PAGE = "https://app.example";

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void aPageOfAnAllowedOrigin_mayAskSendAndReadTheAnswer_refusalsToo() throws Exception {
        admin.post("/v1/admin/apps", new CreateAppRequest(ACME, "Acme")).as(Object.class);
        String key = newKey(ACME);

        HttpResponse<String> preflight = send(HttpRequest.newBuilder(uri("/v1/users/u42/messages"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", PAGE)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization, content-type"));
        assertThat(preflight.statusCode()).as("asked before it is sent, without a credential").isEqualTo(200);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).hasValue(PAGE);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Methods").orElseThrow()).contains("POST");
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Headers").orElseThrow().toLowerCase())
                .contains("authorization").contains("content-type");

        HttpResponse<String> whoami = send(HttpRequest.newBuilder(uri("/v1/whoami")).header("Origin", PAGE)
                .header("Authorization", "Bearer " + key));
        assertThat(whoami.statusCode()).isEqualTo(200);
        assertThat(whoami.headers().firstValue("Access-Control-Allow-Origin")).hasValue(PAGE);

        HttpResponse<String> refused = send(HttpRequest.newBuilder(uri("/v1/whoami")).header("Origin", PAGE));
        assertThat(refused.statusCode()).isEqualTo(401);
        assertThat(refused.headers().firstValue("Access-Control-Allow-Origin")).as("so that the page can read why").hasValue(PAGE);
        assertThat(refused.headers().firstValue("Access-Control-Expose-Headers").orElse("")).doesNotContain("Authorization");
    }

    @Test
    void aPageOfAnotherOrigin_isRefusedWhenItAsks() throws Exception {
        HttpResponse<String> preflight = send(HttpRequest.newBuilder(uri("/v1/users/u42/messages"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "https://elsewhere.example")
                .header("Access-Control-Request-Method", "POST"));
        assertThat(preflight.statusCode()).isEqualTo(403);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private URI uri(String path) {
        return baseUri().resolve(path);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
