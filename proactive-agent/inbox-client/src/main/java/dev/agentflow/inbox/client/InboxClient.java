package dev.agentflow.inbox.client;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.CreateTimerRequest;
import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.HmacSigner;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.Problem;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.api.SteerRequest;
import dev.agentflow.inbox.api.SteerResponse;
import dev.agentflow.inbox.api.SuspendRequest;
import dev.agentflow.inbox.api.TimerView;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;

/**
 * Client for the Event Inbox HTTP API. Signs every request; thread-safe.
 *
 * <p>Methods return an {@link ApiResult} so that callers can branch on the status (204 "nothing to
 * claim" and 409 "lease lost" are ordinary answers for a worker); call {@link ApiResult#require()} to
 * turn a failure into an exception.
 */
public class InboxClient {

    private final HttpClient http;
    private final URI baseUri;
    private final String clientId;
    private final String secret;
    private final Supplier<Instant> clock;
    private final JsonMapper json = InboxJson.mapper();
    private final Duration requestTimeout;

    public InboxClient(URI baseUri, String clientId, String secret) {
        this(baseUri, clientId, secret, Instant::now, Duration.ofSeconds(60));
    }

    /**
     * @param clock source of the request timestamp; must agree with the server clock within its skew allowance
     * @param requestTimeout must exceed the longest long-poll the caller asks for
     */
    public InboxClient(URI baseUri, String clientId, String secret, Supplier<Instant> clock, Duration requestTimeout) {
        String base = baseUri.toString();
        this.baseUri = URI.create(base.endsWith("/") ? base.substring(0, base.length() - 1) : base);
        this.clientId = clientId;
        this.secret = secret;
        this.clock = clock;
        this.requestTimeout = requestTimeout;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    // ---- producer -------------------------------------------------------------------------------

    public ApiResult<WriteEventResponse> writeEvent(String inboxId, WriteEventRequest request) {
        return post("/v1/inbox/" + enc(inboxId) + "/events", request, WriteEventResponse.class);
    }

    public ApiResult<EventDetail> getEvent(String inboxId, String eventId) {
        return get("/v1/inbox/" + enc(inboxId) + "/events/" + enc(eventId), EventDetail.class);
    }

    /** As {@link #getEvent(String, String)} for a platform-scope caller, which must name the tenant. */
    public ApiResult<EventDetail> getEvent(String tenantId, String inboxId, String eventId) {
        return get("/v1/inbox/" + enc(inboxId) + "/events/" + enc(eventId) + "?tenant_id=" + enc(tenantId),
                EventDetail.class);
    }

    /** 201 for a new timer, 200 when its timer_key was already scheduled. */
    public ApiResult<TimerView> createTimer(String inboxId, CreateTimerRequest request) {
        return post("/v1/inbox/" + enc(inboxId) + "/timers", request, TimerView.class);
    }

    public ApiResult<TimerView> getTimer(String inboxId, String timerId) {
        return get("/v1/inbox/" + enc(inboxId) + "/timers/" + enc(timerId), TimerView.class);
    }

    public ApiResult<Void> cancelTimer(String inboxId, String timerId) {
        return delete("/v1/inbox/" + enc(inboxId) + "/timers/" + enc(timerId), Void.class);
    }

    // ---- runtime --------------------------------------------------------------------------------

    /** 200 with a run, or 204 (body null) when nothing is claimable; see the X-Inbox-Reason header. */
    public ApiResult<ClaimedRun> claim(ClaimRequest request) {
        return post("/v1/runs/claim", request, ClaimedRun.class);
    }

    public ApiResult<HeartbeatResponse> heartbeat(String runId, HeartbeatRequest request) {
        return post("/v1/runs/" + enc(runId) + "/heartbeat", request, HeartbeatResponse.class);
    }

    public ApiResult<RunOutcome> ack(String runId, AckRequest request) {
        return post("/v1/runs/" + enc(runId) + "/ack", request, RunOutcome.class);
    }

    /** Acks every event that was delivered with the run. */
    public ApiResult<RunOutcome> ackDone(ClaimedRun run) {
        return ack(run.runId(), AckRequest.done(run.epoch(), run.lastRunSeq()));
    }

    public ApiResult<RunOutcome> nack(String runId, NackRequest request) {
        return post("/v1/runs/" + enc(runId) + "/nack", request, RunOutcome.class);
    }

    public ApiResult<SteerResponse> steer(String runId, SteerRequest request) {
        return post("/v1/runs/" + enc(runId) + "/steer", request, SteerResponse.class);
    }

    public ApiResult<RunOutcome> suspend(String runId, SuspendRequest request) {
        return post("/v1/runs/" + enc(runId) + "/suspend", request, RunOutcome.class);
    }

    public ApiResult<RunView> getRun(String runId) {
        return get("/v1/runs/" + enc(runId), RunView.class);
    }

    // ---- plumbing -------------------------------------------------------------------------------

    public <T> ApiResult<T> get(String pathWithQuery, Class<T> type) {
        return send("GET", pathWithQuery, null, type);
    }

    public <T> ApiResult<T> post(String pathWithQuery, Object body, Class<T> type) {
        return send("POST", pathWithQuery, body == null ? new byte[0] : json.writeValueAsBytes(body), type);
    }

    public <T> ApiResult<T> put(String pathWithQuery, Object body, Class<T> type) {
        return send("PUT", pathWithQuery, body == null ? new byte[0] : json.writeValueAsBytes(body), type);
    }

    public <T> ApiResult<T> delete(String pathWithQuery, Class<T> type) {
        return send("DELETE", pathWithQuery, null, type);
    }

    /** Sends raw bytes, signed. Useful for testing how the server treats malformed bodies. */
    public <T> ApiResult<T> send(String method, String pathWithQuery, byte[] body, Class<T> type) {
        long timestamp = clock.get().getEpochSecond();
        String signature = HmacSigner.sign(secret, timestamp, method, pathWithQuery, body);
        return exchange(method, pathWithQuery, body, type, clientId, Long.toString(timestamp), signature);
    }

    /** Sends a request with explicit authentication headers (any of which may be null). */
    public <T> ApiResult<T> exchange(String method, String pathWithQuery, byte[] body, Class<T> type,
            String clientIdHeader, String timestampHeader, String signatureHeader) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUri + pathWithQuery))
                .timeout(requestTimeout)
                .method(method, body == null || body.length == 0 && !"POST".equals(method) && !"PUT".equals(method)
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        if (body != null && body.length > 0) {
            request.header("Content-Type", "application/json");
        }
        if (clientIdHeader != null) {
            request.header(HmacSigner.HEADER_CLIENT_ID, clientIdHeader);
        }
        if (timestampHeader != null) {
            request.header(HmacSigner.HEADER_TIMESTAMP, timestampHeader);
        }
        if (signatureHeader != null) {
            request.header(HmacSigner.HEADER_SIGNATURE, signatureHeader);
        }
        HttpResponse<byte[]> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while calling the inbox", e);
        }
        int status = response.statusCode();
        byte[] bytes = response.body();
        if (status >= 200 && status < 300) {
            T parsed = bytes.length == 0 || type == Void.class ? null : json.readValue(bytes, type);
            return new ApiResult<>(status, parsed, null, response.headers());
        }
        return new ApiResult<>(status, null, parseProblem(status, bytes), response.headers());
    }

    private Problem parseProblem(int status, byte[] bytes) {
        if (bytes.length == 0) {
            return new Problem(null, null, status, null, null, null);
        }
        try {
            return json.readValue(bytes, Problem.class);
        } catch (RuntimeException e) {
            return new Problem(null, null, status, new String(bytes, StandardCharsets.UTF_8), null, null);
        }
    }

    protected static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public JsonMapper json() {
        return json;
    }

    public URI baseUri() {
        return baseUri;
    }
}
