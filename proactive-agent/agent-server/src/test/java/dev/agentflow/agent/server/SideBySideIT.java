package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.agent.server.support.TestApi;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The work for a user is spread over many sessions: the main agent's, which takes one turn at a
 * time and keeps them short, and one per task, in which a worker does what takes long. Workers of
 * one user are at work side by side, and however many turns of workers an instance has under way,
 * a place is left for the user who writes.
 */
class SideBySideIT extends AgentTestBase {

    private static final String USER = "u42";

    private TestApi acme;

    @BeforeEach
    void aMainAgentWithAResearcher() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research",
                        "Looks things up and reports what it found.")), null))).as(AgentProfileView.class);
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    /** The main agent in a conversation: "research X" hands X to the researcher; anything else it repeats. */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        if (context.step() == 0 && context.userMessage().contains("Call read_events")) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        if (context.step() == 0 && said.startsWith("research ")) {
            String what = said.substring("research ".length());
            return ModelStep.of(custom("delegate_task", args("role", "researcher", "title", what, "instructions", "Research " + what)));
        }
        return say(context.resultOf("delegate_task") != null ? "On it." : "You said: " + said);
    }

    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            return say("Done for now.");
        }
        List<String> reported = new ArrayList<>();
        for (JsonNode report : AgentJson.mapper().readTree(read.text()).path("reports")) {
            if ("report".equals(report.path("kind").asString())) {
                reported.add(report.path("title").asString() + ": " + report.path("text").asString());
            }
        }
        return reported.isEmpty() ? say("Noted.") : ModelStep.of(custom("notify_user", args("text", "About " + String.join(", ", reported))));
    }

    private ModelStep worker(ModelContext context) {
        return context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("Did it.");
    }

    @Test
    void twoWorkersOfOneUserAreAtWorkAtTheSameTime() {
        userAsksFor("flights");
        userAsksFor("hotels");
        deliver();
        List<String> whileTheFirstWorks = new ArrayList<>();
        // The first worker is in the middle of its turn when the second is handed its task, and does it.
        hooks.once(Point.INPUT_RECORDED, run -> {
            assertThat(worker.runOnce()).as("a second worker of the same user gets a place").isTrue();
            whileTheFirstWorks.addAll(jdbc.sql("SELECT state FROM turn WHERE merge_key LIKE 'work:%' ORDER BY created_at, run_id")
                    .query(String.class).list());
        });

        assertThat(worker.runOnce()).as("the first worker's turn").isTrue();

        assertThat(whileTheFirstWorks).as("the turn of the one is under way while the turn of the other is over already")
                .containsExactly("open", "done");
        assertThat(cma.sessionIds()).as("the main agent's session, and one for each task").hasSize(3);
        untilQuiet();
        assertThat(repliesTo(acme, USER)).as("the main agent hears of both, and tells the user in one turn").hasSize(3);
        assertThat(repliesTo(acme, USER).getLast()).contains("flights: Did it.", "hotels: Did it.");
    }

    @Test
    void aPlaceThatIsKeptForUsersNeverTakesAWorkersTurn_andTakesTheUsersMessage() {
        userAsksFor("flights");
        deliver();
        assertThat(waitingRuns()).as("a worker's run waits to be taken").containsExactly("batch");

        assertThat(worker.runOnceForUsers()).as("not by a place that is kept for users").isFalse();

        userSays(acme, USER, "are you there?");
        deliver();
        assertThat(worker.runOnceForUsers()).as("which takes what the user wrote").isTrue();
        assertThat(repliesTo(acme, USER)).containsExactly("On it.", "You said: are you there?");
        assertThat(taskStatuses()).as("while the worker's turn has not begun").containsExactly("queued");

        untilQuiet();
        assertThat(repliesTo(acme, USER)).contains("About flights: Did it.");
    }

    // ================================================================ helpers

    /** The user asks for something that the main agent hands to a worker, and is told so. */
    private void userAsksFor(String what) {
        userSays(acme, USER, "research " + what);
        deliver();
        assertThat(worker.runOnce()).as("the main agent's turn").isTrue();
    }

    private List<String> taskStatuses() {
        return jdbc.sql("SELECT status FROM delegated_task ORDER BY created_at, task_id").query(String.class).list();
    }

    /** The lanes of the runs and events that the Inbox could hand out now. */
    private List<String> waitingRuns() {
        return EmbeddedInbox.jdbc().sql("SELECT DISTINCT lane FROM event WHERE status = 'pending' ORDER BY lane").query(String.class).list();
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
