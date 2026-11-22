package dev.agentflow.agent.server.support;

import dev.agentflow.agent.api.AgentJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import tools.jackson.databind.JsonNode;

/** A plain HTTP client for the Agent API, as one caller: it sends one credential with every request. */
public final class TestApi {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /**
     * What came back.
     *
     * @param body the parsed body; an empty object when there was none
     */
    public record Reply(int status, JsonNode body, HttpHeaders headers) {

        public boolean ok() {
            return status >= 200 && status < 300;
        }

        /** The {@code code} of a problem document, or {@code null}. */
        public String code() {
            return body.path("code").asString(null);
        }

        /** The body as a type of the Agent API. */
        public <T> T as(Class<T> type) {
            if (!ok()) {
                throw new AssertionError("HTTP " + status + ": " + body);
            }
            return AgentJson.mapper().treeToValue(body, type);
        }
    }

    private final URI base;
    private final String credential;

    public TestApi(URI base, String credential) {
        this.base = base;
        this.credential = credential;
    }

    /** The same server, as another caller. {@code null} sends no credential at all. */
    public TestApi as(String otherCredential) {
        return new TestApi(base, otherCredential);
    }

    public Reply get(String path) {
        return send("GET", path, null);
    }

    public Reply post(String path, Object body) {
        return send("POST", path, body);
    }

    public Reply put(String path, Object body) {
        return send("PUT", path, body);
    }

    public Reply patch(String path, Object body) {
        return send("PATCH", path, body);
    }

    public Reply delete(String path) {
        return send("DELETE", path, null);
    }

    /**
     * @param body an object to send as JSON, a {@code String} to send as it is, or {@code null} for no body
     */
    public Reply send(String method, String path, Object body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(30));
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            String json = body instanceof String text ? text : AgentJson.mapper().writeValueAsString(body);
            request.method(method, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json");
        }
        if (credential != null) {
            request.header("Authorization", "Bearer " + credential);
        }
        try {
            HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode parsed = response.body().isBlank() ? AgentJson.mapper().createObjectNode()
                    : AgentJson.mapper().readTree(response.body());
            return new Reply(response.statusCode(), parsed, response.headers());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
