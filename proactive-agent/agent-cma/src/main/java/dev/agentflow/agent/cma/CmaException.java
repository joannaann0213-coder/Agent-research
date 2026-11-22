package dev.agentflow.agent.cma;

import java.time.Duration;

/** A call to Claude Managed Agents failed. {@link #kind()} says what to do about it. */
public class CmaException extends RuntimeException {

    public enum Kind {
        /** The request is wrong, or not allowed in the session's current state. Retrying does not help. */
        INVALID_REQUEST,
        /** The API key is missing, wrong, or lacks access. */
        UNAUTHORIZED,
        /** No such environment, agent or session. */
        NOT_FOUND,
        /** The object changed since it was read. */
        CONFLICT,
        /** Too many requests. See {@link CmaException#retryAfter()}. */
        RATE_LIMITED,
        /** The service is overloaded. Retry later. */
        OVERLOADED,
        /** The service failed. Retry later. */
        SERVER,
        /** No answer: the connection failed, timed out, or a stream was lost. */
        CONNECTION
    }

    private final Kind kind;
    private final int status;
    private final Duration retryAfter;

    public CmaException(Kind kind, int status, String message, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public CmaException(Kind kind, String message) {
        this(kind, defaultStatus(kind), message, null, null);
    }

    public static CmaException invalid(String message) {
        return new CmaException(Kind.INVALID_REQUEST, message);
    }

    public static CmaException notFound(String message) {
        return new CmaException(Kind.NOT_FOUND, message);
    }

    public static CmaException conflict(String message) {
        return new CmaException(Kind.CONFLICT, message);
    }

    public static CmaException rateLimited(Duration retryAfter) {
        return new CmaException(Kind.RATE_LIMITED, 429, "rate limited", retryAfter, null);
    }

    public static CmaException overloaded() {
        return new CmaException(Kind.OVERLOADED, "the service is overloaded");
    }

    public static CmaException connection(String message, Throwable cause) {
        return new CmaException(Kind.CONNECTION, 0, message, null, cause);
    }

    public Kind kind() {
        return kind;
    }

    /** The HTTP status, or 0 when there was no response. */
    public int status() {
        return status;
    }

    /** How long the service asked the caller to wait, when it said so; otherwise {@code null}. */
    public Duration retryAfter() {
        return retryAfter;
    }

    /** Whether the same call may succeed later. */
    public boolean retryable() {
        return switch (kind) {
            case RATE_LIMITED, OVERLOADED, SERVER, CONNECTION -> true;
            case INVALID_REQUEST, UNAUTHORIZED, NOT_FOUND, CONFLICT -> false;
        };
    }

    /**
     * Whether the request may have taken effect although it failed. True when no definite answer came
     * back; a refusal by the service is definite.
     */
    public boolean maybeDelivered() {
        return kind == Kind.CONNECTION || kind == Kind.SERVER;
    }

    private static int defaultStatus(Kind kind) {
        return switch (kind) {
            case INVALID_REQUEST -> 400;
            case UNAUTHORIZED -> 401;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case RATE_LIMITED -> 429;
            case OVERLOADED -> 529;
            case SERVER -> 500;
            case CONNECTION -> 0;
        };
    }
}
