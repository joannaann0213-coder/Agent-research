package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.ProblemCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps failures to {@code application/problem+json} with a stable {@code code}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AgentException.class)
    public ResponseEntity<ProblemDetail> handle(AgentException e) {
        return problem(e.status(), e.code(), e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, ProblemCodes.INVALID_REQUEST, rootMessage(e));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException e) {
        return problem(HttpStatus.NOT_FOUND, ProblemCodes.NOT_FOUND, "no such endpoint");
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ProblemDetail> handleDatabase(Exception e) {
        log.error("database failure", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, ProblemCodes.UNAVAILABLE,
                "the service's database is unavailable; try again");
    }

    /**
     * Everything else. A request that the web framework itself refuses before a controller sees it (a
     * method the path does not have, a media type it cannot read, a response type the caller does not
     * accept) is the caller's error and gets the same format as every other one; only what is left is
     * an internal error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse refused && refused.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.resolve(refused.getStatusCode().value());
            if (status != null) {
                String detail = refused.getBody().getDetail();
                return problem(status, switch (status) {
                    case NOT_FOUND -> ProblemCodes.NOT_FOUND;
                    case CONTENT_TOO_LARGE -> ProblemCodes.PAYLOAD_TOO_LARGE;
                    default -> ProblemCodes.INVALID_REQUEST;
                }, detail != null ? detail : status.getReasonPhrase());
            }
        }
        log.error("unexpected failure", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ProblemCodes.INTERNAL, "internal error");
    }

    static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty("code", code);
        return ResponseEntity.status(status).body(body);
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null) {
            return "malformed request";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
