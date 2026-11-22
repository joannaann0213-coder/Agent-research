package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.ProblemCodes;
import org.springframework.http.HttpStatus;

/** A request that cannot be served, with the HTTP status and machine-readable code to answer with. */
public class AgentException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AgentException(HttpStatus status, String code, String message) {
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

    public static AgentException invalid(String message) {
        return new AgentException(HttpStatus.BAD_REQUEST, ProblemCodes.INVALID_REQUEST, message);
    }

    public static AgentException forbidden(String message) {
        return new AgentException(HttpStatus.FORBIDDEN, ProblemCodes.FORBIDDEN, message);
    }

    public static AgentException notFound(String message) {
        return new AgentException(HttpStatus.NOT_FOUND, ProblemCodes.NOT_FOUND, message);
    }

    public static AgentException conflict(String message) {
        return new AgentException(HttpStatus.CONFLICT, ProblemCodes.CONFLICT, message);
    }

    public static AgentException tooLarge(String message) {
        return new AgentException(HttpStatus.CONTENT_TOO_LARGE, ProblemCodes.PAYLOAD_TOO_LARGE, message);
    }

    public static AgentException unavailable(String message) {
        return new AgentException(HttpStatus.SERVICE_UNAVAILABLE, ProblemCodes.UNAVAILABLE, message);
    }
}
