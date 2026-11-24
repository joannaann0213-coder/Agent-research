package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.agent.server.support.SimulatedCrash;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.inbox.server.sweep.LeaseSweeper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * What becomes of a turn when something breaks: the worker dies, the agent platform fails or
 * answers late, a session ends or is left busy. Whatever happens, a message is given to the agent
 * once and answered at most once, and the user's next message is answered as if nothing had
 * happened.
 */
class TurnRecoveryIT extends AgentTestBase {

    private static final String USER = "u42";

    private TestApi acme;

    @BeforeEach
    void anAgentThatCountsWhatItIsTold() {
        acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say(counted(context)));
    }

    /** "Message n: text", where n is how many messages the session has been given: a message given twice shows. */
    private static String counted(ModelContext context) {
        return "Message " + context.userMessages().size() + ": " + context.userMessage().lines().reduce((a, b) -> b).orElse("");
    }

    // ================================================================ a worker dies

    @ParameterizedTest
    @EnumSource(value = Point.class, mode = EnumSource.Mode.EXCLUDE, names = {"HANDED_OVER", "TOOLS_RUN", "ANSWERS_SENT", "PARKING"})
    void aWorkerThatDiesAnywhereInATurnIsReplaced_andNothingHappensTwice(Point point) {
        userSays(acme, USER, "hello");
        deliver();
        hooks.crashAt(point);

        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
        assertThat(worker.runOnce()).as("until its lease runs out, the run is the dead worker's").isFalse();
        leaseRunsOut();
        assertThat(worker.runOnce()).isTrue();

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(givenToTheAgent()).containsExactly("hello");
        assertThat(runs()).containsExactly("done attempt 2");
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'done' AND outcome = 'replied' AND epoch = 2")).isEqualTo(1);
        assertThat(cma.sessionIds()).as("only a worker that died right after making a session leaves one behind, unused")
                .hasSize(point == Point.SESSION_CREATED ? 2 : 1);

        userSays(acme, USER, "and again");
        assertThat(settle()).isEqualTo(1);
        assertThat(repliesTo(acme, USER)).as("the conversation goes on in the same session")
                .containsExactly("Message 1: hello", "Message 2: and again");
        assertThat(count("SELECT count(*) FROM thread WHERE generation = 1")).isEqualTo(1);
    }

    @Test
    void aWorkerThatLostItsLeaseWithoutKnowingCannotWriteAfterItsSuccessor() {
        userSays(acme, USER, "hello");
        deliver();
        AtomicBoolean successorWorked = new AtomicBoolean();
        hooks.once(Point.INPUT_RECORDED, run -> {
            // The worker stalls here for longer than its lease. The Inbox gives the run to another one, which ends it.
            leaseRunsOut();
            successorWorked.set(worker.runOnce());
        });

        assertThat(worker.runOnce()).as("the first worker wakes up, reads the reply, and finds the turn is not its any more").isTrue();

        assertThat(successorWorked).isTrue();
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(givenToTheAgent()).containsExactly("hello");
        assertThat(runs()).containsExactly("done attempt 2");
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'done' AND epoch = 2")).isEqualTo(1);
    }

    @Test
    void aMessageThatArrivesDuringATurnIsNotPushedIntoIt_itGetsATurnOfItsOwn() {
        userSays(acme, USER, "one");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> {
            userSays(acme, USER, "two");
            relay.runOnce();
        });

        assertThat(worker.runOnce()).isTrue();
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: one");

        assertThat(settle()).isEqualTo(1);
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: one", "Message 2: two");
        assertThat(runs()).containsExactly("done attempt 1", "done attempt 1");
    }

    @Test
    void aReplyThatIsReadyIsStored_evenWhenTheNextWorkerComesAfterTheTurnsDeadline() {
        userSays(acme, USER, "hello");
        deliver();
        hooks.crashAt(Point.INPUT_RECORDED);
        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);

        clock.advance(Duration.ofMinutes(11));
        assertThat(EmbeddedInbox.bean(LeaseSweeper.class).runOnce()).isEqualTo(1);
        assertThat(worker.runOnce()).isTrue();

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(runs()).containsExactly("done attempt 2");
    }

    // ================================================================ a run dies

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void afterARunThatDiedInTheMiddle_theNextReplyAnswersTheNextMessage(boolean sessionStillWorking) {
        userSays(acme, USER, "one");
        settle();
        if (sessionStillWorking) {
            cma.hold(cma.sessionIds().getFirst());
            cma.interruptsHaveIds(false);
        }
        userSays(acme, USER, "two");
        deliver();
        dieAtEveryAttempt(Point.INPUT_RECORDED);
        assertThat(runs()).containsExactly("done attempt 1", "dead attempt 5");

        userSays(acme, USER, "three");
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).as("the reply is to \"three\", whatever the session still held for \"two\"")
                .containsExactly("Message 1: one", "Message 3: three");
        assertThat(givenToTheAgent()).containsExactly("one", "two", "three");
        assertThat(interruptsSent()).as("a session that still works is stopped first").isEqualTo(sessionStillWorking ? 1 : 0);
        assertThat(cma.sessionIds()).hasSize(1);
    }

    @Test
    void aSessionLeftWaitingForAnAnswerNobodyWillGiveIsStoppedBeforeTheNextMessageGoesIn() {
        agentWithATicketSystemThatAsks();
        cma.brain(context -> context.userMessage().startsWith("close") && context.step() == 0
                ? ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}")) : say(counted(context)));
        userSays(acme, USER, "close ticket T-1");
        deliver();
        dieAtEveryAttempt(Point.INPUT_RECORDED);

        userSays(acme, USER, "never mind");
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Message 2: never mind");
        assertThat(interruptsSent()).isEqualTo(1);
        assertThat(cma.executions()).as("the ticket was not closed").isEmpty();
        assertThat(cma.sessionIds()).as("the session was stopped, not replaced").hasSize(1);
    }

    @Test
    void aTurnThatIsDeliveredAgainPutsTheCallThatStillWaitsBeforeTheApprover_once() {
        agentWithATicketSystemThatAsks();
        cma.brain(context -> context.step() == 0 ? ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}"))
                : say(context.toolResults().getFirst().error() ? "Not closed: " + context.toolResults().getFirst().text() : "Closed."));
        userSays(acme, USER, "close ticket T-1");
        deliver();
        hooks.crashAt(Point.INPUT_RECORDED);
        assertThatThrownBy(() -> worker.runOnce()).as("the session already waits for a permission when the worker dies")
                .isInstanceOf(SimulatedCrash.class);
        leaseRunsOut();

        assertThat(worker.runOnce()).isTrue();

        assertThat(runs()).containsExactly("suspended attempt 2");
        String action = jdbc.sql("SELECT action_id FROM action WHERE status = 'pending'").query(String.class).single();
        acme.post("/v1/users/u42/actions/" + action + "/resolve", new ResolveActionRequest("approve", null, null)).as(Object.class);
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Closed.");
        assertThat(cma.count(Operation.SEND)).as("the message, and one answer").isEqualTo(2);
        assertThat(cma.executions()).hasSize(1);
        assertThat(runs()).containsExactly("done attempt 2");
    }

    @Test
    void theTurnOfARunThatTheInboxGaveUpIsClosed_andOneThatCanStillBeDeliveredIsLeftAlone() {
        userSays(acme, USER, "hello");
        deliver();
        hooks.crashAt(Point.INPUT_RECORDED);
        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);

        clock.advance(Duration.ofMinutes(3));
        assertThat(sweeper.runOnce()).as("its lease has run out, but the Inbox will hand the run out again").isZero();
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'open'")).isEqualTo(1);

        hooks.crashAlwaysAt(Point.INPUT_RECORDED);
        for (int attempt = 2; attempt <= 5; attempt++) {
            assertThat(EmbeddedInbox.bean(LeaseSweeper.class).runOnce()).isEqualTo(1);
            assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
            clock.advance(Duration.ofSeconds(61));
        }
        assertThat(EmbeddedInbox.bean(LeaseSweeper.class).runOnce()).isEqualTo(1);
        assertThat(runs()).containsExactly("dead attempt 5");
        assertThat(sweeper.runOnce()).as("touched a minute ago: not yet").isZero();

        clock.advance(Duration.ofMinutes(2));
        assertThat(sweeper.runOnce()).isEqualTo(1);

        assertThat(jdbc.sql("SELECT state || ': ' || error FROM turn").query(String.class).single())
                .startsWith("failed: the run ended in the Inbox as dead (attempts_exhausted)");
        assertThat(sweeper.runOnce()).isZero();
    }

    // ================================================================ the platform fails

    @Test
    void whenTheAnswerToASendIsLost_theSessionIsAskedWhetherTheMessageArrived_beforeAnythingIsSentAgain() {
        cma.failNextAfterEffect(Operation.SEND, CmaException.connection("connection reset", null));
        userSays(acme, USER, "hello");

        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(cma.count(Operation.SEND)).as("it had arrived: not sent again").isEqualTo(1);
        assertThat(runs()).containsExactly("done attempt 1");
    }

    @Test
    void whenASendDidNotArrive_itIsSentAgain() {
        cma.failNext(Operation.SEND, CmaException.connection("connection refused", null));
        userSays(acme, USER, "hello");

        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(cma.count(Operation.SEND)).isEqualTo(2);
        assertThat(givenToTheAgent()).containsExactly("hello");
    }

    @Test
    void aPlatformThatFailsForAMomentIsAskedAgain_andTheInboxNeverKnows() {
        cma.failNext(Operation.CREATE_SESSION, CmaException.overloaded());
        cma.failNext(Operation.CREATE_SESSION, CmaException.rateLimited(Duration.ofSeconds(5)));
        userSays(acme, USER, "hello");
        deliver();
        Instant start = clock.now();

        assertThat(worker.runOnce()).isTrue();

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(cma.count(Operation.CREATE_SESSION)).isEqualTo(3);
        assertThat(Duration.between(start, clock.now())).as("a second, then the five the platform asked for")
                .isEqualTo(Duration.ofSeconds(6));
        assertThat(runs()).containsExactly("done attempt 1");
    }

    @Test
    void aPlatformThatKeepsFailingSendsTheRunBackToTheInbox_whichHandsItOutAgainLater() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            cma.failNext(Operation.CREATE_SESSION, CmaException.overloaded());
        }
        userSays(acme, USER, "hello");
        deliver();

        assertThat(worker.runOnce()).isTrue();
        assertThat(runs()).containsExactly("queued attempt 2");
        assertThat(worker.runOnce()).as("held back for a while").isFalse();
        assertThat(repliesTo(acme, USER)).isEmpty();

        clock.advance(Duration.ofSeconds(30));
        assertThat(worker.runOnce()).isTrue();
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(runs()).containsExactly("done attempt 2");
    }

    @Test
    void anOutageThatOutlastsEveryAttemptCostsAMessageItsAnswer_butNotTheNextMessage() {
        for (int call = 1; call <= 20; call++) {
            cma.failNext(Operation.CREATE_SESSION, CmaException.overloaded());
        }
        userSays(acme, USER, "one");
        deliver();
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(worker.runOnce()).isTrue();
            clock.advance(Duration.ofSeconds(30));
        }
        assertThat(worker.runOnce()).isFalse();
        assertThat(runs()).containsExactly("dead attempt 5");
        assertThat(cma.count(Operation.CREATE_SESSION)).isEqualTo(20);

        userSays(acme, USER, "two");
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: two");
        clock.advance(Duration.ofMinutes(2));
        assertThat(sweeper.runOnce()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'open'")).isZero();
    }

    @Test
    void aStreamThatBreaksInTheMiddleOfATurnDoesNotLoseTheReply() {
        cma.brain(context -> {
            cma.dropStreams(context.sessionId());
            return say(counted(context));
        });
        userSays(acme, USER, "hello");

        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(cma.count(Operation.OPEN_STREAM)).isEqualTo(2);
        assertThat(runs()).containsExactly("done attempt 1");
    }

    // ================================================================ a session ends

    @Test
    void aSessionThatHasEndedIsReplaced_andTheNewOneIsToldWhatWasSaidBefore() {
        userSays(acme, USER, "my order is 1234");
        settle();
        String first = cma.sessionIds().getFirst();
        cma.terminate(first);

        userSays(acme, USER, "where is it?");
        assertThat(settle()).isEqualTo(1);

        assertThat(cma.sessionIds()).hasSize(2);
        String second = cma.sessionIds().get(1);
        assertThat(cma.userMessages(second)).hasSize(1).first().asString()
                .contains("User: my order is 1234\n", "You: Message 1: my order is 1234\n")
                .endsWith("\n\nwhere is it?");
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: my order is 1234", "Message 1: where is it?");
        assertThat(runs()).as("replaced within the turn").containsExactly("done attempt 1", "done attempt 1");
        assertThat(cma.sessionSpec(second).metadata()).containsEntry("generation", "2");

        userSays(acme, USER, "thanks");
        settle();
        assertThat(cma.userMessages(second)).as("it is told once").containsExactly(cma.userMessages(second).getFirst(), "thanks");
        assertThat(count("SELECT count(*) FROM thread WHERE generation = 2")).isEqualTo(1);
    }

    @Test
    void aSessionThePlatformNoLongerKnowsIsReplacedToo() {
        userSays(acme, USER, "one");
        settle();
        cma.failNext(Operation.GET_SESSION, CmaException.notFound("no such session"));

        userSays(acme, USER, "two");
        assertThat(settle()).isEqualTo(1);

        assertThat(cma.sessionIds()).hasSize(2);
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: one", "Message 1: two");
    }

    @Test
    void aSessionThatEndsInTheMiddleOfATurnIsReplaced_andTheMessageIsAnsweredInTheNewOne() {
        AtomicBoolean first = new AtomicBoolean(true);
        cma.brain(context -> {
            if (first.getAndSet(false)) {
                cma.terminate(context.sessionId());
            }
            return say(counted(context));
        });
        userSays(acme, USER, "hello");

        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(cma.sessionIds()).hasSize(2);
        assertThat(cma.userMessages(cma.sessionIds().get(1))).as("nothing was said before: nothing to tell").containsExactly("hello");
        assertThat(runs()).containsExactly("done attempt 1");
    }

    @Test
    void aTurnThePlatformGaveUpOnIsTriedInANewSession_andIfThatFailsTooAgainLater() {
        AtomicInteger failures = new AtomicInteger(3);
        cma.brain(context -> failures.getAndDecrement() > 0 ? ModelStep.stop(StopReason.RETRIES_EXHAUSTED) : say(counted(context)));
        userSays(acme, USER, "hello");
        deliver();

        assertThat(worker.runOnce()).isTrue();
        assertThat(cma.sessionIds()).as("the first session, and one replacement").hasSize(2);
        assertThat(runs()).containsExactly("queued attempt 2");
        assertThat(repliesTo(acme, USER)).isEmpty();

        clock.advance(Duration.ofSeconds(30));
        assertThat(worker.runOnce()).isTrue();

        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: hello");
        assertThat(cma.sessionIds()).hasSize(4);
        assertThat(runs()).containsExactly("done attempt 2");
        assertThat(count("SELECT count(*) FROM thread WHERE generation = 4")).isEqualTo(1);
    }

    @Test
    void aRefusalEndsTheTurnWithoutAReply_andTheConversationGoesOn() {
        cma.brain(context -> context.userMessage().equals("something it will not do") ? ModelStep.stop(StopReason.REFUSAL)
                : say(counted(context)));
        userSays(acme, USER, "something it will not do");

        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).isEmpty();
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'done' AND outcome = 'refused'")).isEqualTo(1);
        assertThat(runs()).containsExactly("done attempt 1");

        userSays(acme, USER, "something else");
        settle();
        assertThat(repliesTo(acme, USER)).containsExactly("Message 2: something else");
        assertThat(cma.sessionIds()).hasSize(1);
    }

    // ================================================================ a turn takes too long

    @Test
    void aTurnThatTakesTooLongIsStoppedAndFails_andTheNextMessageIsAnsweredAsUsual() {
        userSays(acme, USER, "one");
        settle();
        cma.hold(cma.sessionIds().getFirst());
        userSays(acme, USER, "two");
        deliver();
        Instant start = clock.now();

        assertThat(worker.runOnce()).isTrue();

        assertThat(Duration.between(start, clock.now())).isBetween(Duration.ofMinutes(10), Duration.ofMinutes(10).plusSeconds(5));
        assertThat(runs()).containsExactly("done attempt 1", "failed attempt 1");
        assertThat(jdbc.sql("SELECT error FROM turn WHERE state = 'failed'").query(String.class).single())
                .isEqualTo("the turn took longer than PT10M");
        assertThat(interruptsSent()).isEqualTo(1);
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: one");
        assertThat(worker.runOnce()).as("a failed run is not handed out again").isFalse();

        userSays(acme, USER, "three");
        assertThat(settle()).isEqualTo(1);
        assertThat(repliesTo(acme, USER)).containsExactly("Message 1: one", "Message 3: three");
        assertThat(interruptsSent()).as("the session had stopped: nothing to stop before the next turn").isEqualTo(1);
    }

    // ================================================================ helpers

    /** Every worker that takes the run dies at {@code point}, until the Inbox gives the run up. */
    private void dieAtEveryAttempt(Point point) {
        hooks.crashAlwaysAt(point);
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
            leaseRunsOut();
        }
        hooks.reset();
        assertThat(worker.runOnce()).as("the Inbox has given the run up").isFalse();
    }

    private long interruptsSent() {
        return cma.calls().stream().filter(call -> call.operation() == Operation.SEND)
                .filter(call -> ((List<?>) call.argument()).stream().anyMatch(Outgoing.Interrupt.class::isInstance)).count();
    }

    private void agentWithATicketSystemThatAsks() {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null,
                List.of(new McpServerConfig("tickets", "https://mcp.acme.example/tickets", "always_ask", "app")))).as(AgentProfileView.class);
    }
}
