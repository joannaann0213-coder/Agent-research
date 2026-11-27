package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.CreateScheduleRequest;
import dev.agentflow.agent.api.ScheduleView;
import dev.agentflow.agent.api.TaskNoteView;
import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.TaskView;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.inbox.server.timer.TimerScheduler;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * What was scheduled before a conversation had a main agent. Its timer stays on the line of its
 * subject, where the main agent would know nothing of it. When it falls due there, it is passed on:
 * to the main agent's own line, or to its worker as a task of the main agent. Nothing runs on the
 * old line any more.
 */
class EarlierSchedulesIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String SCHEDULES = "/v1/users/u42/schedules";
    private static final String BRIEF = "Look at the user's open orders and say what needs them today.";

    private TestApi acme;

    @BeforeEach
    void anApplicationWhoseAgentsAreOrdinaryToBeginWith() {
        acme = app(ACME);
        defaultAgent(acme);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? worker(context) : inTheBackground(context));
    }

    /** An agent in the background: tells the user what is due, or what a worker reported. */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() > 0) {
            ToolOutcome read = context.resultOf("read_events");
            if (read != null) {
                for (JsonNode report : AgentJson.mapper().readTree(read.text()).path("reports")) {
                    if ("report".equals(report.path("kind").asString())) {
                        return ModelStep.of(custom("notify_user", args("text", "About " + report.path("title").asString() + ": "
                                + report.path("text").asString())));
                    }
                }
            }
            return say("Done for now.");
        }
        if (context.userMessage().contains("It is time for")) {
            return ModelStep.of(custom("notify_user", args("text", "Your morning brief.")));
        }
        return context.userMessage().contains("Call read_events") ? ModelStep.of(custom("read_events", "{}")) : say("Noted.");
    }

    private ModelStep worker(ModelContext context) {
        return context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : say("Looked at the orders.");
    }

    /** The default agent becomes a main agent, with the researcher as its worker. */
    private void theAssistantBecomesAMainAgent() {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You are a helpful assistant.", null, null,
                null, null, new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research",
                        "Looks things up and reports what it found.")), null))).as(AgentProfileView.class);
    }

    @Test
    void whatWasScheduledForTheAgentBeforeItBecameAMainAgentComesToTheConversationThatItServes() {
        ScheduleView before = acme.post(SCHEDULES, new CreateScheduleRequest("brief", "Morning brief", BRIEF,
                clock.now().plus(Duration.ofHours(1)), null, null, "orders", null, null)).as(ScheduleView.class);
        assertThat(before.subject()).isEqualTo("orders");
        assertThat(jdbc.sql("SELECT merge_key FROM schedule").query(String.class).single()).isEqualTo("task:orders");
        theAssistantBecomesAMainAgent();

        clock.advance(Duration.ofHours(1));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isEqualTo(1);
        untilQuiet();

        assertThat(jdbc.sql("SELECT merge_key || ' ' || outcome FROM turn ORDER BY created_at, run_id").query(String.class).list())
                .as("nothing ran on the old line: the main agent did it, in its conversation").containsExactly("conv:default notified");
        assertThat(cma.sessionIds()).hasSize(1);
        assertThat(cma.sessionSpec(cma.sessionIds().getFirst()).metadata()).containsEntry("line", "conv:default");
        assertThat(cma.userMessages(cma.sessionIds().getFirst()).getFirst())
                .contains("It is time for \"Morning brief\", which the application scheduled", BRIEF);
        assertThat(repliesTo(acme, USER)).containsExactly("Your morning brief.");
        assertThat(jdbc.sql("SELECT state FROM schedule").query(String.class).single()).as("it ran once, and has run").isEqualTo("done");
    }

    @Test
    void whatWasScheduledForAnotherAgentBecomesATaskOfTheMainAgentThatServesTheConversationByNow() {
        acme.post(SCHEDULES, new CreateScheduleRequest("brief", "Morning brief", BRIEF, null, "0 8 * * *", "Asia/Shanghai",
                "orders", "research", null)).as(ScheduleView.class);
        assertThat(jdbc.sql("SELECT merge_key FROM schedule").query(String.class).single()).isEqualTo("task:orders");
        theAssistantBecomesAMainAgent();

        // 10:00 UTC is 18:00 in Shanghai. 8:00 in Shanghai is 0:00 UTC.
        clock.advance(Duration.ofHours(14));
        EmbeddedInbox.bean(TimerScheduler.class).runOnce();
        untilQuiet();
        clock.advance(Duration.ofDays(1));
        EmbeddedInbox.bean(TimerScheduler.class).runOnce();
        untilQuiet();

        TaskView task = acme.get("/v1/users/u42/tasks").as(TaskPage.class).tasks().getFirst();
        assertThat(task).extracting(TaskView::createdBy, TaskView::subject, TaskView::role, TaskView::agent, TaskView::title)
                .containsExactly("app", "orders", "researcher", "research", "Morning brief");
        assertThat(acme.get("/v1/users/u42/tasks/" + task.taskId()).as(TaskView.class).notes())
                .extracting(TaskNoteView::from, TaskNoteView::kind).as("both runs went to the one task")
                .containsExactly(tuple("app", "schedule"), tuple("app", "assigned"), tuple("worker", "report"),
                        tuple("app", "schedule"), tuple("app", "assigned"), tuple("worker", "report"));
        assertThat(count("SELECT count(*) FROM turn WHERE merge_key = 'task:orders'")).as("nothing ran on the old line").isZero();
        assertThat(repliesTo(acme, USER)).containsExactly("About Morning brief: Looked at the orders.",
                "About Morning brief: Looked at the orders.");
        assertThat(jdbc.sql("SELECT state FROM schedule").query(String.class).single()).isEqualTo("active");
    }

    @Test
    void whereNoMainAgentServesTheConversation_itRunsOnTheLineOfItsSubject_asItAlwaysHas() {
        acme.post(SCHEDULES, new CreateScheduleRequest("brief", "Morning brief", BRIEF, clock.now().plus(Duration.ofHours(1)), null,
                null, "orders", null, null)).as(ScheduleView.class);

        clock.advance(Duration.ofHours(1));
        EmbeddedInbox.bean(TimerScheduler.class).runOnce();
        untilQuiet();

        assertThat(jdbc.sql("SELECT merge_key || ' ' || outcome FROM turn").query(String.class).list())
                .containsExactly("task:orders notified");
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
