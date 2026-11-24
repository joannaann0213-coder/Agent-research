package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
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
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.api.TaskNoteView;
import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.TaskView;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.agent.server.tools.DemoAgent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A main agent stands by for the user, so it knows how every task of its conversation stands, not
 * only what came of it. It is told when a worker waits for a person, and what the person decided.
 * And whatever its tasks have for it is given to its next turn, whichever that is: when the user
 * writes first, the main agent answers knowing it, and the run that was to bring it has nothing
 * left to do.
 */
class MainAgentContextIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String TASKS = "/v1/users/u42/tasks";
    private static final String EVENTS = "/v1/users/u42/events";

    private TestApi acme;
    private TestApi alice;
    /** What read_events gave the main agent in the background, call by call. */
    private final List<JsonNode> mainRead = new ArrayList<>();
    /** What read_events gave the main agent in a conversation, call by call. */
    private final List<JsonNode> chatRead = new ArrayList<>();
    /** Whether the main agent reads what the service points it to when the user writes. */
    private boolean readsTheNews;

    @BeforeEach
    void aMainAgentWithAResearcherAndAPayer() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/ops", new AgentProfileRequest("Ops", null, null, "You handle payments.", null,
                List.of(new McpServerConfig("payments", "https://mcp.acme.example/payments", "always_ask", "user"))))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(
                        new WorkerConfig("researcher", "research", "Looks things up and reports what it found."),
                        new WorkerConfig("payer", "ops", "Pays and refunds.")), null))).as(AgentProfileView.class);
        alice = user(acme, USER);
        mainRead.clear();
        chatRead.clear();
        readsTheNews = true;
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    /**
     * The main agent in the background. It tells the user that a worker waits for them, and what a
     * worker reports; of everything else it takes note. An event whose data says {@code look_into}
     * makes it offer that work to the user.
     */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return context.userMessage().contains("Call read_events") ? ModelStep.of(custom("read_events", "{}")) : say("Looked around.");
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            return say("Done for now.");
        }
        JsonNode waiting = AgentJson.mapper().readTree(read.text());
        mainRead.add(waiting);
        for (JsonNode event : waiting.path("events")) {
            String what = event.path("data").path("look_into").asString(null);
            if (what != null) {
                ObjectNode input = AgentJson.mapper().createObjectNode();
                input.put("role", "researcher").put("title", what).put("instructions", "Research " + what)
                        .put("summary", "I can look into " + what + " for you.");
                return ModelStep.of(custom("delegate_task", input.toString()));
            }
        }
        List<String> tell = new ArrayList<>();
        for (JsonNode report : waiting.path("reports")) {
            switch (report.path("kind").asString()) {
                case "waiting" -> tell.add("Needs you: " + report.path("title").asString());
                case "report" -> tell.add("About " + report.path("title").asString() + ": " + report.path("text").asString());
                default -> {
                }
            }
        }
        return tell.isEmpty() ? say("Noted.") : ModelStep.of(custom("notify_user", args("text", String.join(" ", tell))));
    }

    /**
     * The main agent in a conversation. It reads what the service points it to first. "refund X"
     * hands X to the payer and "research X" to the researcher; anything else it repeats, with the
     * kinds of news it has just read.
     */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        ToolOutcome read = context.resultOf("read_events");
        if (context.step() == 0 && readsTheNews && context.userMessage().contains("Call read_events")) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        if (context.resultOf("delegate_task") != null) {
            return say("Handed on.");
        }
        List<String> news = new ArrayList<>();
        if (read != null) {
            JsonNode since = AgentJson.mapper().readTree(read.text());
            chatRead.add(since);
            since.path("reports").forEach(report -> news.add(report.path("kind").asString()));
        }
        if (said.startsWith("refund ") || said.startsWith("research ")) {
            ObjectNode input = AgentJson.mapper().createObjectNode();
            input.put("role", said.startsWith("refund ") ? "payer" : "researcher").put("title", said).put("instructions", said);
            return ModelStep.of(custom("delegate_task", input.toString()));
        }
        return say("You said: " + said + (news.isEmpty() ? "" : ". News: " + String.join(", ", news)));
    }

    /** A worker. A task to refund makes it call the payment system, which waits for a person; anything else it just does. */
    private ModelStep worker(ModelContext context) {
        if (context.step() == 0) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read != null) {
            JsonNode handed = AgentJson.mapper().readTree(read.text());
            JsonNode last = handed.path("messages").path(handed.path("messages").size() - 1);
            String asked = last.path("text").asString("");
            if (asked.startsWith("refund ")) {
                return ModelStep.of(mcp("payments", "refund", "{\"order\": \"1234\"}"));
            }
            return say(last.has("event") ? "Handled " + last.path("event").path("type").asString() + "." : "Did: " + asked);
        }
        ToolOutcome refund = context.resultOf("refund");
        return say(refund == null ? "Finished." : refund.error() ? "Refund not made: " + refund.text() : "Refunded.");
    }

    // ================================================================ a worker that waits for a person

    @Test
    void whenAWorkerWaitsForAnApproval_theMainAgentIsToldAtOnce_andLaterWhatWasDecided() {
        userSays(acme, USER, "refund order 1234");
        untilQuiet();

        ActionView action = alice.get("/v1/users/u42/actions").as(ActionPage.class).actions().getFirst();
        TaskView task = tasks().getFirst();
        assertThat(task.status()).as("the worker stands still").isEqualTo("working");

        // The main agent heard of it through its own line, and told the user.
        JsonNode told = mainRead.getFirst().path("reports").path(0);
        assertThat(told.path("kind").asString()).isEqualTo("waiting");
        assertThat(told.path("from").asString()).isEqualTo("the service");
        assertThat(told.path("task_id").asString()).isEqualTo(task.taskId());
        assertThat(told.path("role").asString()).isEqualTo("payer");
        assertThat(told.path("text").asString())
                .startsWith("Your worker payer waits for the user's approval before it calls payments.refund on the task "
                        + "\"refund order 1234\". It goes on as soon as that is decided")
                .contains("The user decides it in the application", "What the call would be given: {\"order\":\"1234\"}");
        assertThat(cma.userMessages(sessionOf("conv:default")).get(1)).contains("The user is not here",
                "One of your workers waits for somebody's approval. Call read_events to get it");
        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).containsExactly(
                tuple("message", "refund order 1234"), tuple("reply", "Handed on."),
                tuple("notification", "Needs you: refund order 1234"));

        alice.post("/v1/users/u42/actions/" + action.actionId() + "/resolve", new ResolveActionRequest("approve", null, null))
                .as(ActionView.class);
        untilQuiet();

        // What was decided comes with the worker's report: nothing woke the main agent for it alone.
        assertThat(mainRead).hasSize(2);
        assertThat(mainRead.getLast().path("reports")).extracting(report -> report.path("kind").asString())
                .containsExactly("decided", "report");
        assertThat(mainRead.getLast().path("reports").path(0).path("text").asString())
                .isEqualTo("The user approved payments.refund, which your worker payer waited for. The worker goes on.");
        assertThat(repliesTo(acme, USER)).endsWith("About refund order 1234: Refunded.");
        assertThat(acme.get(TASKS + "/" + task.taskId()).as(TaskView.class).notes())
                .extracting(TaskNoteView::from, TaskNoteView::kind)
                .containsExactly(tuple("main", "assignment"), tuple("service", "waiting"), tuple("service", "decided"),
                        tuple("worker", "report"));
    }

    @Test
    void whenThePersonSaysNo_theMainAgentIsToldThatToo() {
        userSays(acme, USER, "refund order 1234");
        untilQuiet();
        ActionView action = alice.get("/v1/users/u42/actions").as(ActionPage.class).actions().getFirst();

        alice.post("/v1/users/u42/actions/" + action.actionId() + "/resolve", new ResolveActionRequest("deny", null, "too much"))
                .as(ActionView.class);
        untilQuiet();

        assertThat(mainRead.getLast().path("reports").path(0).path("text").asString()).isEqualTo("The user denied "
                + "payments.refund, which your worker payer waited for: too much. The worker has been told and goes on without it.");
        assertThat(repliesTo(acme, USER).getLast()).startsWith("About refund order 1234: Refund not made: ");
    }

    // ================================================================ whatever turn comes next is given the news

    @Test
    void whenTheUserWritesFirst_theMainAgentAnswersKnowingTheNews_andTheRunThatWasToBringItHasNothingLeft() {
        EventAccepted handed = event("order.delayed", "order-1", "research", "{}");
        userSays(acme, USER, "hello");
        deliver();

        assertThat(worker.runOnce()).as("the user's message goes first").isTrue();

        assertThat(cma.userMessages(sessionOf("conv:default")).getFirst()).as("the user's message, with the service's note")
                .contains("there is news of your workers' tasks that you have not seen yet: the application has handed one of "
                        + "your workers work itself. Call read_events to get it before you answer, and tell the user what "
                        + "they should know of it.]")
                .endsWith("hello");
        JsonNode news = chatRead.getFirst().path("reports").path(0);
        assertThat(news.path("kind").asString()).isEqualTo("assigned");
        assertThat(news.path("task_id").asString()).isEqualTo(handed.taskId());
        assertThat(repliesTo(acme, USER)).containsExactly("You said: hello. News: assigned");

        untilQuiet();

        assertThat(jdbc.sql("SELECT split_part(merge_key, ':', 1) || ' ' || outcome FROM turn ORDER BY created_at, run_id")
                .query(String.class).list())
                .as("no turn of the main agent for what it knew already")
                .containsExactlyInAnyOrder("conv replied", "work reported", "conv notified");
        assertThat(mainRead).as("the worker's report still woke it").hasSize(1);
        assertThat(runs()).as("the run that was to bring the news ended without a turn").hasSize(4)
                .allMatch(run -> run.equals("done attempt 1"));
    }

    @Test
    void whatTheMainAgentDidNotReadWithTheUsersMessageStillComesByItself() {
        readsTheNews = false;
        event("order.delayed", "order-1", "research", "{}");
        userSays(acme, USER, "hello");

        untilQuiet();

        assertThat(repliesTo(acme, USER).getFirst()).isEqualTo("You said: hello");
        assertThat(mainRead.getFirst().path("reports").path(0).path("kind").asString()).as("in a turn of its own").isEqualTo("assigned");
        assertThat(jdbc.sql("SELECT split_part(merge_key, ':', 1) || ' ' || outcome FROM turn ORDER BY created_at, run_id")
                .query(String.class).list())
                .containsExactlyInAnyOrder("conv replied", "conv silent", "work reported", "conv notified");
    }

    @Test
    void whatIsHeldBackIsNoNewsBeforeItsTime() {
        acme.post(EVENTS, new PostEventRequest(null, "order.delayed", "order-1", "research", null, null,
                clock.now().plus(Duration.ofHours(2)))).as(EventAccepted.class);
        userSays(acme, USER, "hello");
        untilQuiet();

        assertThat(cma.userMessages(sessionOf("conv:default")).getFirst()).isEqualTo("hello");
        assertThat(count("SELECT count(*) FROM task_note WHERE kind = 'assigned' AND read_at IS NULL AND ready_at IS NOT NULL"))
                .isEqualTo(1);
    }

    @Test
    void aTurnInTheBackgroundIsGivenEverythingThatIsNew_notOnlyWhatWokeIt() {
        // The main agent offers some work; the user confirms it in their own client, where the main agent does not see it.
        acme.post(EVENTS, new PostEventRequest(null, "order.delayed", "order-1", null, null,
                AgentJson.mapper().readTree("{\"look_into\": \"why order 1 is late\"}"), null)).as(EventAccepted.class);
        untilQuiet();
        String proposed = tasks().getFirst().taskId();
        int turnsBefore = (int) count("SELECT count(*) FROM turn WHERE merge_key = 'conv:default'");

        alice.post(TASKS + "/" + proposed + "/start", null).as(TaskView.class);

        assertThat(count("SELECT count(*) FROM inbox_relay WHERE request ->> 'merge_key' = 'conv:default' AND status = 'pending'"))
                .as("nothing wakes the main agent for it").isZero();
        untilQuiet();

        assertThat(count("SELECT count(*) FROM turn WHERE merge_key = 'conv:default'")).as("one turn, for the report").isEqualTo(turnsBefore + 1);
        assertThat(mainRead.getLast().path("reports")).extracting(report -> report.path("kind").asString())
                .containsExactly("decided", "report");
        assertThat(mainRead.getLast().path("reports").path(0).path("text").asString()).isEqualTo("The user confirmed the task "
                + "\"why order 1 is late\", which you proposed, outside this conversation. Its worker researcher has started.");
    }

    // ================================================================ the demo agent

    @Test
    void theDemoAgentTellsTheUserThatAWorkerWaits_andAnswersTheUserWithTheNews() {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(new WorkerConfig("payer", "ops", "Pays and refunds.")), null)))
                .as(AgentProfileView.class);
        cma.brain(new DemoAgent());

        userSays(acme, USER, "task: refund order 1234, needs approval");
        untilQuiet();

        ActionView action = alice.get("/v1/users/u42/actions").as(ActionPage.class).actions().getFirst();
        assertThat(action.tool().server() + "." + action.tool().name()).isEqualTo("payments.demo");
        assertThat(repliesTo(acme, USER).getLast()).isEqualTo("[demo] Your payer worker waits for an approval before it can go on "
                + "with \"refund order 1234, needs approval\".");

        // The user approves and writes right away: the answer has the news that the approval brought.
        alice.post("/v1/users/u42/actions/" + action.actionId() + "/resolve", new ResolveActionRequest("approve", null, null))
                .as(ActionView.class);
        untilQuiet();

        assertThat(repliesTo(acme, USER).getLast()).isEqualTo("[demo] Your task \"refund order 1234, needs approval\" is back "
                + "from the payer: [demo] Done. The call of payments.demo was approved and made.");
    }

    // ================================================================ helpers

    private EventAccepted event(String type, String subject, String agent, String data) {
        return acme.post(EVENTS, new PostEventRequest(null, type, subject, agent, null, AgentJson.mapper().readTree(data), null))
                .as(EventAccepted.class);
    }

    private int untilQuiet() {
        int runs = 0;
        for (int settled = settle(); settled > 0; settled = settle()) {
            runs += settled;
        }
        return runs;
    }

    private List<TaskView> tasks() {
        return acme.get(TASKS).as(TaskPage.class).tasks();
    }

    private String sessionOf(String line) {
        return cma.sessionIds().stream().filter(session -> line.equals(cma.sessionSpec(session).metadata().get("line")))
                .reduce((first, second) -> second).orElseThrow();
    }

    private static String args(String... namesAndValues) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            input.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return input.toString();
    }
}
