package dev.agentflow.inbox.client;

import dev.agentflow.inbox.api.AckRequest;
import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.HeartbeatRequest;
import dev.agentflow.inbox.api.HeartbeatResponse;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.SteerRequest;
import dev.agentflow.inbox.api.SteerResponse;
import dev.agentflow.inbox.api.SuspendRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A worker that stands in for the agent runtime: it claims a run, lets a {@link Script} decide what
 * to do with it, and then ends the run explicitly. There is no model here; the script is plain code.
 *
 * <p>It shows the contract a real runtime has to keep: carry the epoch on every call, send messages
 * through an idempotent outbox, end every run with exactly one of ack, nack or suspend, and stop at
 * once when the inbox says the lease is lost.
 */
public final class ScriptedWorker {

    /** What the stand-in "agent" does with a run. */
    @FunctionalInterface
    public interface Script {
        Decision handle(RunContext context) throws Exception;
    }

    /** How a run ends. */
    public sealed interface Decision {

        /** Everything consumed was handled. Saying nothing to the user is a perfectly good way to get here. */
        record Done() implements Decision {
        }

        /** A final failure that must not be retried. */
        record Failed(String error) implements Decision {
        }

        /** Could not be processed now; the same run is issued again later. */
        record Retry(String reason, Long retryAfterMs) implements Decision {
        }

        /** Wait for an answer that will arrive as an event carrying {@code resumeKey}. */
        record Suspend(String resumeKey, long timeoutSeconds, String onTimeout) implements Decision {
        }

        static Decision done() {
            return new Done();
        }
    }

    /** Raised inside a script when the inbox reports that this worker no longer holds the run. */
    public static final class LeaseLostException extends RuntimeException {
        public LeaseLostException(String runId) {
            super("lease lost for run " + runId);
        }
    }

    /** What a script can see and do while it holds a run. */
    public final class RunContext {
        private final ClaimedRun run;
        private final List<ClaimedEvent> events;
        private int lastRunSeq;
        private int nextOutboxSequence = 1;

        private RunContext(ClaimedRun run) {
            this.run = run;
            this.events = new ArrayList<>(run.events());
            this.lastRunSeq = run.lastRunSeq();
        }

        public ClaimedRun run() {
            return run;
        }

        /** Every event handed to this worker for the run so far, including steered ones. */
        public List<ClaimedEvent> events() {
            return List.copyOf(events);
        }

        /** Renews the lease and reports whether something is waiting behind the run. */
        public HeartbeatResponse heartbeat() {
            ApiResult<HeartbeatResponse> result = client.heartbeat(run.runId(), new HeartbeatRequest(run.epoch(), null));
            if (result.status() == 409) {
                throw new LeaseLostException(run.runId());
            }
            return result.require();
        }

        /** Pulls events of the run's merge key that arrived after it started. Call between two steps. */
        public List<ClaimedEvent> steer() {
            ApiResult<SteerResponse> result = client.steer(run.runId(), new SteerRequest(run.epoch(), lastRunSeq));
            if (result.status() == 409) {
                throw new LeaseLostException(run.runId());
            }
            List<ClaimedEvent> fresh = result.require().events();
            events.addAll(fresh);
            lastRunSeq = result.body().lastRunSeq(lastRunSeq);
            return fresh;
        }

        /**
         * Sends the next message of this delivery through the outbox: the first call is sequence 1, the
         * second 2, and so on. Enough for a script that starts from the top every time it gets the run.
         *
         * @return false when this message had already been delivered by an earlier attempt
         */
        public boolean send(String channel, String text) {
            return send(nextOutboxSequence, channel, text);
        }

        /**
         * Sends a message under an explicit sequence number. The rule that makes replay safe is that
         * the n-th message of a <em>run</em> always has sequence n, however the run reached that point:
         * first attempt, redelivery after a crash, or resume after a suspension. A real runtime keeps
         * that counter in the session state it saves; a script with several phases passes it here.
         */
        public boolean send(int sequence, String channel, String text) {
            nextOutboxSequence = Math.max(nextOutboxSequence, sequence + 1);
            return outbox.send(new OutboxMessage(run.runId(), sequence, channel, run.traceId(), text));
        }

        public InboxClient client() {
            return client;
        }
    }

    private final InboxClient client;
    private final Outbox outbox;
    private final String workerId;
    private final Script script;

    public ScriptedWorker(InboxClient client, Outbox outbox, String workerId, Script script) {
        this.client = client;
        this.outbox = outbox;
        this.workerId = workerId;
        this.script = script;
    }

    /** Claims from any inbox, without waiting. */
    public boolean runOnce() {
        return runOnce(ClaimRequest.any(workerId));
    }

    /**
     * Claims one run and drives it to its end.
     *
     * @return false when there was nothing to claim
     */
    public boolean runOnce(ClaimRequest request) {
        ApiResult<ClaimedRun> claimed = client.claim(new ClaimRequest(workerId, request.tenantId(), request.inboxId(),
                request.lanes(), request.acceptsSteer(), request.waitMs()));
        if (claimed.status() == 204) {
            return false;
        }
        ClaimedRun run = claimed.require();
        RunContext context = new RunContext(run);
        Decision decision;
        try {
            decision = script.handle(context);
        } catch (LeaseLostException e) {
            return true; // someone else owns the run now; stop without touching it
        } catch (Exception e) {
            decision = new Decision.Retry(e.toString(), null);
        }
        end(context, decision);
        return true;
    }

    /** Keeps claiming and processing until {@code keepGoing} says stop. */
    public void runLoop(BooleanSupplier keepGoing, long waitMs) {
        while (keepGoing.getAsBoolean()) {
            runOnce(ClaimRequest.any(workerId).withWait(waitMs));
        }
    }

    /** Every run ends with exactly one explicit call; otherwise the lease would expire and replay it. */
    private void end(RunContext context, Decision decision) {
        ClaimedRun run = context.run;
        ApiResult<?> result = switch (decision) {
            case Decision.Done done -> client.ack(run.runId(), AckRequest.done(run.epoch(), context.lastRunSeq));
            case Decision.Failed failed ->
                    client.ack(run.runId(), AckRequest.failed(run.epoch(), context.lastRunSeq, failed.error()));
            case Decision.Retry retry ->
                    client.nack(run.runId(), new NackRequest(run.epoch(), retry.reason(), retry.retryAfterMs()));
            case Decision.Suspend suspend -> client.suspend(run.runId(), new SuspendRequest(run.epoch(),
                    suspend.resumeKey(), suspend.timeoutSeconds(), suspend.onTimeout(), context.lastRunSeq));
        };
        if (result.status() != 409) {
            result.require(); // 409 means the lease was lost in the meantime: nothing more to do
        }
    }
}
