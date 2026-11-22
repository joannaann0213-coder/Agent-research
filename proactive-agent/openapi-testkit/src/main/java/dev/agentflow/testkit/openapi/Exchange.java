package dev.agentflow.testkit.openapi;

import java.util.List;
import java.util.Map;

/**
 * One HTTP exchange as the server saw it.
 *
 * @param path the request path as sent, without the query string
 * @param query the raw query string, or {@code null}
 * @param responseBody the response body, or {@code null} when it was streamed and not kept
 */
public record Exchange(
        String method,
        String path,
        String query,
        Map<String, List<String>> requestHeaders,
        byte[] requestBody,
        int status,
        Map<String, List<String>> responseHeaders,
        byte[] responseBody) {

    /** {@code POST /v1/things?x=1 -> 202}. */
    public String summary() {
        return method + " " + path + (query == null ? "" : "?" + query) + " -> " + status;
    }
}
