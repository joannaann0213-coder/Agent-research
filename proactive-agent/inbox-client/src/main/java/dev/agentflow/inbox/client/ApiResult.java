package dev.agentflow.inbox.client;

import dev.agentflow.inbox.api.Problem;
import java.net.http.HttpHeaders;
import java.util.Optional;

/**
 * Outcome of one HTTP call: the status, the parsed body on success, the problem document on failure.
 */
public record ApiResult<T>(int status, T body, Problem problem, HttpHeaders headers) {

    public boolean ok() {
        return status >= 200 && status < 300;
    }

    /** The body, or an exception carrying the status and problem code. */
    public T require() {
        if (!ok()) {
            throw new InboxApiException(status, problem);
        }
        return body;
    }

    public String code() {
        return problem == null ? null : problem.code();
    }

    public Optional<String> header(String name) {
        return headers.firstValue(name);
    }
}
