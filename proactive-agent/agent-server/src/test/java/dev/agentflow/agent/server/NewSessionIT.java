package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A session knows only what happened in it, and a line's session is replaced now and then: when its
 * agent is configured anew, when credentials change, when the session is lost or does not come to
 * rest. For a main agent, which is the one that knows everything about its user, that would be a
 * clean slate each time. So the service keeps what the agent did and made of things, and gives it to
 * the new session: what was said with the turn's input, and the rest through read_events.
 */
class NewSessionIT extends AgentTestBase {

    private static final String USER = "u42";

    private TestApi acme;
    /** What read_events gave an agent in a conversation, and in the background, call by call. */
    private final List<JsonNode> chatRead = new ArrayList<>();
    private final List<JsonNode> backgroundRead = new ArrayList<>();
    /** Whether the agent in a conversation reads what it is pointed to. */
    private boolean reads;
    private String lastTask;

    @BeforeEach
    void aMainAgentWithAResearcher_andAnOrdinaryAgent() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/helper", new AgentProfileRequest("Helper", null, null, "You help.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", mainAgent("You look after the user.")).as(AgentProfileView.class);
        chatRead.clear();
        backgroundRead.clear();
        reads = true;
        lastTask = null;
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    private static AgentProfileRequest mainAgent(String instructions) {
        return new AgentProfileRequest("Assistant", null, null, instructions, null, null, null, null, new CoordinatorConfig(
                List.of(new WorkerConfig("researcher", "research", "Looks things up and reports what it found.")), null));
    }

