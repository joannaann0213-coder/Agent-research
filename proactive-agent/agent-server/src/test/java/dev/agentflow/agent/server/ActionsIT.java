package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.ActionView;
import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.agent.server.support.SimulatedCrash;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.inbox.server.sweep.SuspendSweeper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/**
 * What the agent waits for people to decide: a tool call that needs an approval, a question to
 * the user. The turn stands still, its run suspended in the Inbox, until somebody decides, until
 * nobody did in time, or until something new happens on its line. Whichever comes first decides,
 * and a decision is never undone.
 */
class ActionsIT extends AgentTestBase {

    private static final String USER = "u42";

    private TestApi acme;

    @BeforeEach
    void anApplication() {
        acme = app(ACME);
    }

    // ================================================================ approvals

    @Test
    void anApprovalStopsTheTurnUntilTheApplicationDecides_andTheToolRunsOnce() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");

        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).as("no reply while it waits").isEmpty();
        assertThat(runs()).containsExactly("suspended attempt 1");
        ActionView action = onlyPending(acme);
        assertThat(action.kind()).isEqualTo("approval");
        assertThat(action.status()).isEqualTo("pending");
        assertThat(action.approver()).isEqualTo("app");
        assertThat(action.conversationId()).isEqualTo("default");
        assertThat(action.tool().server()).isEqualTo("tickets");
        assertThat(action.tool().name()).isEqualTo("close_ticket");
        assertThat(action.tool().input().path("id").asString()).isEqualTo("T-1");
        assertThat(action.expiresAt()).isEqualTo(action.createdAt().plus(Duration.ofHours(24)));
        assertThat(cma.executions()).isEmpty();

        ActionView decided = resolve(acme, action.actionId(), "approve", null, null).as(ActionView.class);
        assertThat(decided.status()).isEqualTo("approved");
        assertThat(decided.resolvedBy()).isEqualTo("app");
        assertThat(decided.resolvedAt()).isEqualTo(clock.now());
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Closed T-1.");
        assertThat(cma.executions()).extracting(execution -> execution.input().path("id").asString()).containsExactly("T-1");
        assertThat(cma.count(Operation.SEND)).as("the message, and the approval").isEqualTo(2);
        assertThat(runs()).containsExactly("done attempt 1");
        assertThat(pending(acme)).isEmpty();

        assertThat(resolve(acme, action.actionId(), "approve", null, null).as(ActionView.class).status())
                .as("the same decision again is harmless").isEqualTo("approved");
        TestApi.Reply otherwise = resolve(acme, action.actionId(), "deny", null, null);
        assertThat(otherwise.status()).isEqualTo(409);
        assertThat(otherwise.code()).isEqualTo(ProblemCodes.CONFLICT);
        assertThat(settle()).as("a decision that was refused wakes nothing").isZero();
    }

    @Test
    void aDenialTellsTheAgentWhy() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();

        ActionView denied = resolve(acme, onlyPending(acme).actionId(), "deny", null, "not during the release freeze")
                .as(ActionView.class);
        settle();

        assertThat(denied.status()).isEqualTo("denied");
        assertThat(denied.note()).isEqualTo("not during the release freeze");
        assertThat(repliesTo(acme, USER))
                .containsExactly("Not closed: Denied by the application: not during the release freeze");
        assertThat(cma.executions()).isEmpty();
    }

    @Test
    void theUserDecidesWhatIsTheirsToDecide_andNothingElse() {
        agentWithTickets("user");
        TestApi alice = user(acme, "alice");
        TestApi bob = user(acme, "bob");
        userSays(alice, "alice", "close ticket T-1");
        settle();

        ActionView action = onlyPending(alice, "alice");
        assertThat(action.approver()).isEqualTo("user");
        assertThat(bob.get("/v1/users/alice/actions").status()).isEqualTo(403);
        assertThat(bob.get("/v1/users/alice/actions/" + action.actionId()).status()).isEqualTo(403);
        assertThat(bob.post("/v1/users/alice/actions/" + action.actionId() + "/resolve", new ResolveActionRequest("approve", null, null))
                .status()).isEqualTo(403);
        assertThat(bob.post("/v1/users/bob/actions/" + action.actionId() + "/resolve", new ResolveActionRequest("approve", null, null))
                .status()).as("an action belongs to its user").isEqualTo(404);

        ActionView approved = alice.post("/v1/users/alice/actions/" + action.actionId() + "/resolve",
                new ResolveActionRequest("approve", null, null)).as(ActionView.class);
        settle();

        assertThat(approved.resolvedBy()).isEqualTo("user");
        assertThat(repliesTo(alice, "alice")).containsExactly("Closed T-1.");
    }

    @Test
    void aUsersClientDoesNotSeeWhatTheApplicationDecides() {
        agentWithTickets("app");
        TestApi alice = user(acme, "alice");
        userSays(alice, "alice", "close ticket T-1");
        settle();
        String actionId = onlyPending(acme, "alice").actionId();

        assertThat(alice.get("/v1/users/alice/actions").as(ActionPage.class).actions()).isEmpty();
        TestApi.Reply look = alice.get("/v1/users/alice/actions/" + actionId);
        assertThat(look.status()).isEqualTo(404);
        assertThat(look.code()).isEqualTo(ProblemCodes.NOT_FOUND);
        assertThat(alice.post("/v1/users/alice/actions/" + actionId + "/resolve", new ResolveActionRequest("approve", null, null))
                .status()).isEqualTo(404);
        assertThat(acme.get("/v1/users/alice/actions/" + actionId).as(ActionView.class).status()).isEqualTo("pending");
    }

    @Test
    void twoApprovalsAndAToolCallAtOnce_eachGetsItsOwnOutcome() {
        agentWithTickets("app");
        List<String> results = new ArrayList<>();
        cma.brain(context -> {
            context.toolResults().forEach(result -> results.add(result.tool() + (result.error() ? " failed: " : ": ") + result.text()));
            return context.step() == 0
                    ? ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}"), mcp("tickets", "close_ticket", "{\"id\": \"T-2\"}"),
                            custom("read_events", "{}"))
                    : say("Done with " + results.size() + " results.");
        });
        userSays(acme, USER, "close T-1 and T-2");
        settle();

        List<ActionView> waiting = pending(acme);
        assertThat(waiting).extracting(action -> action.tool().input().path("id").asString()).containsExactly("T-1", "T-2");
        assertThat(runs()).containsExactly("suspended attempt 1");

        resolve(acme, waiting.get(0).actionId(), "approve", null, null).as(ActionView.class);
        assertThat(settle()).isEqualTo(1);
        assertThat(cma.executions()).as("an approved call runs at once").hasSize(1);
        assertThat(runs()).as("still waiting for the other one").containsExactly("suspended attempt 1");
        assertThat(repliesTo(acme, USER)).isEmpty();

        resolve(acme, waiting.get(1).actionId(), "deny", null, null).as(ActionView.class);
        assertThat(settle()).isEqualTo(1);

        assertThat(cma.executions()).extracting(execution -> execution.input().path("id").asString()).containsExactly("T-1");
        assertThat(results).containsExactlyInAnyOrder("read_events: {\"messages\":[]}", "close_ticket: ok",
                "close_ticket failed: Denied by the application.");
        assertThat(repliesTo(acme, USER)).containsExactly("Done with 3 results.");
        assertThat(runs()).containsExactly("done attempt 1");
    }

    // ================================================================ questions

    @Test
    void aQuestionInAConversationIsAnsweredByTheUsersNextMessage() {
        defaultAgent(acme);
        cma.brain(context -> context.step() == 0
                ? ModelStep.of(custom("ask_user", "{\"question\": \"Which order do you mean?\", \"options\": [\"1234\", \"5678\"]}"))
                : say("Looking at order " + context.resultOf("ask_user").text() + "."));
        userSays(acme, USER, "where is my order?");
        settle();

        ActionView question = onlyPending(acme);
        assertThat(question.kind()).isEqualTo("question");
        assertThat(question.approver()).isEqualTo("user");
        assertThat(question.question()).isEqualTo("Which order do you mean?");
        assertThat(question.options()).containsExactly("1234", "5678");
        assertThat(messagesOf(acme, USER)).extracting(MessageView::role, MessageView::kind, MessageView::text, MessageView::actionId)
                .containsExactly(tuple("user", "message", "where is my order?", null),
                        tuple("agent", "question", "Which order do you mean?", question.actionId()));

        MessageAccepted answer = userSays(acme, USER, "1234");
        assertThat(answer.actionId()).isEqualTo(question.actionId());
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Which order do you mean?", "Looking at order 1234.");
        assertThat(cma.userMessages(cma.sessionIds().getFirst())).as("the answer went to the agent as the answer, not as a message")
                .containsExactly("where is my order?");
        assertThat(acme.get("/v1/users/u42/actions/" + question.actionId()).as(ActionView.class))
                .extracting(ActionView::status, ActionView::answer, ActionView::resolvedBy)
                .containsExactly("answered", "1234", "user");
        assertThat(runs()).as("the answer started no run of its own").containsExactly("done attempt 1");
        assertThat(messagesOf(acme, USER).get(2).actionId()).isEqualTo(question.actionId());
    }

    @Test
    void aQuestionFromTheBackgroundCanBeAnsweredThroughItsAction_andTheAnswerShowsInTheConversation() {
        defaultAgent(acme);
        cma.brain(context -> {
            if (context.step() == 0) {
                return ModelStep.of(custom("ask_user", "{\"question\": \"Order 1234 is late. A refund, or do you wait?\"}"));
            }
            ToolOutcome answer = context.resultOf("ask_user");
            if (answer != null) {
                return ModelStep.of(custom("notify_user", text("OK: " + answer.text() + ". Done.")));
            }
            return say("Handled.");
        });
        acme.post("/v1/users/u42/events", new PostEventRequest(null, "order.delayed", "order-1234", null, null, null, null))
                .as(EventAccepted.class);
        settle();
        ActionView question = onlyPending(acme);

        ActionView answered = resolve(acme, question.actionId(), null, "a refund, please", null).as(ActionView.class);
        settle();

        assertThat(answered.answer()).isEqualTo("a refund, please");
        assertThat(messagesOf(acme, USER)).extracting(MessageView::role, MessageView::kind, MessageView::text)
                .containsExactly(tuple("agent", "question", "Order 1234 is late. A refund, or do you wait?"),
                        tuple("user", "message", "a refund, please"),
                        tuple("agent", "notification", "OK: a refund, please. Done."));
        assertThat(jdbc.sql("SELECT outcome FROM turn").query(String.class).single()).isEqualTo("notified");
    }

    @Test
    void aMessageAnswersTheLatestQuestionOfItsConversation_evenOneFromTheBackground() {
        defaultAgent(acme);
        cma.brain(context -> {
            if (context.sessionMetadata().get("line").startsWith("task:")) {
                return context.step() == 0 ? ModelStep.of(custom("ask_user", "{\"question\": \"A refund?\"}"))
                        : say("Background got: " + context.resultOf("ask_user").text());
            }
            return say("You said: " + context.userMessage().lines().reduce((first, second) -> second).orElse(""));
        });
        acme.post("/v1/users/u42/events", new PostEventRequest(null, "order.delayed", "order-1234", null, null, null, null))
                .as(EventAccepted.class);
        settle();

        assertThat(userSays(acme, USER, "yes please").actionId()).isNotNull();
        settle();
        userSays(acme, USER, "thanks");
        settle();

        assertThat(jdbc.sql("SELECT note FROM turn WHERE merge_key = 'task:order-1234'").query(String.class).single())
                .isEqualTo("Background got: yes please");
        assertThat(repliesTo(acme, USER)).containsExactly("A refund?", "You said: thanks");
        String chat = cma.sessionIds().stream().filter(session -> cma.sessionSpec(session).metadata().get("line").equals("conv:default"))
                .findFirst().orElseThrow();
        assertThat(cma.userMessages(chat)).as("the conversation is told that the background exchanged messages with the user")
                .hasSize(1).first().asString().contains("has exchanged 2 messages with the user").endsWith("thanks");
    }

    @Test
    void aQuestionCanBeDeclined_andTheAgentIsTold() {
        defaultAgent(acme);
        cma.brain(context -> context.step() == 0 ? ModelStep.of(custom("ask_user", "{\"question\": \"Your phone number?\"}"))
                : say(context.resultOf("ask_user").text()));
        userSays(acme, USER, "call me");
        settle();

        resolve(acme, onlyPending(acme).actionId(), "deny", null, "I'd rather not say").as(ActionView.class);
        settle();

        assertThat(repliesTo(acme, USER).getLast()).isEqualTo("Not answered: the application declined the question: I'd rather not say");
    }

    @Test
    void aQuestionTheAgentDidNotPutRightIsReturnedToIt_andNobodyIsAsked() {
        defaultAgent(acme);
        cma.brain(context -> switch (context.step()) {
            case 0 -> ModelStep.of(custom("ask_user", "{}"));
            case 1 -> ModelStep.of(custom("ask_user", "{\"question\": \"Which?\", \"options\": [\"" + "x".repeat(201) + "\"]}"));
            default -> say(context.resultOf("ask_user").text());
        });
        userSays(acme, USER, "hello");

        settle();

        assertThat(repliesTo(acme, USER)).containsExactly("Not asked: each option is a text of at most 200 characters.");
        assertThat(count("SELECT count(*) FROM action")).isZero();
    }

    // ================================================================ decisions and what races them

    @Test
    void approvedBeforeTheUserWritesAgain_staysApproved_andTheMessageComesNext() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();

        resolve(acme, onlyPending(acme).actionId(), "approve", null, null).as(ActionView.class);
        userSays(acme, USER, "and thanks");
        assertThat(settle()).isEqualTo(2);

        assertThat(cma.executions()).hasSize(1);
        assertThat(repliesTo(acme, USER)).containsExactly("Closed T-1.", "You said: and thanks");
    }

    @Test
    void aMessageBeforeTheDecision_withdrawsTheApproval_andTheDecisionComesTooLate() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();
        String actionId = onlyPending(acme).actionId();

        userSays(acme, USER, "never mind");
        TestApi.Reply late = resolve(acme, actionId, "approve", null, null);
        assertThat(settle()).isEqualTo(2);

        assertThat(late.status()).isEqualTo(409);
        assertThat(late.body().path("detail").asString()).contains("withdrawn", "the user wrote a new message");
        assertThat(cma.executions()).isEmpty();
        assertThat(repliesTo(acme, USER)).containsExactly(
                "Not closed: Not approved: the user wrote a new message before this was decided; it comes in the next turn.",
                "You said: never mind");
        assertThat(acme.get("/v1/users/u42/actions/" + actionId).as(ActionView.class).resolvedBy()).isEqualTo("service");
    }

    @Test
    void aNewMessageWithdrawsOnlyWhatStillWaits() {
        agentWithTickets("app");
        cma.brain(context -> {
            if (context.step() == 0 && context.userMessage().startsWith("close")) {
                return ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}"), mcp("tickets", "close_ticket", "{\"id\": \"T-2\"}"));
            }
            return say(context.toolResults().stream().map(result -> result.error() ? "failed: " + result.text() : "ok")
                    .collect(Collectors.joining(" / ", "Results: ", "")));
        });
        userSays(acme, USER, "close T-1 and T-2");
        settle();
        List<ActionView> waiting = pending(acme);
        resolve(acme, waiting.get(0).actionId(), "approve", null, null).as(ActionView.class);

        userSays(acme, USER, "hurry up");
        settle();

        assertThat(acme.get("/v1/users/u42/actions/" + waiting.get(0).actionId()).as(ActionView.class).status()).isEqualTo("approved");
        assertThat(acme.get("/v1/users/u42/actions/" + waiting.get(1).actionId()).as(ActionView.class).status()).isEqualTo("withdrawn");
        assertThat(cma.executions()).extracting(execution -> execution.input().path("id").asString()).containsExactly("T-1");
    }

    @Test
    void approvedBeforeItExpires_staysApproved_evenWhenTheRingComesLate() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();
        clock.advance(Duration.ofHours(24).minusMinutes(1));
        resolve(acme, onlyPending(acme).actionId(), "approve", null, null).as(ActionView.class);

        // The ring is still on its way when the Inbox's deadline for the run passes.
        clock.advance(Duration.ofMinutes(2));
        assertThat(EmbeddedInbox.bean(SuspendSweeper.class).runOnce()).isEqualTo(1);
        assertThat(worker.runOnce()).isTrue();

        assertThat(cma.executions()).as("approved is approved").hasSize(1);
        assertThat(repliesTo(acme, USER)).containsExactly("Closed T-1.");

        long sends = cma.count(Operation.SEND);
        assertThat(settle()).as("the ring arrives after the turn ended, and is only taken note of").isEqualTo(1);
        assertThat(cma.count(Operation.SEND)).isEqualTo(sends);
        assertThat(runs()).containsExactly("done attempt 1", "done attempt 1");
    }

    @Test
    void aRingThatComesAfterItsTurnTogetherWithAMessage_isLeftOutOfTheMessagesTurn() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();
        clock.advance(Duration.ofHours(24).minusMinutes(1));
        resolve(acme, onlyPending(acme).actionId(), "approve", null, null).as(ActionView.class);
        clock.advance(Duration.ofMinutes(2));
        EmbeddedInbox.bean(SuspendSweeper.class).runOnce();
        worker.runOnce();

        userSays(acme, USER, "great");
        assertThat(settle()).as("the ring and the message in one run").isEqualTo(1);

        assertThat(repliesTo(acme, USER)).containsExactly("Closed T-1.", "You said: great");
        assertThat(cma.userMessages(cma.sessionIds().getFirst())).containsExactly("close ticket T-1", "great");
    }

    @Test
    void nobodyDecidedInTime_theAgentIsTold_andALateDecisionIsRefused() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();
        String actionId = onlyPending(acme).actionId();

        clock.advance(Duration.ofHours(24).plusSeconds(2));
        assertThat(EmbeddedInbox.bean(SuspendSweeper.class).runOnce()).isEqualTo(1);
        assertThat(worker.runOnce()).isTrue();

        assertThat(repliesTo(acme, USER)).containsExactly("Not closed: Not approved: nobody decided in time.");
        TestApi.Reply late = resolve(acme, actionId, "approve", null, null);
        assertThat(late.status()).isEqualTo(409);
        assertThat(late.body().path("detail").asString()).contains("expired");
        assertThat(cma.executions()).isEmpty();
    }

    @Test
    void aDecisionAfterTheDeadlineIsRefused_evenBeforeTheTurnNoticed() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();
        String actionId = onlyPending(acme).actionId();

        clock.advance(Duration.ofHours(25));
        TestApi.Reply late = resolve(acme, actionId, "approve", null, null);

        assertThat(late.status()).isEqualTo(409);
        assertThat(acme.get("/v1/users/u42/actions/" + actionId).as(ActionView.class).status()).isEqualTo("expired");
        assertThat(pending(acme)).isEmpty();
    }

    @Test
    void aNewEventForTheSubjectWithdrawsWhatTheSubjectWaitsFor() {
        defaultAgent(acme);
        List<String> told = new ArrayList<>();
        cma.brain(context -> {
            ToolOutcome asked = context.resultOf("ask_user");
            if (asked != null) {
                told.add(asked.text());
                return say("Moving on.");
            }
            return context.userMessages().size() == 1 ? ModelStep.of(custom("ask_user", "{\"question\": \"A refund?\"}"))
                    : say("Looked at the next event.");
        });
        event("order.delayed");
        settle();
        String actionId = onlyPending(acme).actionId();

        event("order.delivered");
        assertThat(settle()).isEqualTo(2);

        assertThat(told).containsExactly("No answer: something new happened on this subject before this was decided; it comes in "
                + "the next turn.");
        assertThat(acme.get("/v1/users/u42/actions/" + actionId).as(ActionView.class).status()).isEqualTo("withdrawn");
        assertThat(jdbc.sql("SELECT note FROM turn ORDER BY created_at, run_id").query(String.class).list())
                .containsExactly("Moving on.", "Looked at the next event.");
    }

    @Test
    void aMessageThatArrivesJustBeforeTheTurnStopsToWait_isStillAnsweredAfterIt() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "never mind"));

        assertThat(worker.runOnce()).isTrue();
        assertThat(pending(acme)).as("the message came before anything waited: nothing was withdrawn then").hasSize(1);

        settle();
        assertThat(repliesTo(acme, USER)).as("the turn that waited finishes first")
                .containsExactly("Not closed: Not approved: the user wrote a new message before this was decided; it comes in the next turn.");
        assertThat(runs()).as("the run of the message waited for its line, which cost it no attempt")
                .containsExactly("done attempt 1", "queued attempt 1");
        clock.advance(Duration.ofSeconds(2));
        settle();

        assertThat(repliesTo(acme, USER)).containsExactly(
                "Not closed: Not approved: the user wrote a new message before this was decided; it comes in the next turn.",
                "You said: never mind");
        assertThat(cma.executions()).isEmpty();
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'open'")).isZero();
    }

    // ================================================================ what breaks while it waits

    @Test
    void aWorkerThatDiesAsItStopsToWait_putsTheCallBeforeTheApproverOnce() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        deliver();
        hooks.crashAt(Point.PARKING);

        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
        leaseRunsOut();
        assertThat(worker.runOnce()).isTrue();

        assertThat(count("SELECT count(*) FROM action")).isEqualTo(1);
        assertThat(runs()).containsExactly("suspended attempt 2");
        resolve(acme, onlyPending(acme).actionId(), "approve", null, null).as(ActionView.class);
        settle();
        assertThat(repliesTo(acme, USER)).containsExactly("Closed T-1.");
        assertThat(cma.executions()).hasSize(1);
    }

    @Test
    void theTurnsTimeRunsOnlyWhileItWorks() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();

        clock.advance(Duration.ofHours(2));
        resolve(acme, onlyPending(acme).actionId(), "approve", null, null).as(ActionView.class);
        settle();

        assertThat(repliesTo(acme, USER)).containsExactly("Closed T-1.");
        assertThat(runs()).containsExactly("done attempt 1");
    }

    @Test
    void aSessionThatEndsWhileTheTurnWaits_isReplaced_andTheCallIsPutAgain() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();
        ActionView first = onlyPending(acme);
        cma.terminate(cma.sessionIds().getFirst());

        resolve(acme, first.actionId(), "approve", null, null).as(ActionView.class);
        settle();

        assertThat(cma.sessionIds()).hasSize(2);
        assertThat(cma.executions()).as("the approval was for a call of the session that ended").isEmpty();
        ActionView again = onlyPending(acme);
        assertThat(again.actionId()).isNotEqualTo(first.actionId());
        resolve(acme, again.actionId(), "approve", null, null).as(ActionView.class);
        settle();
        assertThat(repliesTo(acme, USER)).containsExactly("Closed T-1.");
        assertThat(cma.executions()).hasSize(1);
    }

    // ================================================================ the edge

    @Test
    void whatIsDecidedMustFitTheAction() {
        agentWithTickets("app");
        userSays(acme, USER, "close ticket T-1");
        settle();
        String approval = onlyPending(acme).actionId();

        assertThat(resolve(acme, approval, null, null, null).status()).isEqualTo(400);
        assertThat(resolve(acme, approval, "maybe", null, null).status()).isEqualTo(400);
        assertThat(resolve(acme, approval, "approve", "yes", null).status()).isEqualTo(400);
        assertThat(resolve(acme, approval, "approve", null, "because").status()).as("a message goes with a denial").isEqualTo(400);
        assertThat(resolve(acme, approval, "deny", null, "x".repeat(1001)).status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/u42/actions/" + approval + "/resolve", "{\"decision\": \"approve\", \"when\": \"now\"}").status())
                .isEqualTo(400);
        TestApi.Reply unknown = resolve(acme, "act_nothing", "approve", null, null);
        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.code()).isEqualTo(ProblemCodes.NOT_FOUND);
        assertThat(acme.post("/v1/users/nobody/actions/" + approval + "/resolve", new ResolveActionRequest("approve", null, null))
                .status()).isEqualTo(404);
        assertThat(acme.get("/v1/users/u42/actions?conversation_id=bad/id").status()).isEqualTo(400);
        assertThat(pending(acme)).as("nothing was decided").hasSize(1);
    }

    @Test
    void aQuestionTakesAnAnswer_notADecision() {
        defaultAgent(acme);
        cma.brain(context -> context.step() == 0 ? ModelStep.of(custom("ask_user", "{\"question\": \"Which?\"}"))
                : say(context.resultOf("ask_user").text()));
        userSays(acme, USER, "hello");
        settle();
        String question = onlyPending(acme).actionId();

        assertThat(resolve(acme, question, "approve", null, null).status()).isEqualTo(400);
        assertThat(resolve(acme, question, null, " ", null).status()).isEqualTo(400);
        assertThat(resolve(acme, question, null, "x".repeat(4001), null).status()).isEqualTo(400);
        assertThat(resolve(acme, question, "deny", "this one", null).status()).isEqualTo(400);
        assertThat(resolve(acme, question, null, "this one", null).as(ActionView.class).status()).isEqualTo("answered");
        assertThat(resolve(acme, question, null, "this one", null).as(ActionView.class).status()).as("again: the same").isEqualTo("answered");
        assertThat(resolve(acme, question, null, "that one", null).status()).as("another answer").isEqualTo(409);
    }

    @Test
    void actionsAreListedPerConversation_oldestFirst() {
        agentWithTickets("app");
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "a", null, "close ticket T-1")).as(MessageAccepted.class);
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "b", null, "close ticket T-2")).as(MessageAccepted.class);
        settle();

        assertThat(pending(acme)).extracting(ActionView::conversationId).containsExactly("a", "b");
        assertThat(acme.get("/v1/users/u42/actions?conversation_id=b").as(ActionPage.class).actions())
                .extracting(action -> action.tool().input().path("id").asString()).containsExactly("T-2");
        assertThat(acme.get("/v1/users/someone-else/actions").as(ActionPage.class).actions()).isEmpty();
    }

    // ================================================================ helpers

    /** An agent with a ticket system whose calls need an approval from {@code approver}. It closes the ticket it is told to. */
    private void agentWithTickets(String approver) {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null,
                List.of(new McpServerConfig("tickets", "https://mcp.acme.example/tickets", "always_ask", approver))))
                .as(AgentProfileView.class);
        cma.brain(this::ticketDesk);
    }

    private ModelStep ticketDesk(ModelContext context) {
        String said = context.userMessage();
        if (!said.contains("close ticket")) {
            return say("You said: " + said.lines().reduce((first, second) -> second).orElse(""));
        }
        String ticket = said.substring(said.indexOf("close ticket") + "close ticket ".length()).split("\\s")[0];
        if (context.step() == 0) {
            return ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"" + ticket + "\"}"));
        }
        ToolOutcome result = context.toolResults().getFirst();
        return say(result.error() ? "Not closed: " + result.text() : "Closed " + ticket + ".");
    }

    private List<ActionView> pending(TestApi caller) {
        return caller.get("/v1/users/" + USER + "/actions").as(ActionPage.class).actions();
    }

    private ActionView onlyPending(TestApi caller) {
        return onlyPending(caller, USER);
    }

    private ActionView onlyPending(TestApi caller, String userId) {
        List<ActionView> actions = caller.get("/v1/users/" + userId + "/actions").as(ActionPage.class).actions();
        assertThat(actions).as("the actions that wait").hasSize(1);
        return actions.getFirst();
    }

    private TestApi.Reply resolve(TestApi caller, String actionId, String decision, String answer, String message) {
        return caller.post("/v1/users/" + USER + "/actions/" + actionId + "/resolve", new ResolveActionRequest(decision, answer, message));
    }

    private void event(String type) {
        acme.post("/v1/users/u42/events", new PostEventRequest(null, type, "order-1234", null, null, null, null)).as(EventAccepted.class);
    }

    private static String text(String text) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        input.put("text", text);
        return input.toString();
    }

    @SuppressWarnings("unused")
    private static String joined(List<String> parts) {
        return parts.stream().collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unused")
    private static Instant never() {
        return Instant.MAX;
    }
}
