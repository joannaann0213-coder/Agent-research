package dev.agentflow.inbox.server.claim;

import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.RunStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides what a claim on one inbox takes. A pure function: no clock, no database, no model.
 *
 * <p>The caller holds the inbox row lock and passes in the inbox's open runs and its unclaimed
 * ("fresh") events in {@code seq} order. Three limits are applied:
 *
 * <ol>
 * <li><b>Lane slots.</b> At most {@code cap(lane)} active runs per lane.</li>
 * <li><b>Key exclusivity.</b> At most one active run per merge key, across lanes. This is the real
 *     content of "single flight": two runs working on the same conversation or order would overwrite
 *     each other's state and answer twice.</li>
 * <li><b>Optional inbox-wide cap.</b> {@code maxActiveRuns = 1} is the strict "one run per inbox".</li>
 * </ol>
 *
 * <p>Within those limits, lanes are served in priority order and inside a lane the candidate with the
 * lowest head {@code seq} wins, interrupt-flagged work first. Ordering is strict per merge key. A key
 * that is in backoff or still inside its quiet window does not block other keys of the lane.
 */
public final class ClaimPlanner {

    private record Candidate(int priority, long headSeq, ClaimPlan plan) {
        static final Comparator<Candidate> ORDER =
                Comparator.comparingInt(Candidate::priority).thenComparingLong(Candidate::headSeq);
    }

    /** Collects why nothing was claimable, to answer the caller precisely. */
    private static final class Outlook {
        private boolean blocked;
        private Instant windowAt;
        private Instant backoffAt;

        void window(Instant at) {
            if (windowAt == null || at.isBefore(windowAt)) {
                windowAt = at;
            }
        }

        void backoff(Instant at) {
            if (backoffAt == null || at.isBefore(backoffAt)) {
                backoffAt = at;
            }
        }

        ClaimPlan.Nothing toPlan(boolean anyWork) {
            if (windowAt != null && (backoffAt == null || !backoffAt.isBefore(windowAt))) {
                return new ClaimPlan.Nothing(ClaimPlan.Reason.WINDOW, windowAt);
            }
            if (backoffAt != null) {
                return new ClaimPlan.Nothing(ClaimPlan.Reason.BACKOFF, backoffAt);
            }
            return new ClaimPlan.Nothing(blocked || anyWork ? ClaimPlan.Reason.BUSY : ClaimPlan.Reason.EMPTY, null);
        }
    }

    /**
     * @param runs the inbox's open runs
     * @param fresh the inbox's unclaimed events, ascending by {@code seq}
     * @param requestedLanes lanes the claimer accepts; null or empty means all
     */
    public ClaimPlan plan(Instant now, InboxLimits limits, List<OpenRun> runs, List<FreshEvent> fresh,
            Collection<Lane> requestedLanes) {
        List<OpenRun> active = runs.stream().filter(r -> r.status() == RunStatus.ACTIVE).toList();
        boolean anyWork = !fresh.isEmpty() || runs.stream().anyMatch(r -> r.status() == RunStatus.QUEUED);

        if (limits.maxActiveRuns() != null && active.size() >= limits.maxActiveRuns()) {
            return new ClaimPlan.Nothing(anyWork ? ClaimPlan.Reason.BUSY : ClaimPlan.Reason.EMPTY, null);
        }

        Set<String> activeKeys = new HashSet<>();
        for (OpenRun run : active) {
            if (run.mergeKey() != null) {
                activeKeys.add(run.mergeKey());
            }
        }

        // Events that answer a suspended run are reserved for it: they never batch and never steer.
        Set<String> suspendedKeys = new HashSet<>();
        for (OpenRun run : runs) {
            if (run.status() == RunStatus.SUSPENDED && run.resumeKey() != null) {
                suspendedKeys.add(run.resumeKey());
            }
        }
        Map<String, List<FreshEvent>> reserved = new HashMap<>();
        List<FreshEvent> unreserved = new ArrayList<>(fresh.size());
        for (FreshEvent event : fresh) {
            if (event.resumeKey() != null && suspendedKeys.contains(event.resumeKey())) {
                reserved.computeIfAbsent(event.resumeKey(), k -> new ArrayList<>()).add(event);
            } else {
                unreserved.add(event);
            }
        }

        Outlook outlook = new Outlook();
        for (Lane lane : Lane.values()) { // declared in priority order
            if (requestedLanes != null && !requestedLanes.isEmpty() && !requestedLanes.contains(lane)) {
                continue;
            }
            List<OpenRun> activeInLane = active.stream().filter(r -> r.lane() == lane).toList();
            if (activeInLane.size() >= limits.cap(lane)) {
                if (hasWork(lane, runs, unreserved, reserved)) {
                    outlook.blocked = true;
                }
                continue;
            }
            int slot = lowestFreeSlot(activeInLane, limits.cap(lane));

            List<Candidate> candidates = new ArrayList<>();
            Set<String> queuedKeys = new HashSet<>();

            // (i) A queued run goes before any younger event of its key: same run id, same events.
            for (OpenRun run : runs) {
                if (run.status() != RunStatus.QUEUED || run.lane() != lane) {
                    continue;
                }
                if (run.mergeKey() != null) {
                    queuedKeys.add(run.mergeKey());
                    if (activeKeys.contains(run.mergeKey())) {
                        outlook.blocked = true;
                        continue;
                    }
                }
                if (run.nextAttemptAt().isAfter(now)) {
                    outlook.backoff(run.nextAttemptAt());
                    continue;
                }
                candidates.add(new Candidate(1, run.headSeq(), new ClaimPlan.Reissue(run.runId(), lane, slot)));
            }

            // (ii) A suspended run whose resume key has arrived.
            for (OpenRun run : runs) {
                if (run.status() != RunStatus.SUSPENDED || run.lane() != lane) {
                    continue;
                }
                List<FreshEvent> answers = reserved.get(run.resumeKey());
                if (answers == null) {
                    continue;
                }
                if (run.mergeKey() != null && activeKeys.contains(run.mergeKey())) {
                    outlook.blocked = true;
                    continue;
                }
                boolean interrupt = answers.stream().anyMatch(FreshEvent::interrupt);
                candidates.add(new Candidate(interrupt ? 0 : 1, answers.getFirst().seq(),
                        new ClaimPlan.Resume(run.runId(), lane, slot, List.copyOf(answers))));
            }

            // (iii) Unclaimed events, grouped by merge key. An event without a key is its own group.
            for (List<FreshEvent> group : groups(lane, unreserved)) {
                String key = group.getFirst().mergeKey();
                if (key != null && (activeKeys.contains(key) || queuedKeys.contains(key))) {
                    outlook.blocked = true;
                    continue;
                }
                boolean interrupt = group.stream().anyMatch(FreshEvent::interrupt);
                Instant eligibleAt = eligibleAt(group, key, interrupt, lane, limits);
                if (eligibleAt.isAfter(now)) {
                    outlook.window(eligibleAt);
                    continue;
                }
                candidates.add(new Candidate(interrupt ? 0 : 1, group.getFirst().seq(),
                        new ClaimPlan.Start(lane, slot, key, batch(group, limits), eligibleAt)));
            }

            if (!candidates.isEmpty()) {
                return candidates.stream().min(Candidate.ORDER).orElseThrow().plan();
            }
        }
        return outlook.toPlan(false);
    }

