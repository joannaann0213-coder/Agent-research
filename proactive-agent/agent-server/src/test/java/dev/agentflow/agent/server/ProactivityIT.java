package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.ActionView;
import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.PreferencesRequest;
import dev.agentflow.agent.api.PreferencesView;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.UpdatePage;
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
 * How forward a main agent is, is the user's to say. An assistant that is keen at first is right
 * for most people and too much for some, so there are three levels: {@code proactive}, which is
 * where it starts, {@code balanced} and {@code quiet}. The level decides what wakes the main agent
 * at once, what it is asked to tell the user, and whether it offers work unasked. Whatever the
 * level, it knows of everything: what does not wake it comes with its next turn.
 */
class ProactivityIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String EVENTS = "/v1/users/u42/events";
    private static final String PREFERENCES = "/v1/users/u42/preferences";

    private TestApi acme;
    private TestApi alice;
    /** What read_events gave the main agent in the background, call by call. */
    private final List<JsonNode> mainRead = new ArrayList<>();
    /** What the tools answered the main agent, call by call. */
    private final List<String> answers = new ArrayList<>();

    @BeforeEach
    void aMainAgentWithAResearcherAndAPayer() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/ops", new AgentProfileRequest("Ops", null, null, "You handle payments.", null,
                List.of(new McpServerConfig("payments", "https://mcp.acme.example/payments", "always_ask", "user"))))
                .as(AgentProfileView.class);
        mainAgent(null);
        alice = user(acme, USER);
        mainRead.clear();
        answers.clear();
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    /** The main agent, with its own default for how forward it is; the service's when {@code null}. */
    private void mainAgent(String proactivity) {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(
                        new WorkerConfig("researcher", "research", "Looks things up and reports what it found."),
                        new WorkerConfig("payer", "ops", "Pays and refunds.")), null, proactivity))).as(AgentProfileView.class);
    }

    /**
     * The main agent in the background. An event of its own it tells the user about; one whose data
     * says {@code look_into} makes it offer that work, and one that says {@code be} makes it try to
     * set how forward it is. What a worker reports, it tells the user.
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
        for (JsonNode event : waiting.path("events")) {
            JsonNode data = event.path("data");
            if (data.has("be")) {
                return ModelStep.of(custom("set_proactivity", args("level", data.path("be").asString())));
            }
            if (data.has("look_into")) {
                ObjectNode input = AgentJson.mapper().createObjectNode();
                input.put("role", "researcher").put("title", data.path("look_into").asString())
                        .put("instructions", "Research " + data.path("look_into").asString())
                        .put("summary", "I can look into " + data.path("look_into").asString() + " for you.");
                return ModelStep.of(custom("delegate_task", input.toString()));
            }
            return ModelStep.of(custom("notify_user", args("text", "Event: " + event.path("type").asString())));
        }
        for (JsonNode report : waiting.path("reports")) {
            if ("report".equals(report.path("kind").asString())) {
                return ModelStep.of(custom("notify_user", args("text", "About " + report.path("title").asString() + ": "
                        + report.path("text").asString())));
            }
        }
        return say("Noted.");
    }

    /**
     * The main agent in a conversation. "be X" records that the user wants it X; "refund X" hands X to
     * the payer and "research X" to the researcher.
     */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        if (context.step() > 0) {
            context.toolResults().forEach(this::heard);
            return say(context.resultOf("set_proactivity") != null ? "As you wish." : "Done.");
        }
        if (said.startsWith("be ")) {
            return ModelStep.of(custom("set_proactivity", args("level", said.substring("be ".length()))));
        }
        if (said.startsWith("refund ") || said.startsWith("research ")) {
            ObjectNode input = AgentJson.mapper().createObjectNode();
            input.put("role", said.startsWith("refund ") ? "payer" : "researcher").put("title", said).put("instructions", said);
            return ModelStep.of(custom("delegate_task", input.toString()));
        }
        return say("You said: " + said);
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
            if (last.path("text").asString("").startsWith("refund ")) {
                return ModelStep.of(mcp("payments", "refund", "{\"order\": \"1234\"}"));
            }
            return say(last.has("event") ? "Handled " + last.path("event").path("type").asString() + "." : "Did it.");
        }
        return say(context.resultOf("refund") == null ? "Finished." : "Refunded.");
    }

    private void heard(ToolOutcome answer) {
        answers.add(answer.tool() + (answer.error() ? " error: " : ": ") + answer.text());
    }

    // ================================================================ the three levels

    @Test
    void toStartWith_theMainAgentIsKeen_andIsWokenForEverything() {
        assertThat(acme.get(PREFERENCES).as(PreferencesView.class)).as("the user has said nothing")
                .isEqualTo(new PreferencesView(null, null, null));

        event("order.delayed", "order-1", "research", "{}");
        untilQuiet();

        assertThat(mainTurns()).as("told when the work started, and again when it was done")
                .containsExactly("conv:default silent", "conv:default notified");
        assertThat(mainRead.getFirst().path("reports")).extracting(report -> report.path("kind").asString()).containsExactly("assigned");
        assertThat(cma.userMessages(sessionOf("conv:default")).getFirst()).contains("This user likes to be kept up to date: tell "
                + "them briefly what has started for them, what waits for them and what came of it.");
    }

    @Test
    void aUserWhoWantsItBalanced_wakesTheMainAgentOnlyForWhatMayNeedThem() {
        PreferencesView set = alice.put(PREFERENCES, new PreferencesRequest("balanced")).as(PreferencesView.class);
        assertThat(set).extracting(PreferencesView::proactivity, PreferencesView::setBy, PreferencesView::updatedAt)
                .containsExactly("balanced", "user", clock.now());

        event("order.delayed", "order-1", "research", "{}");
        untilQuiet();

        assertThat(mainTurns()).as("not woken for what merely started").containsExactly("conv:default notified");
        assertThat(mainRead.getFirst().path("reports")).extracting(report -> report.path("kind").asString())
                .as("but it knows of it when it is woken").containsExactly("assigned", "report");
        assertThat(cma.userMessages(sessionOf("conv:default")).getFirst()).contains("This user wants to hear from you what needs "
                + "them and what came of the work, not what has merely started or moves along.");

        // A worker that waits for the user is something the user may have to be told.
        userSays(acme, USER, "refund order 1234");
        untilQuiet();
        assertThat(mainRead.getLast().path("reports")).extracting(report -> report.path("kind").asString()).containsExactly("waiting");
    }

    @Test
    void aUserWhoWantsItQuiet_hearsOnlyWhatCameOfIt_andIsOfferedNothing() {
        assertThat(acme.put(PREFERENCES, new PreferencesRequest("quiet")).as(PreferencesView.class).setBy()).isEqualTo("app");

        userSays(acme, USER, "refund order 1234");
        untilQuiet();
        assertThat(mainRead).as("not woken because a worker waits").isEmpty();
        ActionView action = alice.get("/v1/users/u42/actions").as(ActionPage.class).actions().getFirst();
        alice.post("/v1/users/u42/actions/" + action.actionId() + "/resolve", new ResolveActionRequest("approve", null, null))
                .as(ActionView.class);
        untilQuiet();

        assertThat(mainRead).hasSize(1);
        assertThat(mainRead.getFirst().path("reports")).extracting(report -> report.path("kind").asString())
                .as("everything it had not seen, with the report").containsExactly("waiting", "decided", "report");
        assertThat(repliesTo(acme, USER)).containsExactly("Done.", "About refund order 1234: Refunded.");
        assertThat(cma.userMessages(sessionOf("conv:default")).getLast()).contains("This user wants to hear from you only what is "
                + "key: what needs their decision, and the outcome of what they asked for themselves. Offer them no work that "
                + "they did not ask for.");

        // Work of its own initiative is not offered to such a user.
        event("order.delayed", "order-2", null, "{\"look_into\": \"why order 2 is late\"}");
        untilQuiet();
        assertThat(answers.getLast()).startsWith("delegate_task error: Not delegated, and not offered to the user: they have "
                + "asked to hear from you only what is key");
        assertThat(acme.get("/v1/users/u42/tasks?status=proposed").as(TaskPage.class).tasks()).isEmpty();
    }

    @Test
    void aUserWhoWantsItQuietIsSentFewerNotificationsInADay() {
        acme.put(PREFERENCES, new PreferencesRequest("quiet")).as(PreferencesView.class);

        for (int i = 1; i <= 6; i++) {
            clock.advance(Duration.ofMinutes(1));
            event("order.shipped", "order-" + i, null, "{}");
            untilQuiet();
        }

        assertThat(repliesTo(acme, USER)).hasSize(5);
        assertThat(answers.getLast()).isEqualTo("notify_user error: Not sent: this user has been sent 5 notifications in the last "
                + "24 hours, which is the limit. Do not try again in this turn.");

        // What came of work that the user asked for themselves is what they wait for: that still reaches them.
        userSays(acme, USER, "research the best route");
        untilQuiet();

        assertThat(repliesTo(acme, USER)).endsWith("Done.", "About research the best route: Did it.");
    }

    // ================================================================ whose word it is

    @Test
    void theUserTellsTheMainAgentThemselves_andItIsOnRecord() {
        userSays(acme, USER, "be quiet");
        untilQuiet();

        assertThat(answers.getLast()).isEqualTo("set_proactivity: Set to quiet. This user wants to hear from you only what is key: "
                + "what needs their decision, and the outcome of what they asked for themselves. Offer them no work that they did "
                + "not ask for.");
        assertThat(alice.get(PREFERENCES).as(PreferencesView.class)).extracting(PreferencesView::proactivity, PreferencesView::setBy)
                .containsExactly("quiet", "agent");
        Update recorded = updates("preferences.updated").getFirst();
        assertThat(recorded.data().path("proactivity").asString()).isEqualTo("quiet");
        assertThat(recorded.data().path("set_by").asString()).isEqualTo("agent");

        userSays(acme, USER, "be everything");
        untilQuiet();
        assertThat(answers.getLast()).isEqualTo("set_proactivity error: Not set: level is one of proactive, balanced, quiet.");
    }

    @Test
    void whatTheMainAgentReadsInTheBackgroundIsNotTheUsersWord() {
        event("ticket.updated", "ticket-1", null, "{\"be\": \"quiet\"}");
        untilQuiet();

        assertThat(answers.getLast()).startsWith("set_proactivity error: Not set: how much the user hears from you is theirs to say.");
        assertThat(acme.get(PREFERENCES).as(PreferencesView.class).proactivity()).isNull();
    }

    @Test
    void whereTheUserHasSaidNothing_theAgentsOwnDefaultApplies_andTheUsersWordGoesFirst() {
        mainAgent("quiet");

        event("order.delayed", "order-1", "research", "{}");
        untilQuiet();
        assertThat(mainTurns()).as("the agent's default: not woken for what merely started").containsExactly("conv:default notified");

        acme.put(PREFERENCES, new PreferencesRequest("proactive")).as(PreferencesView.class);
        event("order.delayed", "order-2", "research", "{}");
        untilQuiet();
        assertThat(mainTurns()).as("the user's own word: woken for everything")
                .containsExactly("conv:default notified", "conv:default silent", "conv:default notified");

        // Taking the user's own setting away brings the agent's default back.
        PreferencesView cleared = acme.put(PREFERENCES, new PreferencesRequest(null)).as(PreferencesView.class);
        assertThat(cleared.proactivity()).isNull();
        event("order.delayed", "order-3", "research", "{}");
        untilQuiet();
        assertThat(mainTurns()).hasSize(4);
        assertThat(updates("preferences.updated")).as("each change is recorded, and nothing else").hasSize(2);
        assertThat(acme.put(PREFERENCES, new PreferencesRequest(null)).as(PreferencesView.class)).isEqualTo(cleared);
        assertThat(updates("preferences.updated")).as("setting what is set already changes nothing").hasSize(2);
    }

    @Test
    void aLevelThatIsNotOneIsRefused() {
        TestApi.Reply user = acme.put(PREFERENCES, new PreferencesRequest("loud"));
        TestApi.Reply noBody = acme.put(PREFERENCES, null);
        TestApi.Reply agent = acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.",
                null, null, null, null, new CoordinatorConfig(List.of(), null, "loud")));

        assertThat(user.status()).isEqualTo(400);
        assertThat(user.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        assertThat(noBody.status()).isEqualTo(400);
        assertThat(agent.status()).isEqualTo(400);
        assertThat(acme.get("/v1/users/nobody-yet/preferences").as(PreferencesView.class)).as("a user nobody has mentioned yet")
                .isEqualTo(new PreferencesView(null, null, null));
    }

    // ================================================================ the demo agent

    @Test
    void theDemoAgentTakesTheUsersWordForIt() {
        cma.brain(new DemoAgent());

        userSays(acme, USER, "proactivity: quiet");
        untilQuiet();

        assertThat(repliesTo(acme, USER).getLast()).startsWith("[demo] Set to quiet. This user wants to hear from you only what is key");
        assertThat(acme.get(PREFERENCES).as(PreferencesView.class).proactivity()).isEqualTo("quiet");
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

    /** The turns of the main agent in the background, in order, as "line outcome". */
    private List<String> mainTurns() {
        return jdbc.sql("""
                SELECT merge_key || ' ' || outcome FROM turn
                 WHERE merge_key = 'conv:default' AND outcome IN ('silent', 'notified') ORDER BY created_at, run_id
                """).query(String.class).list();
    }

    private List<Update> updates(String type) {
        return acme.get("/v1/users/u42/updates").as(UpdatePage.class).updates().stream()
                .filter(update -> update.type().equals(type)).toList();
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
