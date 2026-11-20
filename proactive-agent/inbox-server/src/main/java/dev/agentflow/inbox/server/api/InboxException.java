package dev.agentflow.inbox.server.api;

import dev.agentflow.inbox.api.ProblemCodes;
import org.springframework.http.HttpStatus;

/** A request that cannot be served, with the HTTP status and machine-readable code to answer with. */
public class InboxException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public InboxException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public static InboxException invalid(String message) {
        return new InboxException(HttpStatus.BAD_REQUEST, ProblemCodes.INVALID_REQUEST, message);
    }

    public static InboxException forbidden(String message) {
        return new InboxException(HttpStatus.FORBIDDEN, ProblemCodes.FORBIDDEN, message);
    }

    public static InboxException notFound(String message) {
        return new InboxException(HttpStatus.NOT_FOUND, ProblemCodes.NOT_FOUND, message);
    }

    public static InboxException conflict(String code, String message) {
        return new InboxException(HttpStatus.CONFLICT, code, message);
    }

    public static InboxException tooLarge(String message) {
        return new InboxException(HttpStatus.CONTENT_TOO_LARGE, ProblemCodes.PAYLOAD_TOO_LARGE, message);
    }

    public static InboxException unavailable(String message) {
        return new InboxException(HttpStatus.SERVICE_UNAVAILABLE, ProblemCodes.DATABASE_UNAVAILABLE, message);
    }
}
