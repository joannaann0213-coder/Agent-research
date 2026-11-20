package dev.agentflow.inbox.api;

/** Machine-readable error codes carried in {@link Problem#code()}. */
public final class ProblemCodes {

    public static final String INVALID_REQUEST = "invalid_request";
    public static final String UNAUTHENTICATED = "unauthenticated";
    public static final String FORBIDDEN = "forbidden";
    public static final String NOT_FOUND = "not_found";
    public static final String IDEMPOTENCY_CONFLICT = "idempotency_conflict";
    public static final String PAYLOAD_TOO_LARGE = "payload_too_large";
    public static final String DATABASE_UNAVAILABLE = "database_unavailable";
    public static final String LEASE_LOST = "lease_lost";
    public static final String RESUME_KEY_IN_USE = "resume_key_in_use";
    public static final String CONFLICT = "conflict";
    public static final String INTERNAL = "internal_error";

    private ProblemCodes() {
    }
}
