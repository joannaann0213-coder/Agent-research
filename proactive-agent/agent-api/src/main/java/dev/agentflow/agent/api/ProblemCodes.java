package dev.agentflow.agent.api;

/** The values of {@link Problem#code()}. */
public final class ProblemCodes {

    public static final String INVALID_REQUEST = "invalid_request";
    public static final String UNAUTHENTICATED = "unauthenticated";
    public static final String FORBIDDEN = "forbidden";
    public static final String NOT_FOUND = "not_found";
    public static final String CONFLICT = "conflict";
    public static final String PAYLOAD_TOO_LARGE = "payload_too_large";
    public static final String RATE_LIMITED = "rate_limited";
    public static final String UNAVAILABLE = "unavailable";
    public static final String INTERNAL = "internal_error";

    private ProblemCodes() {
    }
}
