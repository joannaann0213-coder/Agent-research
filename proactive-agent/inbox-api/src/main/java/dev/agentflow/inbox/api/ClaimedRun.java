package dev.agentflow.inbox.api;

import java.time.Instant;
import java.util.List;

/**
 * A run granted to a worker: a batch of already merged events plus a lease.
 *
 * @param epoch fencing token of this lease grant; every later call for the run must carry it
 * @param attempt failure budget used so far (1 on the first delivery)
 * @param kind {@code start} for a new run, {@code redelivery} when the same run is issued again after a
 *        nack or a lease expiry, {@code resume} when a suspended run continues
 * @param resumeReason {@code event} when an event carrying the resume key arrived, {@code timeout} when
 *        the suspend deadline passed and the run asked to be told
 * @param events for {@code start} and {@code redelivery}: every event of the run; for a resume by event:
 *        the events that arrived for it
 */
public record ClaimedRun(
        String runId,
        int epoch,
        int attempt,
        String kind,
        String resumeReason,
        String tenantId,
        String inboxId,
        Lane lane,
        String mergeKey,
        String traceId,
        boolean acceptsSteer,
        Instant leaseExpiresAt,
        Instant leaseDeadlineAt,
        List<ClaimedEvent> events) {

    public static final String KIND_START = "start";
    public static final String KIND_REDELIVERY = "redelivery";
    public static final String KIND_RESUME = "resume";

    /** Highest {@code run_seq} delivered here: the cursor to send with ack when everything was consumed. */
    public int lastRunSeq() {
        return events.stream().mapToInt(ClaimedEvent::runSeq).max().orElse(0);
    }
}
