package dev.agentflow.inbox.server.claim;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.RunStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The claim decision as a pure function: no database, no clock. */
class ClaimPlannerTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final Duration QUIET = Duration.ofMillis(1500);
    private static final Duration MAX_WAIT = Duration.ofMillis(6000);

    private final ClaimPlanner planner = new ClaimPlanner();
    private final List<OpenRun> runs = new ArrayList<>();
    private final List<FreshEvent> fresh = new ArrayList<>();
    private long nextSeq = 1;

    private static InboxLimits limits(Integer maxActiveRuns) {
        Map<Lane, Integer> caps = new EnumMap<>(Lane.class);
        caps.put(Lane.INTERACTIVE, 1);
        caps.put(Lane.BACKGROUND, 2);
        caps.put(Lane.BATCH, 1);
        Map<Lane, Duration> quiet = new EnumMap<>(Lane.class);
        Map<Lane, Duration> wait = new EnumMap<>(Lane.class);
        for (Lane lane : Lane.values()) {
            quiet.put(lane, QUIET);
            wait.put(lane, MAX_WAIT);
        }
        return new InboxLimits(caps, maxActiveRuns, quiet, wait, 20, 256 * 1024);
    }

    /** An unclaimed event that arrived {@code ageMillis} ago. */
    private FreshEvent event(Lane lane, String key, long ageMillis) {
        return event(lane, key, ageMillis, null, false, 100);
    }

    private FreshEvent event(Lane lane, String key, long ageMillis, String resumeKey, boolean interrupt, int bytes) {
        FreshEvent event = new FreshEvent("e" + nextSeq, nextSeq, lane, key, resumeKey, interrupt, bytes,
                NOW.minusMillis(ageMillis));
        nextSeq++;
        fresh.add(event);
        return event;
    }

    private void activeRun(String id, Lane lane, String key, int slot) {
        runs.add(new OpenRun(id, lane, key, RunStatus.ACTIVE, 0, slot, null, null));
    }

    private void queuedRun(String id, Lane lane, String key, long headSeq, Instant nextAttemptAt) {
        runs.add(new OpenRun(id, lane, key, RunStatus.QUEUED, headSeq, null, nextAttemptAt, null));
    }

    private void suspendedRun(String id, Lane lane, String key, String resumeKey) {
        runs.add(new OpenRun(id, lane, key, RunStatus.SUSPENDED, 0, null, null, resumeKey));
    }

    private ClaimPlan plan() {
        return planner.plan(NOW, limits(null), runs, fresh, null);
    }

    private static List<String> ids(List<FreshEvent> events) {
        return events.stream().map(FreshEvent::eventId).toList();
    }

    // ---- nothing to do --------------------------------------------------------------------------

    @Test
    void emptyInbox() {
        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.EMPTY, null));
    }

    // ---- batching -------------------------------------------------------------------------------

    @Test
    void eventWithoutMergeKeyIsItsOwnRunAndDoesNotWait() {
        event(Lane.INTERACTIVE, null, 0);
        event(Lane.INTERACTIVE, null, 0);

        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(ids(start.events())).containsExactly("e1");
        assertThat(start.slotNo()).isEqualTo(1);
        assertThat(start.mergeKey()).isNull();
    }

    @Test
    void keyedEventWaitsForItsQuietWindow() {
        FreshEvent event = event(Lane.INTERACTIVE, "conversation:u42", 1000);

        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.WINDOW, event.receivedAt().plus(QUIET)));
    }

    @Test
    void burstBecomesOneBatchOnceTheWindowAfterTheLastEventClosed() {
        event(Lane.INTERACTIVE, "conversation:u42", 3000);
        event(Lane.INTERACTIVE, "conversation:u42", 2500);
        FreshEvent last = event(Lane.INTERACTIVE, "conversation:u42", 1500);

        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(ids(start.events())).containsExactly("e1", "e2", "e3");
        assertThat(start.mergeKey()).isEqualTo("conversation:u42");
        assertThat(start.eligibleAt()).isEqualTo(last.receivedAt().plus(QUIET));
    }

    @Test
    void windowIsMeasuredFromTheLastEvent() {
        event(Lane.INTERACTIVE, "conversation:u42", 3000);
        FreshEvent last = event(Lane.INTERACTIVE, "conversation:u42", 200);

        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.WINDOW, last.receivedAt().plus(QUIET)));
    }

    @Test
    void maxWaitStopsATrickleFromHoldingTheWindowOpen() {
        FreshEvent first = event(Lane.INTERACTIVE, "conversation:u42", 6000);
        for (long age = 5000; age >= 0; age -= 1000) {
            event(Lane.INTERACTIVE, "conversation:u42", age); // a message every second, the last one just now
        }

        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(start.events()).hasSize(7);
        assertThat(start.eligibleAt()).isEqualTo(first.receivedAt().plus(MAX_WAIT));
    }

    @Test
    void aFullBatchDoesNotWait_andTheRestStaysForTheNextRun() {
        for (int i = 0; i < 25; i++) {
            event(Lane.INTERACTIVE, "conversation:u42", 10);
        }
        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(start.events()).hasSize(20);
        assertThat(start.events().getFirst().seq()).isEqualTo(1);
        assertThat(start.events().getLast().seq()).isEqualTo(20);
    }

    @Test
    void batchIsCutAtTheByteCap_butTheHeadIsAlwaysTaken() {
        event(Lane.INTERACTIVE, "k", 10, null, false, 200 * 1024);
        event(Lane.INTERACTIVE, "k", 10, null, false, 50 * 1024);
        event(Lane.INTERACTIVE, "k", 10, null, false, 50 * 1024);

        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(ids(start.events())).containsExactly("e1", "e2");

        fresh.clear();
        nextSeq = 1;
        event(Lane.INTERACTIVE, "big", 10, null, false, 300 * 1024);
        assertThat(ids(((ClaimPlan.Start) plan()).events())).containsExactly("e1");
    }

    @Test
    void otherKeysInterleavedInTheQueueDoNotSplitABatch() {
        event(Lane.INTERACTIVE, "a", 5000);
        event(Lane.INTERACTIVE, "b", 5000);
        event(Lane.INTERACTIVE, "a", 4000);

        assertThat(ids(((ClaimPlan.Start) plan()).events())).containsExactly("e1", "e3");
    }

    @Test
    void aKeyStillInItsWindowDoesNotBlockAnotherKey() {
        event(Lane.INTERACTIVE, "typing", 100);
        event(Lane.INTERACTIVE, "settled", 5000);

        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(start.mergeKey()).isEqualTo("settled");
    }

    // ---- lanes and slots ------------------------------------------------------------------------

    @Test
    void lanesAreServedInPriorityOrder() {
        event(Lane.BATCH, null, 9000);
        event(Lane.BACKGROUND, null, 8000);
        event(Lane.INTERACTIVE, null, 10);

        assertThat(((ClaimPlan.Start) plan()).lane()).isEqualTo(Lane.INTERACTIVE);
    }

    @Test
    void aFullLaneIsBusy_butALowerLaneCanStillRun() {
        activeRun("r1", Lane.INTERACTIVE, "conversation:u42", 1);
        event(Lane.INTERACTIVE, null, 5000);
        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.BUSY, null));

        event(Lane.BACKGROUND, null, 5000);
        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(start.lane()).isEqualTo(Lane.BACKGROUND);
        assertThat(ids(start.events())).containsExactly("e2");
    }

    @Test
    void backgroundHasTwoSlots() {
        activeRun("r1", Lane.BACKGROUND, "report:daily", 1);
        event(Lane.BACKGROUND, null, 5000);
        assertThat(((ClaimPlan.Start) plan()).slotNo()).isEqualTo(2);

        activeRun("r2", Lane.BACKGROUND, "report:weekly", 2);
        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.BUSY, null));
    }

    @Test
    void theSameMergeKeyNeverRunsTwiceAtOnce_evenAcrossLanes() {
        activeRun("r1", Lane.INTERACTIVE, "order:1234", 1);
        event(Lane.BACKGROUND, "order:1234", 5000); // a free background slot, but the key is being handled

        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.BUSY, null));
    }

    @Test
    void strictModeAllowsOneRunPerInbox() {
        activeRun("r1", Lane.INTERACTIVE, "conversation:u42", 1);
        event(Lane.BACKGROUND, null, 5000);

        assertThat(planner.plan(NOW, limits(1), runs, fresh, null))
                .isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.BUSY, null));
    }

    @Test
    void aClaimerMayRestrictItselfToSomeLanes() {
        event(Lane.INTERACTIVE, null, 5000);
        event(Lane.BATCH, null, 5000);

        ClaimPlan.Start start = (ClaimPlan.Start) planner.plan(NOW, limits(null), runs, fresh, List.of(Lane.BATCH));
        assertThat(start.lane()).isEqualTo(Lane.BATCH);
        assertThat(planner.plan(NOW, limits(null), runs, fresh, List.of(Lane.BACKGROUND)))
                .isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.EMPTY, null));
    }

    // ---- retries --------------------------------------------------------------------------------

    @Test
    void aQueuedRunThatIsDueIsReissued() {
        queuedRun("r1", Lane.INTERACTIVE, "order:1", 1, NOW.minusSeconds(1));

        assertThat(plan()).isEqualTo(new ClaimPlan.Reissue("r1", Lane.INTERACTIVE, 1));
    }

    @Test
    void aQueuedRunGoesBeforeYoungerEventsOfItsKey() {
        queuedRun("r1", Lane.INTERACTIVE, "order:1", 1, NOW.plusSeconds(4));
        nextSeq = 2;
        event(Lane.INTERACTIVE, "order:1", 5000);

        // The run is still backing off: its key waits with it instead of starting a second run.
        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.BACKOFF, NOW.plusSeconds(4)));
    }

    @Test
    void aRunInBackoffDoesNotBlockOtherKeys() {
        queuedRun("r1", Lane.INTERACTIVE, "order:1", 1, NOW.plusSeconds(4));
        nextSeq = 2;
        event(Lane.INTERACTIVE, "order:2", 5000);

        assertThat(((ClaimPlan.Start) plan()).mergeKey()).isEqualTo("order:2");
    }

    @Test
    void amongCandidatesTheLowestHeadSeqWins() {
        event(Lane.INTERACTIVE, null, 5000); // seq 1
        queuedRun("r1", Lane.INTERACTIVE, "order:1", 7, NOW.minusSeconds(1));
        assertThat(ids(((ClaimPlan.Start) plan()).events())).containsExactly("e1");

        fresh.clear();
        nextSeq = 9;
        event(Lane.INTERACTIVE, null, 5000); // seq 9, younger than the queued run's head
        assertThat(plan()).isEqualTo(new ClaimPlan.Reissue("r1", Lane.INTERACTIVE, 1));
    }

    @Test
    void aQueuedRunWhoseKeyIsActiveElsewhereWaits() {
        activeRun("r0", Lane.BACKGROUND, "order:1", 1);
        queuedRun("r1", Lane.INTERACTIVE, "order:1", 1, NOW.minusSeconds(1));

        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.BUSY, null));
    }

    // ---- interrupt ------------------------------------------------------------------------------

    @Test
    void anInterruptFlaggedEventJumpsTheQueueAndSkipsTheWindow() {
        event(Lane.INTERACTIVE, null, 5000);
        event(Lane.INTERACTIVE, "conversation:u42", 5000);
        event(Lane.INTERACTIVE, "conversation:u7", 10, null, true, 100);

        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(ids(start.events())).containsExactly("e3");
    }

    // ---- suspend / resume -----------------------------------------------------------------------

    @Test
    void anEventCarryingAResumeKeyContinuesTheSuspendedRun() {
        suspendedRun("r1", Lane.BACKGROUND, "order:1", "approval:7788");
        FreshEvent reply = event(Lane.INTERACTIVE, "conversation:u42", 10, "approval:7788", false, 100);

        // The reply is on the interactive lane, but the run resumes in its own lane, without a quiet window.
        assertThat(plan()).isEqualTo(new ClaimPlan.Resume("r1", Lane.BACKGROUND, 1, List.of(reply)));
    }

    @Test
    void aReplyIsReservedForItsRun_neverBatchedWithOtherEventsOfItsKey() {
        suspendedRun("r1", Lane.INTERACTIVE, "order:1", "approval:7788");
        event(Lane.INTERACTIVE, "conversation:u42", 5000);
        FreshEvent reply = event(Lane.INTERACTIVE, "conversation:u42", 4000, "approval:7788", false, 100);
        event(Lane.INTERACTIVE, "conversation:u42", 3000);

        ClaimPlan first = plan();
        assertThat(first).isInstanceOf(ClaimPlan.Start.class);
        assertThat(ids(((ClaimPlan.Start) first).events())).containsExactly("e1", "e3");
        assertThat(reply.seq()).isEqualTo(2);
    }

    @Test
    void aSuspendedRunHoldsNoSlotAndBlocksNoKey() {
        suspendedRun("r1", Lane.INTERACTIVE, "conversation:u42", "approval:7788");
        event(Lane.INTERACTIVE, "conversation:u42", 5000);

        ClaimPlan.Start start = (ClaimPlan.Start) plan();
        assertThat(start.mergeKey()).isEqualTo("conversation:u42");
        assertThat(start.slotNo()).isEqualTo(1);
    }

    @Test
    void aResumeKeyThatMatchesNoSuspendedRunIsAnOrdinaryEvent() {
        event(Lane.INTERACTIVE, null, 5000, "approval:already-handled", false, 100);

        assertThat(plan()).isInstanceOf(ClaimPlan.Start.class);
    }

    @Test
    void aResumeWaitsWhileItsMergeKeyIsActive() {
        activeRun("r0", Lane.INTERACTIVE, "order:1", 1);
        suspendedRun("r1", Lane.BACKGROUND, "order:1", "approval:7788");
        event(Lane.INTERACTIVE, null, 10, "approval:7788", false, 100);

        assertThat(plan()).isEqualTo(new ClaimPlan.Nothing(ClaimPlan.Reason.BUSY, null));
    }
}
