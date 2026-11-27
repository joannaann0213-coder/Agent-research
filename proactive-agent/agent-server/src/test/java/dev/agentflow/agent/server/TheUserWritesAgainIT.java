package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.api.StopResult;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.SimulatedCrash;
import dev.agentflow.agent.server.support.TestApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * People write the way they talk: a sentence, then the rest of it. An agent that answers each
 * message by itself answers the first before it knows what the user means. So a turn answers
 * everything the user has written that no turn has yet; and when the user writes again before a
 * main agent's answer is there, that reply is ended, and the turn of the new message answers all of
 * it in one. A reply that reaches the user was written knowing everything they had written by then.
 */
class TheUserWritesAgainIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String NOTE = "the user wrote again before you had answered";

    @Autowired
    private AgentProperties properties;

    private TestApi acme;
    /** What read_events gave the main agent in a conversation, call by call. */
    private final List<JsonNode> chatRead = new ArrayList<>();

    @BeforeEach
    void aMainAgentWithAResearcher_andAnOrdinaryAgent() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/helper", new AgentProfileRequest("Helper", null, null, "You help.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", mainAgent(null)).as(AgentProfileView.class);
        chatRead.clear();
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    @AfterEach
    void theSettingsAsTheyWere() {
        AgentProperties.Turn asShipped = new AgentProperties.Turn();
        properties.getTurn().setSupersedeReplies(asShipped.getSupersedeReplies());
        properties.getTurn().setMaxSuperseded(asShipped.getMaxSuperseded());
        properties.getTurn().setMaxMessages(asShipped.getMaxMessages());
    }

    private static AgentProfileRequest mainAgent(List<McpServerConfig> servers) {
        return new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, servers, null, null,
                new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research",
                        "Looks things up and reports what it found.")), null));
    }

    /**
     * An agent in a conversation. It answers everything the user wrote in its input, in one reply.
     * Before that it reads the news it is pointed to; "research X" hands X to the researcher, unless
     * the service says that the turn before did so already; "close ticket T" closes T in the ticket
     * system.
     */
    private ModelStep inConversation(ModelContext context) {
        String input = context.userMessage();
        List<String> said = Arrays.stream(input.split("\n\n")).filter(part -> !part.startsWith("[From the service")).toList();
        String answer = "Answer to: " + String.join(" + ", said);
        if (context.step() == 0) {
            if (input.contains("Call read_events")) {
                return ModelStep.of(custom("read_events", "{}"));
            }
            if (said.getLast().startsWith("research ") && !input.contains("do not do it again")) {
                String what = said.getLast().substring("research ".length());
                return ModelStep.of(custom("delegate_task", args("role", "researcher", "title", what, "instructions",
                        "Research " + what)));
            }
            if (said.getLast().startsWith("close ticket ")) {
                return ModelStep.of(mcp("tickets", "close_ticket", args("id", said.getLast().substring("close ticket ".length()))));
            }
            return say(answer);
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read != null) {
            JsonNode since = AgentJson.mapper().readTree(read.text());
            chatRead.add(since);
            List<String> news = new ArrayList<>();
            since.path("reports").forEach(report -> news.add(report.path("kind").asString()));
            return say(answer + ". News: " + String.join(", ", news));
        }
        ToolOutcome done = context.toolResults().getFirst();
        return say((done.error() ? "Not done. " : "Done. ") + answer);
    }

    /** The main agent in the background: reads what waits, and tells the user what a worker reports. */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            return say("Done for now.");
        }
        for (JsonNode report : AgentJson.mapper().readTree(read.text()).path("reports")) {
            if ("report".equals(report.path("kind").asString())) {
                return ModelStep.of(custom("notify_user", args("text", "About " + report.path("title").asString() + ": "
                        + report.path("text").asString())));
            }
        }
        return say("Noted.");
    }

    private ModelStep worker(ModelContext context) {
        return context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("Did it.");
    }

    // ================================================================ a reply under way is ended

    @Test
    void aNewMessageEndsTheReplyUnderWay_andOneReplyAnswersAllOfIt() {
        userSays(acme, USER, "book a table");
        deliver();
        // The agent is thinking about its answer when the user writes the rest.
        hooks.once(Point.BOUNDARY_FIXED, run -> cma.hold(cma.sessionIds().getFirst()));
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "for four, at seven"));

        assertThat(worker.runOnce()).isTrue();

        assertThat(repliesTo(acme, USER)).as("the reply that was under way is not given to the user").isEmpty();
        assertThat(outcomes()).containsExactly("superseded");
        assertThat(interrupts()).as("the session was told to stop").isEqualTo(1);
        assertThat(waiting()).as("both messages wait for a turn now").isEqualTo(2);
        assertThat(runs()).as("the turn is over: nothing holds the line").containsExactly("done attempt 1");

        untilQuiet();

        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).containsExactly(
                tuple("message", "book a table"), tuple("message", "for four, at seven"),
                tuple("reply", "Answer to: book a table + for four, at seven"));
        List<String> given = cma.userMessages(cma.sessionIds().getFirst());
        assertThat(cma.sessionIds()).as("all of it in the one session of the conversation").hasSize(1);
        assertThat(given).hasSize(2);
        assertThat(given.get(0)).isEqualTo("book a table");
        assertThat(given.get(1)).as("the agent is told what happened, and is given all that has no answer yet")
                .startsWith("[From the service that runs you, not from the user: " + NOTE)
                .contains("The answer you had begun was not shown to them", "do not do it again")
                .endsWith("\n\nbook a table\n\nfor four, at seven");
        assertThat(outcomes()).containsExactly("superseded", "replied");
        assertThat(waiting()).isZero();
        assertThat(count("SELECT superseded FROM thread WHERE merge_key = 'conv:default'")).as("an answer reached the user").isZero();
    }

    @Test
    void aReplyThatIsWrittenButNotYetStoredWhenTheUserWritesAgain_isDroppedAllTheSame() {
        userSays(acme, USER, "book a table");
        deliver();
        // The agent has written its whole answer by the time the service looks: the user's message came first.
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "for four, at seven"));

        assertThat(worker.runOnce()).isTrue();
        assertThat(repliesTo(acme, USER)).isEmpty();

        untilQuiet();
        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: book a table + for four, at seven");
        assertThat(outcomes()).containsExactly("superseded", "replied");
    }

    @Test
    void whatTheUserWritesBeforeTheInputIsSentGoesIntoTheSameTurn() {
        userSays(acme, USER, "book a table");
        deliver();
        // The turn has been handed out and is getting its session ready when the user writes the rest.
        hooks.once(Point.BOUNDARY_FIXED, run -> userSays(acme, USER, "for four, at seven"));

        assertThat(worker.runOnce()).isTrue();

        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: book a table + for four, at seven");
        assertThat(givenToTheAgent()).as("one input, and nothing had to be ended for it")
                .containsExactly("book a table\n\nfor four, at seven");
        assertThat(interrupts()).isZero();

        assertThat(untilQuiet()).as("the run of the second message").isEqualTo(1);
        assertThat(repliesTo(acme, USER)).as("finds it answered").hasSize(1);
        assertThat(outcomes()).containsExactly("replied", "absorbed");
        assertThat(givenToTheAgent()).hasSize(1);
        assertThat(runs()).containsExactly("done attempt 1", "done attempt 1");
    }

    @Test
    void whatTheAgentDidBeforeItsReplyWasEndedStands_andItIsToldNotToDoItAgain() {
        userSays(acme, USER, "research venues");
        deliver();
        // The agent has handed the work to its researcher and is writing its answer when the user writes again.
        hooks.once(Point.ANSWERS_SENT, run -> userSays(acme, USER, "somewhere quiet"));

        assertThat(worker.runOnce()).isTrue();
        assertThat(outcomes()).containsExactly("superseded");
        assertThat(count("SELECT count(*) FROM delegated_task")).as("the task is delegated").isEqualTo(1);

        untilQuiet();

        assertThat(count("SELECT count(*) FROM delegated_task")).as("and not delegated a second time").isEqualTo(1);
        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: research venues + somewhere quiet", "About venues: Did it.");
    }

    @Test
    void theNewsThatTheEndedReplyWouldHaveToldIsGivenToTheNextTurnAgain() {
        EventAccepted handed = event("order.delayed", "order-1", "research");
        deliver();
        assertThat(worker.runOnce()).as("the main agent is told that work was handed to its worker").isTrue();
        assertThat(worker.runOnce()).as("the worker does it and reports").isTrue();
        // The user asks before the main agent has got to the report, and adds to it while the agent answers.
        userSays(acme, USER, "how is my order");
        deliver();
        hooks.once(Point.ANSWERS_SENT, run -> userSays(acme, USER, "and when does it arrive"));

        assertThat(worker.runOnce()).isTrue();
        assertThat(chatRead).as("the turn read the report").hasSize(1);
        assertThat(repliesTo(acme, USER)).as("but what it wrote about it is dropped").isEmpty();

        untilQuiet();

        assertThat(chatRead).as("so the next turn is given the report again").hasSize(2);
        assertThat(chatRead.get(1).path("reports").path(0).path("task_id").asString()).isEqualTo(handed.taskId());
        assertThat(repliesTo(acme, USER)).as("and the user hears of it once")
                .containsExactly("Answer to: how is my order + and when does it arrive. News: report");
        assertThat(count("SELECT count(*) FROM task_note WHERE direction = 'to_main' AND read_at IS NULL")).isZero();
    }

    @Test
    void aTurnThatWaitsForAnApprovalIsEndedByTheUsersNewMessage() {
        acme.put("/v1/agents/default", mainAgent(List.of(new McpServerConfig("tickets", "https://mcp.acme.example/tickets",
                "always_ask", "user")))).as(AgentProfileView.class);
        userSays(acme, USER, "close ticket T-1");
        settle();
        assertThat(pendingActions()).as("the agent waits for the user's approval").isEqualTo(1);
        assertThat(runs()).containsExactly("suspended attempt 1");

        userSays(acme, USER, "sorry, I meant T-2");
        untilQuiet();

        assertThat(pendingActions()).as("nothing is left to approve").isZero();
        assertThat(cma.executions()).as("and the ticket was not closed").isEmpty();
        assertThat(outcomes()).containsExactly("superseded", "replied");
        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: close ticket T-1 + sorry, I meant T-2");
    }

    // ================================================================ where it is not, and how often

    @Test
    void anOrdinaryAgentFinishesItsReply_andAnswersTheNewMessageAfterIt() {
        sideSays("write the report");
        deliver();
        // An agent without workers does its long work in the turn itself: a word of the user does not throw it away.
        hooks.once(Point.INPUT_RECORDED, run -> sideSays("thanks"));

        assertThat(worker.runOnce()).isTrue();
        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: write the report");

        untilQuiet();
        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: write the report", "Answer to: thanks");
        assertThat(outcomes()).containsExactly("replied", "replied");
        assertThat(interrupts()).isZero();
    }

    @Test
    void withTheSettingAll_anOrdinaryAgentsReplyIsEndedToo() {
        properties.getTurn().setSupersedeReplies("all");
        sideSays("book a table");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> sideSays("for four, at seven"));

        untilQuiet();

        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: book a table + for four, at seven");
        assertThat(outcomes()).containsExactly("superseded", "replied");
    }

    @Test
    void withTheSettingNone_aMainAgentFinishesItsReplyAsWell() {
        properties.getTurn().setSupersedeReplies("none");
        userSays(acme, USER, "book a table");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "for four, at seven"));

        untilQuiet();

        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: book a table", "Answer to: for four, at seven");
        assertThat(outcomes()).containsExactly("replied", "replied");
    }

    @Test
    void onlySoManyRepliesInARowAreEnded_soThatTheUserGetsAnAnswerWhateverTheyDo() {
        properties.getTurn().setMaxSuperseded(2);
        userSays(acme, USER, "one");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "two"));
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "three"));
        // The third time the user writes into a reply, the reply is finished first.
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "four"));

        untilQuiet();

        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: one + two + three", "Answer to: four");
        assertThat(outcomes()).containsExactly("superseded", "superseded", "replied", "replied");
        assertThat(count("SELECT superseded FROM thread WHERE merge_key = 'conv:default'")).isZero();
        assertThat(givenToTheAgent().getLast()).as("the turn after an answer starts afresh").isEqualTo("four");
    }

    @Test
    void whatWaitsBeyondWhatOneTurnTakesIsAnsweredByTheNext_andEveryMessageOnce() {
        properties.getTurn().setMaxMessages(3);
        userSays(acme, USER, "one");
        deliver();
        hooks.once(Point.BOUNDARY_FIXED, run -> List.of("two", "three", "four", "five").forEach(text -> userSays(acme, USER, text)));

        untilQuiet();

        assertThat(repliesTo(acme, USER)).as("the oldest first, and the rest is not a reason to end the reply")
                .containsExactly("Answer to: one + two + three", "Answer to: four + five");
        assertThat(outcomes()).containsExactly("replied", "replied");
        assertThat(waiting()).isZero();
    }

    // ================================================================ a stop is a stop

    @Test
    void aReplyThatTheUserStopsIsNotAnsweredLater_alsoWhenTheyWriteAgain() {
        userSays(acme, USER, "write me a long essay");
        deliver();
        hooks.once(Point.BOUNDARY_FIXED, run -> cma.hold(cma.sessionIds().getFirst()));
        hooks.once(Point.INPUT_RECORDED, run -> {
            assertThat(stop().inProgress()).isTrue();
            userSays(acme, USER, "what is the time");
        });

        untilQuiet();

        assertThat(outcomes()).containsExactly("stopped", "replied");
        assertThat(repliesTo(acme, USER)).as("only what came after the stop is answered").containsExactly("Answer to: what is the time");
        assertThat(givenToTheAgent().getLast()).as("and nothing says that an answer is still owed").isEqualTo("what is the time");
    }

    @Test
    void aStopAfterTheNewMessageStopsBoth() {
        userSays(acme, USER, "write me a long essay");
        deliver();
        hooks.once(Point.BOUNDARY_FIXED, run -> cma.hold(cma.sessionIds().getFirst()));
        hooks.once(Point.INPUT_RECORDED, run -> {
            userSays(acme, USER, "about cats");
            stop();
        });

        untilQuiet();

        assertThat(outcomes()).containsExactly("stopped", "stopped");
        assertThat(repliesTo(acme, USER)).isEmpty();
        assertThat(waiting()).as("nothing waits for an answer that the user does not want").isZero();
        assertThat(givenToTheAgent()).as("the second message never reached the agent").containsExactly("write me a long essay");
    }

    @Test
    void aReplyThatWasWrittenWhenTheUserStoppedIt_isNotGivenToThemByTheRunThatIsDeliveredAgain() {
        userSays(acme, USER, "write me a long essay");
        deliver();
        // The agent has written its answer, the user stops it, and the worker of the service dies before it has stored anything.
        hooks.once(Point.INPUT_RECORDED, run -> {
            stop();
            throw new SimulatedCrash(Point.INPUT_RECORDED);
        });

        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
        leaseRunsOut();
        untilQuiet();

        assertThat(outcomes()).containsExactly("stopped");
        assertThat(repliesTo(acme, USER)).isEmpty();
    }

    // ================================================================ nothing is lost

    @Test
    void aWorkerOfTheServiceThatDiesWhileAReplyIsEndedLosesNothing() {
        userSays(acme, USER, "book a table");
        deliver();
        hooks.once(Point.BOUNDARY_FIXED, run -> cma.hold(cma.sessionIds().getFirst()));
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "for four, at seven"));
        hooks.crashAt(Point.SUPERSEDED);

        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
        assertThat(outcomes()).as("the session is stopped, the turn is still open").containsExactly((String) null);

        leaseRunsOut();
        untilQuiet();

        assertThat(outcomes()).containsExactly("superseded", "replied");
        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: book a table + for four, at seven");
        assertThat(interrupts()).as("the session was not stopped a second time").isEqualTo(1);
        assertThat(waiting()).isZero();
    }

    @Test
    void aMessageThatWasPostedBeforeMessagesSaidThatTheyWait_isAnsweredByItsRunAsBefore() {
        userSays(acme, USER, "hello");
        // As a service of an earlier version left it: stored, announced to the Inbox, and nothing on the message says so.
        jdbc.sql("UPDATE message SET announced = false").update();

        untilQuiet();

        assertThat(repliesTo(acme, USER)).containsExactly("Answer to: hello");
    }

    // ================================================================ helpers

    /** The user writes to a conversation that an ordinary agent serves. */
    private MessageAccepted sideSays(String text) {
        return acme.post("/v1/users/" + USER + "/messages", new PostMessageRequest(null, "side", "helper", text))
                .as(MessageAccepted.class);
    }

    private StopResult stop() {
        return acme.post("/v1/users/" + USER + "/conversations/default/stop", null).as(StopResult.class);
    }

    private EventAccepted event(String type, String subject, String agent) {
        return acme.post("/v1/users/" + USER + "/events",
                new PostEventRequest(null, type, subject, agent, null, AgentJson.mapper().readTree("{}"), null)).as(EventAccepted.class);
    }

    private int untilQuiet() {
        int runs = 0;
        for (int settled = settle(); settled > 0; settled = settle()) {
            runs += settled;
        }
        return runs;
    }

    /** How the turns ended, in the order in which they began. */
    private List<String> outcomes() {
        return jdbc.sql("SELECT outcome FROM turn WHERE merge_key LIKE 'conv:%' ORDER BY created_at, run_id").query(String.class).list();
    }

    /** How many messages of the user wait for a turn. */
    private long waiting() {
        return count("SELECT count(*) FROM message WHERE announced AND taken_by IS NULL");
    }

    private int pendingActions() {
        return acme.get("/v1/users/" + USER + "/actions").as(ActionPage.class).actions().size();
    }

    private long interrupts() {
        return cma.calls().stream().filter(call -> call.operation() == Operation.SEND)
                .filter(call -> ((List<?>) call.argument()).stream().anyMatch(Outgoing.Interrupt.class::isInstance)).count();
    }

    private static String args(String... namesAndValues) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            input.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return input.toString();
    }
}