    /**
     * An agent in a conversation. It reads what it is pointed to, then does what the user's last line
     * says: "research X" hands X to the researcher, "close" closes the last task, anything else it
     * repeats.
     */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        ToolOutcome read = context.resultOf("read_events");
        if (read != null) {
            chatRead.add(AgentJson.mapper().readTree(read.text()));
        }
        if (context.step() == 0 && reads && context.userMessage().contains("Call read_events")) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        ToolOutcome made = context.resultOf("delegate_task");
        if (made != null) {
            lastTask = AgentJson.mapper().readTree(made.text()).path("task_id").asString();
            return say("On it.");
        }
        if (context.resultOf("close_task") != null) {
            return say("Closed.");
        }
        boolean first = context.step() == 0 || read != null;
        if (first && said.startsWith("research ")) {
            String what = said.substring("research ".length());
            return ModelStep.of(custom("delegate_task", args("role", "researcher", "title", what, "instructions", "Research " + what)));
        }
        if (first && said.equals("close")) {
            return ModelStep.of(custom("close_task", args("task_id", lastTask, "reason", "the user has what they wanted")));
        }
        return say("You said: " + said);
    }

    /**
     * An agent in the background. It tells the user what a worker reports and what happens, except
     * events whose type begins with "quiet.": of those it takes note in its last words, and tells nobody.
     */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            return say("Told the user.");
        }
        JsonNode waiting = AgentJson.mapper().readTree(read.text());
        backgroundRead.add(waiting);
        for (JsonNode report : waiting.path("reports")) {
            if ("report".equals(report.path("kind").asString())) {
                return ModelStep.of(custom("notify_user", args("text", "About " + report.path("title").asString() + ": "
                        + report.path("text").asString())));
            }
        }
        JsonNode event = waiting.path("events").path(0);
        if (event.isMissingNode()) {
            return say("Nothing to tell.");
        }
        return event.path("type").asString().startsWith("quiet.")
                ? say("Noted that " + event.path("subject").asString() + " is " + event.path("data").path("state").asString()
                        + ". The user need not hear of it.")
                : ModelStep.of(custom("notify_user", args("text", "Event: " + event.path("type").asString())));
    }

    private ModelStep worker(ModelContext context) {
        return context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("Found it.");
    }

    // ================================================================ a main agent

    @Test
    void aMainAgentsNewSessionIsGivenItsTasks_whatItDidInTheBackground_andWhatItMadeOfIt() {
        userSays(acme, USER, "research flights");
        untilQuiet();
        userSays(acme, USER, "close");
        untilQuiet();
        userSays(acme, USER, "research hotels");
        untilQuiet();
        event("quiet.stock", "order-1", null, "{\"state\": \"back in stock\"}");
        untilQuiet();
        int sessions = cma.sessionIds().size();

        // The agent is configured anew. Its sessions are behind from here on, and the next turn gets a new one.
        acme.put("/v1/agents/default", mainAgent("You look after the user, briefly.")).as(AgentProfileView.class);
        userSays(acme, USER, "what is new?");
        untilQuiet();

        assertThat(cma.sessionIds()).as("a new session for the conversation").hasSize(sessions + 1);
        String first = cma.userMessages(cma.sessionIds().getLast()).getFirst();
        assertThat(first).as("what was said is given with the input, and the rest is pointed to")
                .startsWith("[From the service that runs you, not from the user: this conversation continues in a new session.")
                .contains("User: research flights\nYou: On it.\n", "You: About hotels: Found it.\n",
                        "this session is new and knows nothing of your work before it. The service has it on record: your tasks "
                                + "and how they stand, what you handled in the background and what you made of it. Call "
                                + "read_events to get it before you answer.]")
                .endsWith("what is new?");

        JsonNode record = chatRead.getLast().path("before_this_session");
        assertThat(record.path("tasks")).as("the task that is still open, with the worker's report").hasSize(1);
        assertThat(record.path("tasks").get(0).path("title").asString()).isEqualTo("hotels");
        assertThat(record.path("tasks").get(0).path("status").asString()).isEqualTo("idle");
        assertThat(record.path("tasks").get(0).path("latest").path("text").asString()).isEqualTo("Found it.");
        assertThat(record.path("closed_lately")).as("and the one that it closed").hasSize(1);
        assertThat(record.path("closed_lately").get(0).path("title").asString()).isEqualTo("flights");
        assertThat(record.path("closed_lately").get(0).path("closed_by").asString()).isEqualTo("you");
        assertThat(record.path("closed_lately").get(0).path("closed_because").asString()).isEqualTo("the user has what they wanted");
        assertThat(record.path("closed_lately").get(0).path("last_report").path("text").asString()).isEqualTo("Found it.");

        JsonNode background = record.path("in_the_background");
        assertThat(background).as("two reports that it told the user of, and an event that it kept to itself").hasSize(3);
        assertThat(background.get(0).path("about").get(0).path("news_of_your_tasks").asInt()).isEqualTo(1);
        assertThat(background.get(0).path("told_the_user").get(0).asString()).isEqualTo("About flights: Found it.");
        assertThat(background.get(2).path("about").get(0).path("event").asString()).isEqualTo("quiet.stock");
        assertThat(background.get(2).path("about").get(0).path("subject").asString()).isEqualTo("order-1");
        assertThat(background.get(2).path("about").get(0).path("data").asString()).isEqualTo("{\"state\":\"back in stock\"}");
        assertThat(background.get(2).path("told_the_user")).isEmpty();
        assertThat(background.get(2).path("your_last_words").asString())
                .isEqualTo("Noted that order-1 is back in stock. The user need not hear of it.");
        assertThat(record.path("the_user_wants_you").asString()).isEqualTo("proactive");

        // Told once: the session has it now.
        assertThat(count("SELECT count(*) FROM thread WHERE merge_key = 'conv:default' AND briefed_generation = generation")).isEqualTo(1);
        userSays(acme, USER, "thanks");
        untilQuiet();
        assertThat(cma.userMessages(cma.sessionIds().getLast()).getLast()).isEqualTo("thanks");
    }

    @Test
    void aNewSessionThatStartsInTheBackgroundIsGivenTheSame() {
        userSays(acme, USER, "research flights");
        untilQuiet();
        acme.put("/v1/agents/default", mainAgent("You look after the user, briefly.")).as(AgentProfileView.class);

        event("order.delayed", "order-1", null, "{}");
        untilQuiet();

        String first = cma.userMessages(cma.sessionIds().getLast()).getFirst();
        assertThat(first).contains("User: research flights\nYou: On it.\nYou: About flights: Found it.\n[End of the earlier conversation.]",
                "Call read_events to get it before you decide what to do.]",
                "An event from the application's systems is waiting for you.");
        JsonNode read = backgroundRead.getLast();
        assertThat(read.path("events").get(0).path("type").asString()).as("what the turn is for").isEqualTo("order.delayed");
        assertThat(read.path("before_this_session").path("tasks").get(0).path("task_id").asString()).isEqualTo(lastTask);
        assertThat(read.path("before_this_session").path("in_the_background").get(0).path("told_the_user").get(0).asString())
                .isEqualTo("About flights: Found it.");
        assertThat(count("SELECT count(*) FROM thread WHERE merge_key = 'conv:default' AND briefed_generation = generation")).isEqualTo(1);

        event("order.delayed", "order-2", null, "{}");
        untilQuiet();
        assertThat(backgroundRead.getLast().has("before_this_session")).as("told once").isFalse();
    }

    @Test
    void whatIsOnRecordIsOfferedUntilTheSessionHasReadIt() {
        userSays(acme, USER, "research flights");
        untilQuiet();
        acme.put("/v1/agents/default", mainAgent("You look after the user, briefly.")).as(AgentProfileView.class);

        // The agent answers without reading what it is pointed to.
        reads = false;
        userSays(acme, USER, "hello");
        untilQuiet();
        assertThat(chatRead).isEmpty();

        reads = true;
        userSays(acme, USER, "and now?");
        untilQuiet();

        List<String> given = cma.userMessages(cma.sessionIds().getLast());
        assertThat(given.get(0)).contains("this conversation continues in a new session", "Call read_events to get it before you answer.]");
        assertThat(given.get(1)).as("what was said is in the session by now; what is on record is still to be read")
                .doesNotContain("this conversation continues in a new session")
                .contains("Call read_events to get it before you answer.]").endsWith("and now?");
        assertThat(chatRead.getLast().path("before_this_session").path("tasks").get(0).path("title").asString()).isEqualTo("flights");

        userSays(acme, USER, "thanks");
        untilQuiet();
        assertThat(cma.userMessages(cma.sessionIds().getLast()).getLast()).isEqualTo("thanks");
    }

    // ================================================================ any agent

    @Test
    void theNewSessionOfASubjectIsToldOfTheSubjectsEarlierEvents() {
        // An application without a main agent: the events of a subject have a line, and a session, of their own.
        TestApi globex = app(GLOBEX);
        defaultAgent(globex);
        subjectEvent(globex, "quiet.stock", "{\"state\": \"out of stock\"}");
        untilQuiet();
        subjectEvent(globex, "order.delayed", "{}");
        untilQuiet();
        globex.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help, briefly.", null, null))
                .as(AgentProfileView.class);

        subjectEvent(globex, "quiet.stock", "{\"state\": \"back in stock\"}");
        untilQuiet();

        assertThat(cma.sessionIds()).as("the subject's first session, and the one that replaced it").hasSize(2);
        assertThat(cma.userMessages(cma.sessionIds().getLast()).getFirst())
                .contains("The service has it on record: what you handled in the background and what you made of it. Call "
                        + "read_events to get it before you decide what to do.]");
        JsonNode background = backgroundRead.getLast().path("before_this_session").path("in_the_background");
        assertThat(background).hasSize(2);
        assertThat(background.get(0).path("about").get(0).path("data").asString()).isEqualTo("{\"state\":\"out of stock\"}");
        assertThat(background.get(0).path("your_last_words").asString()).startsWith("Noted that order-1 is out of stock.");
        assertThat(background.get(1).path("about").get(0).path("event").asString()).isEqualTo("order.delayed");
        assertThat(background.get(1).path("told_the_user").get(0).asString()).isEqualTo("Event: order.delayed");
        assertThat(backgroundRead.getLast().path("before_this_session").has("tasks")).as("an agent without workers has none").isFalse();
        assertThat(backgroundRead.getLast().path("before_this_session").has("the_user_wants_you")).isFalse();
    }

    @Test
    void aConversationWithNothingElseOnRecordIsToldWhatWasSaid_andIsNotSentToReadAnything() {
        sideSays("hello");
        untilQuiet();
        acme.put("/v1/agents/helper", new AgentProfileRequest("Helper", null, null, "You help, briefly.", null, null))
                .as(AgentProfileView.class);

        sideSays("again");
        untilQuiet();

        String first = cma.userMessages(cma.sessionIds().getLast()).getFirst();
        assertThat(first).contains("User: hello\nYou: You said: hello\n").doesNotContain("read_events").endsWith("again");
        assertThat(count("SELECT count(*) FROM thread WHERE merge_key = 'conv:side' AND briefed_generation = generation"))
                .as("there was nothing to tell it").isEqualTo(1);
    }

    // ================================================================ helpers

    private MessageAccepted sideSays(String text) {
        return acme.post("/v1/users/" + USER + "/messages", new PostMessageRequest(null, "side", "helper", text))
                .as(MessageAccepted.class);
    }

    private EventAccepted subjectEvent(TestApi app, String type, String data) {
        return app.post("/v1/users/" + USER + "/events",
                new PostEventRequest(null, type, "order-1", null, null, AgentJson.mapper().readTree(data), null)).as(EventAccepted.class);
    }

    private EventAccepted event(String type, String subject, String agent, String data) {
        return acme.post("/v1/users/" + USER + "/events",
                new PostEventRequest(null, type, subject, agent, null, AgentJson.mapper().readTree(data), null)).as(EventAccepted.class);
    }

    private int untilQuiet() {
        int runs = 0;
        for (int settled = settle(); settled > 0; settled = settle()) {
            runs += settled;
        }
        return runs;
    }

    private static String args(String... namesAndValues) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            input.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return input.toString();
    }
}
