package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.ActionView;
import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.TaskView;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.actions.ActionDesk;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A main agent asks the user in passing. Everything that concerns the user comes in on its line, so
 * a turn that waited for an answer there would be ended by the next event or report, and the
 * question with it. Its question is put before the user and its turn ends; the user's answer is
 * their next message, which is a turn of its own, and nothing but the user's word ends the question
 * early.
 */
class MainAgentQuestionsIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String ACTIONS = "/v1/users/u42/actions";

    private TestApi acme;
    private TestApi alice;
    /** What read_events gave the main agent in the background, call by call. */
    private final List<JsonNode> mainRead = new ArrayList<>();
    /** What the tools answered the main agent, call by call. */
    private final List<String> answers = new ArrayList<>();

    @Autowired
    private ActionDesk actionDesk;

    @BeforeEach
    void aMainAgentWithAResearcher() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research",
                        "Looks things up and reports what it found.")), null))).as(AgentProfileView.class);
        alice = user(acme, USER);
        mainRead.clear();
        answers.clear();
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? researching(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    /**
     * The main agent in a conversation. "book a table" makes it ask for the time, "book twice" ask two
     * things at once, "research X" hand X to the researcher, and "yes ..." hand on what it had asked
     * about; a time is what it books for. Anything else it repeats.
     */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        if (context.step() == 0) {
            if (said.equals("book a table")) {
                return ModelStep.of(custom("ask_user", "{\"question\": \"For what time?\", \"options\": [\"7 pm\", \"8 pm\"]}"));
            }
            if (said.equals("book twice")) {
                return ModelStep.of(custom("ask_user", "{\"question\": \"For what time?\"}"),
                        custom("ask_user", "{\"question\": \"For how many?\"}"));
            }
            if (said.startsWith("research ") || said.startsWith("yes")) {
                String what = said.startsWith("yes") ? "the refund" : said.substring("research ".length());
                return ModelStep.of(custom("delegate_task", args("role", "researcher", "title", what, "instructions", "Research " + what)));
            }
            return say(said.endsWith(" pm") ? "Booked for " + said + "." : "You said: " + said);
        }
        context.toolResults().forEach(this::heard);
        return say("ask_user".equals(context.toolResults().getFirst().tool()) ? "I need one thing from you." : "On it.");
    }

    /**
     * The main agent in the background. An event whose data has {@code ask} makes it put that question
     * to the user; any other event of the application it tells the user about, and so a worker's
     * report. What the service tells it about a question it only notes.
     */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return context.userMessage().contains("Call read_events") ? ModelStep.of(custom("read_events", "{}")) : say("Looked around.");
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            context.toolResults().forEach(this::heard);
            return say("Done for now.");
        }
        JsonNode waiting = AgentJson.mapper().readTree(read.text());
        mainRead.add(waiting);
        JsonNode event = waiting.path("events").path(0);
        if (event.path("data").has("ask")) {
            return ModelStep.of(custom("ask_user", args("question", event.path("data").path("ask").asString())));
        }
        if (event.path("type").asString("").startsWith("agent.")) {
            return say("Noted.");
        }
        JsonNode report = waiting.path("reports").path(0);
        String text = !report.isMissingNode() ? "About " + report.path("title").asString() + ": " + report.path("text").asString()
                : "Event: " + event.path("type").asString();
        return ModelStep.of(custom("notify_user", args("text", text)));
    }

    private ModelStep researching(ModelContext context) {
        return context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("Researched.");
    }

    private void heard(ToolOutcome answer) {
        answers.add(answer.tool() + (answer.error() ? " error: " : ": ") + answer.text());
    }

    // ================================================================ asked in passing

    @Test
    void theMainAgentsQuestionIsPutBeforeTheUser_andItsTurnDoesNotWait() {
        userSays(alice, USER, "book a table");

        assertThat(settle()).isEqualTo(1);

        ActionView question = onlyPending();
        assertThat(question).extracting(ActionView::kind, ActionView::status, ActionView::approver, ActionView::conversationId,
                ActionView::question).containsExactly("question", "pending", "user", "default", "For what time?");
        assertThat(question.options()).containsExactly("7 pm", "8 pm");
        assertThat(question.expiresAt()).isEqualTo(question.createdAt().plus(Duration.ofHours(24)));
        assertThat(messagesOf(alice, USER)).extracting(MessageView::kind, MessageView::text, MessageView::actionId).containsExactly(
                tuple("message", "book a table", null),
                tuple("question", "For what time?", question.actionId()),
                tuple("reply", "I need one thing from you.", null));
        assertThat(answers.getLast()).startsWith("ask_user: Asked. The user sees your question in the conversation.");
        assertThat(runs()).as("the turn is over: nothing waits in the Inbox").containsExactly("done attempt 1");
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'open'")).isZero();
    }

    @Test
    void whatElseComesInDoesNotTakeTheQuestionAway() {
        userSays(alice, USER, "research flights");
        settle();
        // Nothing of the user's has to be answered from here on; the question is the event's.
        event("order.delayed", "{\"ask\": \"Order 1234 is late. A refund?\"}");
        settle();
        ActionView question = onlyPending();
        assertThat(question.question()).isEqualTo("Order 1234 is late. A refund?");

        // The worker's report comes in on the main agent's line, and then an event that has nothing to do with the question.
        untilQuiet();
        event("newsletter.sent", "{}");
        untilQuiet();

        assertThat(repliesTo(acme, USER)).contains("About flights: Researched.", "Event: newsletter.sent");
        assertThat(onlyPending().actionId()).as("the question still waits for the user").isEqualTo(question.actionId());
        assertThat(acme.get(ACTIONS + "/" + question.actionId()).as(ActionView.class).status()).isEqualTo("pending");
    }

    @Test
    void theUsersNextMessageIsTheAnswer_andATurnOfItsOwn() {
        userSays(alice, USER, "book a table");
        settle();
        ActionView question = onlyPending();

        MessageAccepted accepted = userSays(alice, USER, "7 pm");

        assertThat(accepted.actionId()).as("the message answered the question").isEqualTo(question.actionId());
        assertThat(acme.get(ACTIONS + "/" + question.actionId()).as(ActionView.class))
                .extracting(ActionView::status, ActionView::answer, ActionView::resolvedBy)
                .containsExactly("answered", "7 pm", "user");
        assertThat(pending()).isEmpty();
        assertThat(settle()).as("a turn of its own").isEqualTo(1);
        assertThat(repliesTo(acme, USER)).endsWith("Booked for 7 pm.");
    }

    @Test
    void anAnswerGivenInTheApplicationComesToTheMainAgentAsTheUsersMessage() {
        userSays(alice, USER, "book a table");
        settle();
        ActionView question = onlyPending();

        ActionView answered = resolve(alice, question.actionId(), null, "8 pm", null).as(ActionView.class);

        assertThat(answered).extracting(ActionView::status, ActionView::answer, ActionView::resolvedBy)
                .containsExactly("answered", "8 pm", "user");
        assertThat(messagesOf(alice, USER)).extracting(MessageView::role, MessageView::kind, MessageView::text, MessageView::actionId)
                .endsWith(tuple("user", "message", "8 pm", question.actionId()));
        assertThat(settle()).isEqualTo(1);
        assertThat(repliesTo(acme, USER)).endsWith("Booked for 8 pm.");
        assertThat(resolve(alice, question.actionId(), null, "8 pm", null).as(ActionView.class).status())
                .as("the same answer again is harmless").isEqualTo("answered");
        assertThat(settle()).as("and is not answered a second time").isZero();
    }

    @Test
    void whatTheUserAnswersToAQuestionFromTheBackgroundIsTheirWord_soTheWorkStartsAtOnce() {
        event("order.delayed", "{\"ask\": \"Order 1234 is late. Shall I look into a refund?\"}");
        settle();
        assertThat(messagesOf(alice, USER)).extracting(MessageView::kind, MessageView::text)
                .containsExactly(tuple("question", "Order 1234 is late. Shall I look into a refund?"));

        userSays(alice, USER, "yes, please");
        settle();

        List<TaskView> tasks = acme.get("/v1/users/u42/tasks?status=all").as(TaskPage.class).tasks();
        assertThat(tasks).extracting(TaskView::title, TaskView::authorizedBy)
                .as("delegated in a turn that the user began").containsExactly(tuple("the refund", "conversation"));
        assertThat(tasks.getFirst().status()).isNotEqualTo("proposed");
    }

    // ================================================================ no answer

    @Test
    void aQuestionTheUserDeclinesIsOver_andTheMainAgentIsTold() {
        userSays(alice, USER, "book a table");
        settle();
        ActionView question = onlyPending();

        ActionView declined = resolve(alice, question.actionId(), "deny", null, "not tonight").as(ActionView.class);

        assertThat(declined).extracting(ActionView::status, ActionView::note).containsExactly("denied", "not tonight");
        assertThat(settle()).as("a turn in the background tells the main agent").isEqualTo(1);
        JsonNode told = mainRead.getLast().path("events").path(0);
        assertThat(told.path("type").asString()).isEqualTo("agent.question_unanswered");
        assertThat(told.path("data").path("question").asString()).isEqualTo("For what time?");
        assertThat(told.path("data").path("outcome").asString()).isEqualTo("declined");
        assertThat(told.path("data").path("reason").asString()).isEqualTo("not tonight");
        assertThat(settle()).isZero();
    }

    @Test
    void aQuestionNobodyAnswersInTimeEnds_andTheMainAgentIsTold() {
        userSays(alice, USER, "book a table");
        settle();
        ActionView question = onlyPending();
        clock.advance(Duration.ofHours(24).minusSeconds(60));
        assertThat(actionDesk.expireUnanswered(10)).as("there is still time to answer").isZero();
        clock.advance(Duration.ofSeconds(60));

        assertThat(actionDesk.expireUnanswered(10)).isEqualTo(1);

        assertThat(acme.get(ACTIONS + "/" + question.actionId()).as(ActionView.class))
                .extracting(ActionView::status, ActionView::note, ActionView::resolvedBy)
                .containsExactly("expired", "nobody decided in time", "service");
        assertThat(settle()).isEqualTo(1);
        JsonNode told = mainRead.getLast().path("events").path(0);
        assertThat(told.path("type").asString()).isEqualTo("agent.question_unanswered");
        assertThat(told.path("data").path("outcome").asString()).isEqualTo("expired");
        assertThat(actionDesk.expireUnanswered(10)).as("it ends once").isZero();
    }

    // ================================================================ one at a time, and not too many

    @Test
    void aNewerQuestionTakesThePlaceOfOneThatStillWaits() {
        userSays(alice, USER, "book a table");
        settle();
        ActionView first = onlyPending();

        event("order.delayed", "{\"ask\": \"Order 1234 is late. A refund?\"}");
        settle();

        ActionView second = onlyPending();
        assertThat(second.question()).isEqualTo("Order 1234 is late. A refund?");
        assertThat(acme.get(ACTIONS + "/" + first.actionId()).as(ActionView.class))
                .extracting(ActionView::status, ActionView::note)
                .containsExactly("withdrawn", "the agent asked a newer question");
    }

    @Test
    void oneQuestionIsAskedInATurn() {
        userSays(alice, USER, "book twice");
        settle();

        assertThat(onlyPending().question()).isEqualTo("For what time?");
        assertThat(answers).anyMatch(answer -> answer.startsWith("ask_user: Asked."))
                .anyMatch(answer -> answer.startsWith("ask_user error: Not asked: you have already asked the user a question"));
    }

    @Test
    void inTheBackgroundAQuestionCountsAgainstWhatTheUserIsSentInADay() {
        for (int i = 1; i <= 19; i++) {
            event("order.delayed", "{}");
            settle();
        }
        assertThat(repliesTo(acme, USER)).hasSize(19);

        event("order.delayed", "{\"ask\": \"A refund?\"}");
        settle();
        assertThat(onlyPending().question()).as("the twentieth thing the user is sent today").isEqualTo("A refund?");

        event("order.delayed", "{}");
        settle();
        assertThat(answers.getLast()).as("a notification is refused: the question counted")
                .startsWith("notify_user error: Not sent: this user has been sent 20 notifications in the last 24 hours");

        event("order.delayed", "{\"ask\": \"Or a voucher?\"}");
        settle();
        assertThat(answers.getLast())
                .startsWith("ask_user error: Not asked: this user has been sent 20 notifications and questions in the last 24 hours");
        assertThat(onlyPending().question()).as("the earlier question still waits").isEqualTo("A refund?");

        userSays(alice, USER, "book a table");
        settle();
        assertThat(onlyPending().question()).as("in a conversation the user is there, and can be asked").isEqualTo("For what time?");
    }

    // ================================================================ helpers

    private void event(String type, String data) {
        acme.post("/v1/users/u42/events", new PostEventRequest(null, type, "order-1234", null, null,
                AgentJson.mapper().readTree(data), null)).as(EventAccepted.class);
    }

    private int untilQuiet() {
        int runs = 0;
        for (int settled = settle(); settled > 0; settled = settle()) {
            runs += settled;
        }
        return runs;
    }

    private List<ActionView> pending() {
        return acme.get(ACTIONS).as(ActionPage.class).actions();
    }

    private ActionView onlyPending() {
        List<ActionView> waiting = pending();
        assertThat(waiting).as("the actions that wait").hasSize(1);
        return waiting.getFirst();
    }

    private TestApi.Reply resolve(TestApi caller, String actionId, String decision, String answer, String message) {
        return caller.post(ACTIONS + "/" + actionId + "/resolve", new ResolveActionRequest(decision, answer, message));
    }

    private static String args(String... namesAndValues) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            input.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return input.toString();
    }
}
