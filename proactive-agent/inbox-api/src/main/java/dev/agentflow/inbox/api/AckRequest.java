package dev.agentflow.inbox.api;

/**
 * Body of {@code POST /v1/runs/{run_id}/ack}: the run is over.
 *
 * @param outcome {@code done}, or {@code failed} for a final failure that must not be retried
 * @param lastRunSeq cursor: events up to this {@code run_seq} were consumed and reach the outcome;
 *        later ones (steered in but never read) go back to the queue. Absent means every event.
 * @param error short reason when the outcome is {@code failed}
 */
public record AckRequest(int epoch, String outcome, Integer lastRunSeq, String error) {

    public static final String DONE = "done";
    public static final String FAILED = "failed";

    public static AckRequest done(int epoch, int lastRunSeq) {
        return new AckRequest(epoch, DONE, lastRunSeq, null);
    }

    public static AckRequest failed(int epoch, int lastRunSeq, String error) {
        return new AckRequest(epoch, FAILED, lastRunSeq, error);
    }
}
