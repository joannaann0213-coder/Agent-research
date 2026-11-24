package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.WorkerConfig;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A main agent stands by for the user. Its line takes one turn at a time, and many of its turns are
 * work in the background, so a user who writes would wait behind whatever turn happens to run. That
 * turn gives way instead: it is put aside, the user is answered, and it starts over afterwards,
 * without doing anything twice. Work that takes long is not on this line at all: it is a worker's,
 * in a session of its own, and holds up nobody.
 */
class YieldToTheUserIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String EVENTS = "/v1/users/u42/events";
    /** How long a turn that gave way stays out of the way, as configured by default. */
    private static final Duration YIELD_DELAY = Duration.ofSeconds(5);

    private TestApi acme;
    /** What read_events gave the main agent in a conversation, call by call. */
    private final List<JsonNode> chatRead = new ArrayList<>();

    @BeforeEach
    void aMainAgentWithAResearcher() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research",
                        "Looks things up and reports what it found.")), null))).as(AgentProfileView.class);
        chatRead.clear();
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    /** The main agent in the background: reads what waits, and tells the user about an event of its own or a worker's report. */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            return say("Done for now.");
        }
        JsonNode waiting = AgentJson.mapper().readTree(read.text());
        JsonNode event = waiting.path("events").path(0);
        if (!event.isMissingNode()) {
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

    /** The main agent in a conversation: reads the news it is pointed to, and repeats the user with the kinds of news. */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        if (context.step() == 0 && context.userMessage().contains("Call read_events")) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        List<String> news = new ArrayList<>();
        ToolOutcome read = context.resultOf("read_events");
        if (read != null) {
            JsonNode since = AgentJson.mapper().readTree(read.text());
            chatRead.add(since);
            since.path("reports").forEach(report -> news.add(report.path("kind").asString()));
        }
        return say("You said: " + said + (news.isEmpty() ? "" : ". News: " + String.join(", ", news)));
    }

    private ModelStep worker(ModelContext context) {
        return context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("Did it.");
    }

    // ================================================================ a turn in the background gives way

    @Test
    void aTurnInTheBackgroundGivesWayToTheUsersMessage_andStartsOverWhenTheUserHasTheirAnswer() {
        event("order.delayed", "order-1", null, "{}");
        deliver();
        // The user writes while the main agent is in the middle of the event.
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "hello"));

        assertThat(worker.runOnce()).isTrue();

        assertThat(jdbc.sql("SELECT state || ' ' || yields || ' ' || boundary_set FROM turn").query(String.class).single())
                .as("put aside: it has let go of the session").isEqualTo("open 1 false");
        assertThat(runs()).as("and its run waits in the Inbox").containsExactly("suspended attempt 1");
        assertThat(repliesTo(acme, USER)).as("nothing has been said yet").isEmpty();

        deliver();
        assertThat(worker.runOnce()).as("the user's message is next").isTrue();
        assertThat(repliesTo(acme, USER)).containsExactly("You said: hello");
        assertThat(worker.runOnce()).as("the event waits until its turn comes back").isFalse();

        clock.advance(YIELD_DELAY);
        assertThat(EmbeddedInbox.bean(SuspendSweeper.class).runOnce()).isEqualTo(1);
        untilQuiet();

        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).containsExactly(
                tuple("message", "hello"), tuple("reply", "You said: hello"), tuple("notification", "Event: order.delayed"));
        List<String> given = cma.userMessages(cma.sessionIds().getFirst());
        assertThat(cma.sessionIds()).as("all of it in the one session of the conversation").hasSize(1);
        assertThat(given).hasSize(3);
        assertThat(given.get(0)).contains("An event from the application's systems is waiting for you").doesNotContain("put aside");
        assertThat(given.get(1)).isEqualTo("hello");
        assertThat(given.get(2)).contains("what follows was put aside because the user wrote, and is due again now",
                "An event from the application's systems is waiting for you");
        assertThat(jdbc.sql("SELECT outcome FROM turn ORDER BY finished_at, run_id").query(String.class).list())
                .containsExactly("replied", "notified");
    }

    @Test
    void whatTheTurnHadAlreadyDoneIsNotDoneAgainWhenItStartsOver() {
        event("order.delayed", "order-1", null, "{}");
        deliver();
        // The user writes when the main agent has just sent its notification, and before it ended its turn.
        hooks.once(Point.ANSWERS_SENT, run -> { });
        hooks.once(Point.ANSWERS_SENT, run -> userSays(acme, USER, "hello"));

        assertThat(worker.runOnce()).isTrue();
        assertThat(repliesTo(acme, USER)).as("sent before the turn was put aside").containsExactly("Event: order.delayed");
        assertThat(runs()).containsExactly("suspended attempt 1");

        untilQuiet();
        clock.advance(YIELD_DELAY);
        EmbeddedInbox.bean(SuspendSweeper.class).runOnce();
        untilQuiet();

        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).as("told once")
                .containsExactly(tuple("notification", "Event: order.delayed"), tuple("message", "hello"),
                        tuple("reply", "You said: hello"));
        assertThat(jdbc.sql("SELECT outcome FROM turn ORDER BY finished_at, run_id").query(String.class).list())
                .containsExactly("replied", "notified");
    }

    @Test
    void aTurnThatHasNotBegunGivesWayWithoutTouchingTheSession() {
        event("order.delayed", "order-1", null, "{}");
        deliver();
        // The user's message is on its way to the Inbox when the event's run is handed out.
        userSays(acme, USER, "hello");

        assertThat(worker.runOnce()).isTrue();

        assertThat(runs()).containsExactly("suspended attempt 1");
        assertThat(givenToTheAgent()).as("the session has been given nothing").isEmpty();

        untilQuiet();
        clock.advance(YIELD_DELAY);
        EmbeddedInbox.bean(SuspendSweeper.class).runOnce();
        untilQuiet();

        assertThat(givenToTheAgent().getFirst()).as("the user first").isEqualTo("hello");
        assertThat(repliesTo(acme, USER)).containsExactly("You said: hello", "Event: order.delayed");
    }

    @Test
    void whatTheUsersTurnWasGivenMeanwhileIsNotDueAgain() {
        EventAccepted handed = event("order.delayed", "order-1", "research", "{}");
        deliver();
        assertThat(worker.runOnce()).as("the main agent is told that work was handed to its worker").isTrue();
        assertThat(worker.runOnce()).as("the worker does it and reports").isTrue();
        deliver();
        // The user writes while the main agent is in the middle of the report.
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "hello"));
        assertThat(worker.runOnce()).isTrue();
        assertThat(repliesTo(acme, USER)).isEmpty();

        untilQuiet();

        assertThat(chatRead.getFirst().path("reports").path(0).path("kind").asString()).isEqualTo("report");
        assertThat(chatRead.getFirst().path("reports").path(0).path("task_id").asString()).isEqualTo(handed.taskId());
        assertThat(repliesTo(acme, USER)).as("the user is answered with the news").containsExactly("You said: hello. News: report");

        clock.advance(YIELD_DELAY);
        assertThat(EmbeddedInbox.bean(SuspendSweeper.class).runOnce()).isEqualTo(1);
        untilQuiet();

        assertThat(repliesTo(acme, USER)).as("and not told of the report a second time").hasSize(1);
        assertThat(jdbc.sql("SELECT outcome FROM turn WHERE merge_key = 'conv:default' ORDER BY finished_at, run_id")
                .query(String.class).list())
                .as("the turn that gave way had nothing left to do").containsExactly("silent", "replied", "absorbed");
    }

    @Test
    void aTurnGivesWayOnlySoOften_soThatNothingKeepsItFromItsWorkForGood() {
        event("order.delayed", "order-1", null, "{}");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "one"));
        assertThat(worker.runOnce()).isTrue();
        untilQuiet();
        clock.advance(YIELD_DELAY);
        EmbeddedInbox.bean(SuspendSweeper.class).runOnce();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "two"));
        untilQuiet();
        clock.advance(YIELD_DELAY);
        EmbeddedInbox.bean(SuspendSweeper.class).runOnce();
        // The third time the user writes during it, the turn goes on to its end.
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "three"));
        untilQuiet();

        assertThat(repliesTo(acme, USER)).containsExactly("You said: one", "You said: two", "Event: order.delayed", "You said: three");
        assertThat(jdbc.sql("SELECT max(yields) FROM turn").query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void aWorkerOfTheServiceThatDiesWhileATurnGivesWayLosesNothing() {
        event("order.delayed", "order-1", null, "{}");
        deliver();
        hooks.once(Point.INPUT_RECORDED, run -> userSays(acme, USER, "hello"));
        hooks.crashAt(Point.YIELDED);

        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
        leaseRunsOut();
        untilQuiet();

        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).containsExactly(
                tuple("message", "hello"), tuple("reply", "You said: hello"), tuple("notification", "Event: order.delayed"));
    }

    // ================================================================ long work is elsewhere

    @Test
    void aWorkerInTheMiddleOfItsWorkHoldsUpNobody() {
        event("order.delayed", "order-1", "research", "{}");
        deliver();
        assertThat(worker.runOnce()).as("the main agent is told").isTrue();
        List<String> whileTheWorkerWorks = new ArrayList<>();
        // In the middle of the worker's turn the user writes, and is answered before the worker is done.
        hooks.once(Point.INPUT_RECORDED, run -> {
            userSays(acme, USER, "hello");
            deliver();
            worker.runOnce();
            whileTheWorkerWorks.addAll(repliesTo(acme, USER));
        });

        assertThat(worker.runOnce()).as("the worker's turn").isTrue();

        assertThat(whileTheWorkerWorks).containsExactly("You said: hello");
        untilQuiet();
        assertThat(repliesTo(acme, USER)).containsExactly("You said: hello", "About order.delayed: order-1: Did it.");
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

    private static String args(String... namesAndValues) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            input.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return input.toString();
    }
}
