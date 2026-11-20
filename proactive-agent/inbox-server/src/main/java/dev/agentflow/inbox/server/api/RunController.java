package dev.agentflow.inbox.server.api;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.RunOutcome;
import dev.agentflow.inbox.api.RunView;
import dev.agentflow.inbox.api.SteerRequest;
import dev.agentflow.inbox.api.SteerResponse;
import dev.agentflow.inbox.api.SuspendRequest;
import dev.agentflow.inbox.server.auth.AuthenticatedClient;
import dev.agentflow.inbox.server.claim.ClaimService;
import dev.agentflow.inbox.server.claim.RunLifecycleService;
import dev.agentflow.inbox.server.query.RunQueries;
import dev.agentflow.inbox.server.time.InboxClock;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Runtime-facing API: claim a run and drive it to an explicit end. */
@RestController
@RequestMapping("/v1/runs")
public class RunController {

    /** Why a claim returned nothing: {@code empty}, {@code busy}, {@code window} or {@code backoff}. */
    public static final String HEADER_REASON = "X-Inbox-Reason";
    /** When the answer may change by time alone, in milliseconds from now. */
    public static final String HEADER_RETRY_AFTER_MS = "X-Inbox-Retry-After-Ms";

    private final ClaimService claims;
    private final RunLifecycleService lifecycle;
    private final RunQueries queries;
    private final InboxClock clock;

    public RunController(ClaimService claims, RunLifecycleService lifecycle, RunQueries queries, InboxClock clock) {
        this.claims = claims;
        this.lifecycle = lifecycle;
        this.queries = queries;
        this.clock = clock;
    }

    /**
     * 200 with a run, or 204 when there is nothing to claim. "Busy" is not an error and is not counted
     * as a failure anywhere: losing the race for an inbox is normal scheduling.
     */
    @PostMapping("/claim")
    public ResponseEntity<ClaimedRun> claim(AuthenticatedClient client, @RequestBody ClaimRequest request) {
        ClaimService.Attempt attempt = claims.claim(Tenants.filter(client, request.tenantId()), request);
        if (attempt.run() != null) {
            return ResponseEntity.ok(attempt.run());
        }
        ResponseEntity.HeadersBuilder<?> noContent = ResponseEntity.noContent()
                .header(HEADER_REASON, attempt.reason().wire());
        if (attempt.retryAt() != null) {
            long millis = Math.max(0, Duration.between(clock.now(), attempt.retryAt()).toMillis());
            noContent.header(HEADER_RETRY_AFTER_MS, Long.toString(millis));
        }
        return noContent.build();
    }

    @PostMapping("/{runId}/heartbeat")
    public HeartbeatResponse heartbeat(AuthenticatedClient client, @PathVariable String runId,
            @RequestBody HeartbeatRequest request) {
        return lifecycle.heartbeat(client.tenantId(), runId, request);
    }

    @PostMapping("/{runId}/ack")
    public RunOutcome ack(AuthenticatedClient client, @PathVariable String runId, @RequestBody AckRequest request) {
        return lifecycle.ack(client.tenantId(), runId, request);
    }

    @PostMapping("/{runId}/nack")
    public RunOutcome nack(AuthenticatedClient client, @PathVariable String runId, @RequestBody NackRequest request) {
        return lifecycle.nack(client.tenantId(), runId, request);
    }

    /** Pulls events of the run's merge key that arrived after it started into the run. */
    @PostMapping("/{runId}/steer")
    public SteerResponse steer(AuthenticatedClient client, @PathVariable String runId,
            @RequestBody SteerRequest request) {
        return lifecycle.steer(client.tenantId(), runId, request);
    }

    /** The run waits for an answer; the answer arrives as an event carrying the resume key. */
    @PostMapping("/{runId}/suspend")
    public RunOutcome suspend(AuthenticatedClient client, @PathVariable String runId,
            @RequestBody SuspendRequest request) {
        return lifecycle.suspend(client.tenantId(), runId, request);
    }

    @GetMapping("/{runId}")
    public RunView get(AuthenticatedClient client, @PathVariable String runId) {
        return queries.view(client.tenantId(), runId);
    }
}
