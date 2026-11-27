package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.LevelStats;
import dev.agentflow.agent.api.MessageStats;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.PreferencesRequest;
import dev.agentflow.agent.api.PreferencesView;
import dev.agentflow.agent.api.StatsView;
import dev.agentflow.agent.api.TaskStats;
import dev.agentflow.agent.api.TurnStats;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.inbox.server.sweep.SuspendSweeper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * How much an agent does unasked is a setting, and a setting is tuned by numbers. The service says,
 * for a user and for an application, what the agents' turns were, how they ended, what they used
 * and how long users waited: read from what each turn records about itself.
 */
class StatsIT extends AgentTestBase {

    private static final String USER = "u42";

    @Autowired
    private MeterRegistry meters;

    private TestApi acme;

    @BeforeEach
    void aMainAgentWithAResearcher() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research",
                        "Looks things up and reports what it found.")), null))).as(AgentProfileView.class);
        // Every request to the model: 1,000 tokens of input, of which 800 come from the cache, and 50 of output.
        cma.usagePerRequest(200, 50, 800, 0);
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    /** The main agent in a conversation: "research X" hands X to the researcher, in two steps; anything else it answers in one. */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        if (context.step() == 0 && said.startsWith("research ")) {
            String what = said.substring("research ".length());
            return ModelStep.of(custom("delegate_task", args("role", "researcher", "title", what, "instructions", "Research " + what)));
        }
        return say(context.resultOf("delegate_task") != null ? "On it." : "You said: " + said);
    }

    /** The main agent in the background: reads what waits, tells the user of a report, and says nothing about anything else. */
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

    @Test
    void forAUser_theTurnsByKindAndOutcome_whatTheyUsed_andHowLongTheUserWaited() {
        userSays(acme, USER, "hello");
        untilQuiet();
        userSays(acme, USER, "research flights");
        untilQuiet();

        StatsView stats = acme.get("/v1/users/" + USER + "/stats").as(StatsView.class);

        assertThat(stats.since()).isEqualTo(stats.until().minus(Duration.ofHours(24)));
        assertThat(stats.until()).isEqualTo(clock.now());
        assertThat(stats.turns()).extracting(TurnStats::kind, TurnStats::outcome, TurnStats::turns, TurnStats::modelRequests)
                .as("the reply to hello took one request, the one that delegated two; the worker read and reported; "
                        + "the main agent read the report, told the user and ended its turn")
                .containsExactly(tuple("background", "notified", 1L, 3L), tuple("chat", "replied", 2L, 3L),
                        tuple("worker", "reported", 1L, 2L));
        TurnStats chats = stats.turns().get(1);
        assertThat(List.of(chats.inputTokens(), chats.outputTokens(), chats.cacheReadTokens(), chats.cacheCreationTokens()))
                .as("three requests").containsExactly(600L, 150L, 2400L, 0L);
        assertThat(stats.replies().replies()).isEqualTo(2);
        assertThat(stats.replies().medianSeconds()).as("the Inbox's quiet window is all the user waited in a test").isEqualTo(2.0);
        assertThat(stats.replies().p95Seconds()).isEqualTo(2.0);
        assertThat(stats.messages()).isEqualTo(new MessageStats(2, 2, 1, 0, 0));
        assertThat(stats.tasks()).as("one task, made by the main agent because the user asked for it")
                .isEqualTo(new TaskStats(1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0));
        assertThat(stats.proactivity()).as("the user has not said how forward they want it").isNull();
        assertThat(stats.users()).as("only for an application").isNull();
        assertThat(stats.levels()).isNull();
    }

    @Test
    void forAnApplication_allItsUsersTogether_andTheUsersByHowForwardTheyWantTheirAgent() {
        userSays(acme, USER, "research flights");
        userSays(acme, "u7", "hello");
        acme.put("/v1/users/u7/preferences", new PreferencesRequest("quiet")).as(PreferencesView.class);
        untilQuiet();

        StatsView stats = acme.get("/v1/stats").as(StatsView.class);

        assertThat(stats.users()).isEqualTo(2);
        assertThat(stats.turns()).extracting(TurnStats::kind, TurnStats::outcome, TurnStats::turns)
                .containsExactly(tuple("background", "notified", 1L), tuple("chat", "replied", 2L), tuple("worker", "reported", 1L));
        assertThat(stats.messages().fromUser()).isEqualTo(2);
        assertThat(stats.levels()).as("one user has not said and was told of the result; the other wants it quiet")
                .containsExactly(new LevelStats("default", 1, 1), new LevelStats("quiet", 1, 0));
        assertThat(stats.proactivity()).as("only for one user").isNull();
        assertThat(acme.get("/v1/users/u7/stats").as(StatsView.class).proactivity()).isEqualTo("quiet");
        assertThat(app(GLOBEX).get("/v1/stats").as(StatsView.class).users()).as("another application sees none of it").isZero();
    }

    @Test
    void aReplyThatWasEnded_aTurnThatGaveWay_andARunThatFoundNothingLeft_allShow() {
        // A reply is ended because the user wrote again; what its request to the model used is used all the same.
        userSays(acme, USER, "book a table");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "for four"));
        untilQuiet();
        // A turn in the background gives way to the user, and starts over.
        acme.post("/v1/users/" + USER + "/events", new PostEventRequest(null, "order.delayed", "order-1", null, null,
                AgentJson.mapper().readTree("{}"), null)).as(Object.class);
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "hello"));
        untilQuiet();
        clock.advance(Duration.ofSeconds(5));
        EmbeddedInbox.bean(SuspendSweeper.class).runOnce();
        untilQuiet();

        StatsView stats = acme.get("/v1/users/" + USER + "/stats").as(StatsView.class);

        assertThat(stats.turns()).extracting(TurnStats::kind, TurnStats::outcome, TurnStats::turns, TurnStats::yields,
                        TurnStats::modelRequests)
                .containsExactly(
                        // read the event, said "Noted."; before that, one request that was cut short when it gave way
                        tuple("background", "silent", 1L, 1L, 3L),
                        tuple("chat", "replied", 2L, 0L, 2L),
                        tuple("chat", "superseded", 1L, 0L, 1L));
        assertThat(stats.replies().replies()).as("the user got two replies").isEqualTo(2);
        assertThat(stats.replies().p95Seconds()).as("and waited longer for the one that was written twice")
                .isGreaterThan(stats.replies().medianSeconds());
    }

    @Test
    void aRunWhoseMessageAnotherTurnAnswered_andOneWhoseReplyTheUserStopped_areChatsThatCostNothing() {
        // The user writes the rest while the turn gets ready: one turn answers both, and the second run finds nothing left.
        userSays(acme, USER, "book a table");
        deliver();
        hooks.once(Point.BOUNDARY_FIXED, run -> userSays(acme, USER, "for four"));
        untilQuiet();
        // And a message that the user takes back before anything began.
        userSays(acme, USER, "never mind");
        acme.post("/v1/users/" + USER + "/conversations/default/stop", null).as(Object.class);
        untilQuiet();

        StatsView stats = acme.get("/v1/users/" + USER + "/stats").as(StatsView.class);

        assertThat(stats.turns()).extracting(TurnStats::kind, TurnStats::outcome, TurnStats::turns, TurnStats::modelRequests)
                .containsExactly(tuple("chat", "absorbed", 1L, 0L), tuple("chat", "replied", 1L, 1L), tuple("chat", "stopped", 1L, 0L));
        assertThat(stats.replies().replies()).isEqualTo(1);
    }

    @Test
    void aTurnThatIsUnderWayIsNotCountedYet() {
        userSays(acme, USER, "hello");
        deliver();
        List<StatsView> meanwhile = new ArrayList<>();
        // The span reaches past this moment, so that it is not the time that keeps the turn out.
        String soon = clock.now().plus(Duration.ofMinutes(1)).toString();
        hooks.once(Point.INPUT_RECORDED,
                run -> meanwhile.add(acme.get("/v1/users/" + USER + "/stats?until=" + soon).as(StatsView.class)));

        untilQuiet();

        assertThat(meanwhile.getFirst().turns()).as("while the agent answers").isEmpty();
        assertThat(meanwhile.getFirst().messages().fromUser()).as("the message is there already").isEqualTo(1);
        assertThat(acme.get("/v1/users/" + USER + "/stats?until=" + soon).as(StatsView.class).turns()).hasSize(1);
    }

    @Test
    void theSpanOfTimeIsTheCallers_withinLimits() {
        userSays(acme, USER, "hello");
        untilQuiet();
        String now = clock.now().toString();
        String anHourAgo = clock.now().minus(Duration.ofHours(1)).toString();
        String lastYear = clock.now().minus(Duration.ofDays(365)).toString();

        assertThat(acme.get("/v1/users/" + USER + "/stats?since=" + anHourAgo + "&until=" + now).as(StatsView.class)
                .messages().fromUser()).isEqualTo(1);
        assertThat(acme.get("/v1/users/" + USER + "/stats?since=" + lastYear + "&until=" + anHourAgo).status())
                .as("more than 92 days").isEqualTo(400);
        assertThat(acme.get("/v1/stats?until=" + anHourAgo).as(StatsView.class).messages().fromUser())
                .as("the 24 hours before an hour ago").isZero();
        assertThat(acme.get("/v1/stats?since=yesterday").status()).isEqualTo(400);
        assertThat(acme.get("/v1/stats?since=" + now + "&until=" + anHourAgo).status()).as("backwards").isEqualTo(400);
        assertThat(acme.get("/v1/users/nobody/stats").as(StatsView.class).turns()).as("a user nobody mentioned has no numbers")
                .isEmpty();
        assertThat(user(acme, USER).get("/v1/users/" + USER + "/stats").status()).as("not for a user's own client").isEqualTo(403);
        assertThat(user(acme, USER).get("/v1/stats").status()).isEqualTo(403);
        assertThat(anonymous.get("/v1/stats").status()).isEqualTo(401);
    }

    @Test
    void theMetersOfTheServiceCountTheSameTurns() {
        double before = turnsCounted("chat", "replied");
        userSays(acme, USER, "hello");
        untilQuiet();

        assertThat(turnsCounted("chat", "replied") - before).isEqualTo(1.0);
        assertThat(meters.get("agent.turn.tokens").tag("kind", "chat").tag("type", "cache_read").counter().count())
                .isGreaterThanOrEqualTo(800.0);
        assertThat(meters.get("agent.reply.wait").timer().count()).isPositive();
    }

    // ================================================================ helpers

    private double turnsCounted(String kind, String outcome) {
        return meters.find("agent.turns").tag("kind", kind).tag("outcome", outcome).counters().stream()
                .mapToDouble(counter -> counter.count()).sum();
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