    private static boolean hasWork(Lane lane, List<OpenRun> runs, List<FreshEvent> unreserved,
            Map<String, List<FreshEvent>> reserved) {
        for (OpenRun run : runs) {
            if (run.lane() != lane) {
                continue;
            }
            if (run.status() == RunStatus.QUEUED) {
                return true;
            }
            if (run.status() == RunStatus.SUSPENDED && reserved.containsKey(run.resumeKey())) {
                return true;
            }
        }
        return unreserved.stream().anyMatch(e -> e.lane() == lane);
    }

    private static int lowestFreeSlot(List<OpenRun> activeInLane, int cap) {
        Set<Integer> used = new HashSet<>();
        for (OpenRun run : activeInLane) {
            used.add(run.slotNo());
        }
        for (int slot = 1; slot <= cap; slot++) {
            if (!used.contains(slot)) {
                return slot;
            }
        }
        throw new IllegalStateException("no free slot although the lane is below its cap");
    }

    /** Groups of one lane in order of their first event; events inside a group stay in seq order. */
    private static List<List<FreshEvent>> groups(Lane lane, List<FreshEvent> unreserved) {
        Map<String, List<FreshEvent>> keyed = new LinkedHashMap<>();
        List<List<FreshEvent>> result = new ArrayList<>();
        for (FreshEvent event : unreserved) {
            if (event.lane() != lane) {
                continue;
            }
            if (event.mergeKey() == null) {
                result.add(List.of(event));
            } else {
                List<FreshEvent> group = keyed.get(event.mergeKey());
                if (group == null) {
                    group = new ArrayList<>();
                    keyed.put(event.mergeKey(), group);
                    result.add(group);
                }
                group.add(event);
            }
        }
        return result;
    }

    /**
     * When a group may be claimed. A keyed group waits for a quiet window (no new same-key event for
     * {@code quietWindow}) so that a burst becomes one run, but never longer than {@code maxWait} from
     * its first event. A group at a batch cap, a group with an interrupt and an event without a key do
     * not wait.
     */
    private static Instant eligibleAt(List<FreshEvent> group, String key, boolean interrupt, Lane lane,
            InboxLimits limits) {
        FreshEvent first = group.getFirst();
        if (key == null || interrupt) {
            return first.receivedAt();
        }
        long bytes = 0;
        for (int i = 0; i < group.size(); i++) {
            bytes += group.get(i).payloadBytes();
            if (i + 1 >= limits.maxBatchEvents() || bytes >= limits.maxBatchBytes()) {
                return group.get(i).receivedAt(); // the batch was full the moment this event arrived
            }
        }
        Instant quiet = group.getLast().receivedAt().plus(limits.quietWindow(lane));
        Instant longest = first.receivedAt().plus(limits.maxWait(lane));
        return quiet.isBefore(longest) ? quiet : longest;
    }

    /** First come, first counted: events in seq order up to the caps. The head is always taken. */
    private static List<FreshEvent> batch(List<FreshEvent> group, InboxLimits limits) {
        List<FreshEvent> batch = new ArrayList<>();
        long bytes = 0;
        for (FreshEvent event : group) {
            if (!batch.isEmpty()
                    && (batch.size() >= limits.maxBatchEvents() || bytes + event.payloadBytes() > limits.maxBatchBytes())) {
                break;
            }
            batch.add(event);
            bytes += event.payloadBytes();
        }
        return List.copyOf(batch);
    }
}
