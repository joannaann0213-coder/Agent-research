package dev.agentflow.inbox.client;

import dev.agentflow.inbox.api.Problem;

/** A non-2xx answer from the inbox. */
public class InboxApiException extends RuntimeException {

    private final int status;
    private final transient Problem problem;

    public InboxApiException(int status, Problem problem) {
        super("inbox answered " + status + (problem == null ? "" : " " + problem.code() + ": " + problem.detail()));
        this.status = status;
        this.problem = problem;
    }

    public int status() {
        return status;
    }

    public Problem problem() {
        return problem;
    }

    public String code() {
        return problem == null ? null : problem.code();
    }
}
