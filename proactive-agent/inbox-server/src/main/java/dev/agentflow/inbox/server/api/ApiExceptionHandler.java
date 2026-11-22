package dev.agentflow.inbox.server.api;

import dev.agentflow.inbox.api.ProblemCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps failures to {@code application/problem+json} with a stable {@code code}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InboxException.class)
    public ResponseEntity<ProblemDetail> handle(InboxException e) {
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException e) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ProblemCodes.INVALID_REQUEST, e.getMessage());
    }

    /**
     * A write that cannot reach the database fails explicitly. It is never downgraded to "run it
     * directly": that would bypass dedupe and leave no record of the event.
     */
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ProblemDetail> handleDatabase(Exception e) {
        log.error("database failure", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, ProblemCodes.DATABASE_UNAVAILABLE,
                "the event store is unavailable; retry with the same idempotency key");
    }

    /**
     * Everything else. A request that the web framework itself refuses before a controller sees it (a
     * media type it cannot read, a response type the caller does not accept) is the caller's error and
     * gets the same format as every other one; only what is left is an internal error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse refused && refused.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.resolve(refused.getStatusCode().value());
            if (status != null) {
                String detail = refused.getBody().getDetail();
                return problem(status, codeFor(status), detail != null ? detail : status.getReasonPhrase());
            }
        }
        log.error("unexpected failure", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ProblemCodes.INTERNAL, "internal error");
    }

    private static String codeFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> ProblemCodes.NOT_FOUND;
            case CONTENT_TOO_LARGE -> ProblemCodes.PAYLOAD_TOO_LARGE;
            default -> ProblemCodes.INVALID_REQUEST;
        };
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
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
