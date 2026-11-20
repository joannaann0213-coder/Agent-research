package dev.agentflow.inbox.api;

/**
 * Body of {@code POST /v1/runs/{run_id}/suspend}: the run stops and waits for an answer (an approval,
 * a reply, a confirmation). Its slot and lease are released; its state is the runtime's to keep.
 *
 * <p>The answer is not a callback. It arrives as an ordinary event that carries {@code resume_key},
 * and the next claim hands the same run back with that event.
 *
 * @param resumeKey what the answering event will carry; unique among the inbox's suspended runs (required)
 * @param timeoutSeconds how long to wait (required: a run without a deadline could wait forever)
 * @param onTimeout {@code finish} (default) ends the run silently when the deadline passes;
 *        {@code resume} hands the run back to the runtime with {@code resume_reason=timeout}
 * @param lastRunSeq cursor, as for ack: events beyond it were not consumed and go back to the queue
 */
public record SuspendRequest(int epoch, String resumeKey, Long timeoutSeconds, String onTimeout, Integer lastRunSeq) {

    public static final String FINISH = "finish";
    public static final String RESUME = "resume";
}
