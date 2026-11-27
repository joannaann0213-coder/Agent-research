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
import dev.agentflow.agent.api.CreateScheduleRequest;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.ScheduleView;
import dev.agentflow.agent.api.TaskNoteView;
import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.TaskView;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.UpdatePage;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.AgentSpec;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.agent.server.support.SimulatedCrash;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.agent.server.tools.DemoAgent;
import dev.agentflow.inbox.server.timer.TimerScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A main agent: the one session through which everything about a user passes, and which hands work
 * to workers. The workers run in sessions of their own; what they report comes back to the main
 * agent as input of its line, and the user hears of it from the main agent only.
 */
class CoordinatorIT extends AgentTestBase {

    private static final String USER = "u42";

    private TestApi acme;
    /** What read_events gave the main agent in the background, call by call. */
    private final List<JsonNode> mainRead = new ArrayList<>();
    /** What read_events gave the workers, call by call. */
    private final List<JsonNode> workerRead = new ArrayList<>();
    /** What the tools answered the main agent in conversations, call by call. */
    private final List<String> answers = new ArrayList<>();
    /** The task the main agent delegated last. */
    private String lastTask;
    /** The script that plays the workers' agents. */
    private Function<ModelContext, ModelStep> workerScript;

    @BeforeEach
    void aMainAgentWithAResearcher() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        acme.put("/v1/agents/default", mainAgent("You look after the user.", null)).as(AgentProfileView.class);
        mainRead.clear();
        workerRead.clear();
        answers.clear();
        lastTask = null;
        workerScript = this::researching;
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? workerScript.apply(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    private static AgentProfileRequest mainAgent(String instructions, Integer maxActiveTasks) {
        return new AgentProfileRequest("Assistant", null, null, instructions, null, null, null, null, new CoordinatorConfig(
                List.of(new WorkerConfig("researcher", "research", "Looks things up and reports what it found.")), maxActiveTasks));
    }

    /**
     * The main agent in a conversation. It does what the user's words say: "research X" hands X to the
     * researcher, "more X" sends the worker of the last task a further word, "close" closes that task,
     * "list" lists the open ones. Anything else it repeats.
     */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        if (context.step() == 0) {
            if (said.startsWith("research ")) {
                String what = said.substring("research ".length());
                return ModelStep.of(custom("delegate_task", args("role", "researcher", "title", what, "instructions", "Research " + what)));
            }
            if (said.startsWith("more ")) {
                return ModelStep.of(custom("message_worker", args("task_id", lastTask, "text", said.substring("more ".length()))));
            }
            if (said.equals("close")) {
                return ModelStep.of(custom("close_task", args("task_id", lastTask, "reason", "the user has what they wanted")));
            }
            if (said.equals("list")) {
                return ModelStep.of(custom("list_tasks", "{}"));
            }
            return say("You said: " + said);
        }
        ToolOutcome answer = context.toolResults().getFirst();
        answers.add((answer.error() ? "error: " : "") + answer.text());
        if ("delegate_task".equals(answer.tool()) && !answer.error()) {
            lastTask = AgentJson.mapper().readTree(answer.text()).path("task_id").asString();
            return say("On it: " + lastTask);
        }
        return say(answer.error() ? "That did not work." : "Done.");
    }

    /** The main agent in the background: reads what is waiting and tells the user about the first thing in it. */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return context.userMessage().contains("Call read_events") ? ModelStep.of(custom("read_events", "{}")) : say("Looked around.");
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            return say("Told the user.");
        }
        JsonNode waiting = AgentJson.mapper().readTree(read.text());
        mainRead.add(waiting);
        JsonNode report = waiting.path("reports").path(0);
        if (!report.isMissingNode()) {
            return ModelStep.of(custom("notify_user", args("text", "About " + report.path("title").asString() + ": "
                    + report.path("text").asString())));
        }
        JsonNode event = waiting.path("events").path(0);
        if (!event.isMissingNode()) {
            return ModelStep.of(custom("notify_user", args("text", "Event: " + event.path("type").asString())));
        }
        return say("Nothing to tell.");
    }

    /** A worker: reads what the main agent wants and reports on the last of it. */
    private ModelStep researching(ModelContext context) {
        if (context.step() == 0) {
            return ModelStep.of(custom("read_events", "{}"));
        }
        JsonNode handed = AgentJson.mapper().readTree(context.resultOf("read_events").text());
        workerRead.add(handed);
        JsonNode messages = handed.path("messages");
        return say("Researched: " + messages.path(messages.size() - 1).path("text").asString());
    }

    // ================================================================ the main agent and its workers

    @Test
    void theMainAgentHandsWorkToAWorker_whoseReportComesBackToIt_andTheUserHearsOfItFromTheMainAgent() {
        userSays(acme, USER, "research flights to Tokyo");

        assertThat(settle()).as("the main agent's turn").isEqualTo(1);

        TaskView delegated = tasks(acme).getFirst();
        assertThat(delegated.taskId()).startsWith("task_").isEqualTo(lastTask);
        assertThat(delegated).extracting(TaskView::conversationId, TaskView::role, TaskView::agent, TaskView::title, TaskView::status)
                .containsExactly("default", "researcher", "research", "flights to Tokyo", "queued");
        assertThat(repliesTo(acme, USER)).as("the user is answered at once, before the work is done")
                .containsExactly("On it: " + lastTask);

        assertThat(settle()).as("the worker's turn").isEqualTo(1);

        String workerSession = sessionOf("work:" + lastTask);
        assertThat(cma.agentSpec(cma.sessionSpec(workerSession).agent()).description()).isEqualTo("Researcher");
        assertThat(cma.userMessages(workerSession)).hasSize(1);
        assertThat(cma.userMessages(workerSession).getFirst())
                .as("what the main agent wants is not given as a message: the worker reads it with a tool")
                .contains("The user's main agent has handed you work on a task. Call read_events to get it",
                        "is your report to the main agent")
                .doesNotContain("Tokyo");
        assertThat(workerRead).hasSize(1);
        assertThat(workerRead.getFirst().path("task").path("title").asString()).isEqualTo("flights to Tokyo");
        JsonNode handed = workerRead.getFirst().path("messages").get(0);
        assertThat(handed.path("from").asString()).isEqualTo("the main agent");
        assertThat(handed.path("kind").asString()).isEqualTo("assignment");
        assertThat(handed.path("text").asString()).isEqualTo("Research flights to Tokyo");
        assertThat(tasks(acme).getFirst()).extracting(TaskView::status, TaskView::report)
                .containsExactly("idle", "Researched: Research flights to Tokyo");
        assertThat(repliesTo(acme, USER)).as("the worker's report is not the user's to read").hasSize(1);

        assertThat(settle()).as("the main agent's turn on the report").isEqualTo(1);

        String mainSession = sessionOf("conv:default");
        assertThat(cma.sessionIds()).as("the main agent's session and the worker's").hasSize(2);
        assertThat(cma.userMessages(mainSession)).hasSize(2);
        assertThat(cma.userMessages(mainSession).get(1))
                .contains("The user is not here", "A worker has something for you on a task you delegated. Call read_events to get it");
        JsonNode report = mainRead.getFirst().path("reports").get(0);
        assertThat(report.path("task_id").asString()).isEqualTo(lastTask);
        assertThat(report.path("role").asString()).isEqualTo("researcher");
        assertThat(report.path("from").asString()).isEqualTo("the worker");
        assertThat(report.path("kind").asString()).isEqualTo("report");
        assertThat(messagesOf(acme, USER)).extracting(MessageView::role, MessageView::kind, MessageView::text).containsExactly(
                tuple("user", "message", "research flights to Tokyo"),
                tuple("agent", "reply", "On it: " + lastTask),
                tuple("agent", "notification", "About flights to Tokyo: Researched: Research flights to Tokyo"));
        assertThat(jdbc.sql("SELECT merge_key || ' ' || outcome FROM turn ORDER BY created_at, run_id").query(String.class).list())
                .containsExactly("conv:default replied", "work:" + lastTask + " reported", "conv:default notified");
        assertThat(EmbeddedInbox.jdbc().sql("SELECT type || ' ' || lane || ' ' || merge_key FROM event ORDER BY seq")
                .query(String.class).list()).as("each side wakes the other through the Inbox").containsExactly(
                        "agent.user_message.v1 interactive conv:default",
                        "agent.task_message.v1 batch work:" + lastTask,
                        "agent.task_report.v1 background conv:default");
        assertThat(settle()).as("nothing is left to do").isZero();
    }

    @Test
    void aFurtherWordOfTheMainAgentReachesTheSameWorker_whichRemembersTheTask() {
        userSays(acme, USER, "research flights to Tokyo");
        untilQuiet();

        userSays(acme, USER, "more only direct ones");
        assertThat(settle()).isEqualTo(1);
        assertThat(answers.getLast()).isEqualTo("Sent. The worker's next report wakes you.");
        assertThat(tasks(acme).getFirst().status()).isEqualTo("queued");
        untilQuiet();

        assertThat(cma.sessionIds()).as("still one session for the worker").hasSize(2);
        assertThat(cma.userMessages(sessionOf("work:" + lastTask))).hasSize(2);
        assertThat(workerRead.get(1).path("messages").get(0).path("kind").asString()).isEqualTo("message");
        assertThat(workerRead.get(1).has("earlier")).as("the session knows the task already").isFalse();
        TaskView task = acme.get("/v1/users/u42/tasks/" + lastTask).as(TaskView.class);
        assertThat(task.status()).isEqualTo("idle");
        assertThat(task.notes()).extracting(TaskNoteView::seq, TaskNoteView::from, TaskNoteView::kind, TaskNoteView::text).containsExactly(
                tuple(1, "main", "assignment", "Research flights to Tokyo"),
                tuple(2, "worker", "report", "Researched: Research flights to Tokyo"),
                tuple(3, "main", "message", "only direct ones"),
                tuple(4, "worker", "report", "Researched: only direct ones"));
        assertThat(repliesTo(acme, USER)).endsWith("About flights to Tokyo: Researched: only direct ones");
    }

    @Test
    void aWorkerReachesNobodyButTheMainAgent() {
        List<String> refusals = new ArrayList<>();
        workerScript = context -> {
            if (context.step() == 0) {
                return ModelStep.of(custom("notify_user", args("text", "Hello from the worker.")));
            }
            ToolOutcome refused = context.toolResults().getFirst();
            refusals.add((refused.error() ? "error: " : "") + refused.text());
            return context.step() == 1 ? ModelStep.of(custom("ask_user", args("question", "Which airline?")))
                    : say("I need to know which airline.");
        };
        userSays(acme, USER, "research flights to Tokyo");

        untilQuiet();

        assertThat(refusals).hasSize(2);
        assertThat(refusals.get(0)).startsWith("error: Not sent: in a task you report to the main agent");
        assertThat(refusals.get(1)).startsWith("error: Not asked: in a task you ask the main agent, not the user");
        assertThat(count("SELECT count(*) FROM action")).as("nothing waits for the user").isZero();
        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).as("the question reaches the user through the main agent")
                .containsExactly(tuple("message", "research flights to Tokyo"), tuple("reply", "On it: " + lastTask),
                        tuple("notification", "About flights to Tokyo: I need to know which airline."));
    }

    @Test
    void whatHappensForTheUserReachesTheMainAgentsOwnSession_notASessionPerSubject() {
        EventAccepted accepted = acme.post("/v1/users/u42/events", new PostEventRequest("delayed-1234", "order.delayed", "order-1234",
                null, null, AgentJson.mapper().readTree("{\"order_id\": \"1234\"}"), null)).as(EventAccepted.class);
        assertThat(accepted.subject()).isEqualTo("order-1234");

        assertThat(settle()).isEqualTo(1);

        assertThat(cma.sessionIds()).hasSize(1);
        assertThat(cma.sessionSpec(cma.sessionIds().getFirst()).metadata()).containsEntry("line", "conv:default");
        assertThat(jdbc.sql("SELECT merge_key || ' ' || kind FROM thread").query(String.class).list()).containsExactly("conv:default chat");
        assertThat(mainRead.getFirst().path("events").get(0).path("subject").asString()).isEqualTo("order-1234");
        assertThat(mainRead.getFirst().has("reports")).isFalse();
        assertThat(EmbeddedInbox.jdbc().sql("SELECT lane || ' ' || merge_key FROM event").query(String.class).single())
                .as("in the background, so that it never makes the user wait for a reply").isEqualTo("background conv:default");

        userSays(acme, USER, "hello");
        assertThat(settle()).isEqualTo(1);

        assertThat(cma.sessionIds()).as("the conversation is the same session, which knows of the event").hasSize(1);
        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).containsExactly(
                tuple("notification", "Event: order.delayed"), tuple("message", "hello"), tuple("reply", "You said: hello"));
    }

    @Test
    void aScheduleOfAMainAgentRunsInItsConversation() {
        Instant at = clock.now().plus(Duration.ofHours(1));
        ScheduleView created = acme.post("/v1/users/u42/schedules", new CreateScheduleRequest("look", "Morning look",
                "See whether anything needs the user.", at, null, null, null, null, null)).as(ScheduleView.class);
        assertThat(created.subject()).as("it runs on the conversation's line, not on a subject's").isNull();

        clock.advance(Duration.ofHours(1));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isEqualTo(1);
        assertThat(settle()).isEqualTo(1);

        String session = cma.sessionIds().getFirst();
        assertThat(cma.sessionSpec(session).metadata()).containsEntry("line", "conv:default");
        assertThat(cma.userMessages(session).getFirst()).contains("It is time for \"Morning look\"");
    }

    @Test
    void anEventForAMainAgentNeedsAConversationThatItServes() {
        acme.put("/v1/agents/sales", new AgentProfileRequest("Sales", null, null, "You sell.", null, null)).as(AgentProfileView.class);
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "deals", "sales", "hi")).as(MessageAccepted.class);

        TestApi.Reply refused = acme.post("/v1/users/u42/events",
                new PostEventRequest(null, "order.delayed", "order-1", "default", "deals", null, null));

        assertThat(refused.status()).isEqualTo(409);
        assertThat(refused.code()).isEqualTo(ProblemCodes.CONFLICT);
        assertThat(refused.body().path("detail").asString()).contains("conversation deals is served by agent sales");
    }

    // ================================================================ once, and within limits

    @Test
    void aTaskIsDelegatedOnce_whenTheWorkerOfTheServiceDiesRightAfter() {
        userSays(acme, USER, "research flights to Tokyo");
        deliver();
        hooks.crashAt(Point.TOOLS_RUN);

        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
        assertThat(count("SELECT count(*) FROM delegated_task")).as("the task is on record; the agent has not been told").isEqualTo(1);
        leaseRunsOut();
        assertThat(worker.runOnce()).isTrue();

        assertThat(count("SELECT count(*) FROM delegated_task")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM task_note")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM inbox_relay WHERE request ->> 'type' = 'agent.task_message.v1'")).isEqualTo(1);
        assertThat(repliesTo(acme, USER)).containsExactly("On it: " + lastTask);
        untilQuiet();
        assertThat(workerRead).as("the worker was handed the task once").hasSize(1);
    }

    @Test
    void onlySoManyTasksAreUnderWayAtATime() {
        acme.put("/v1/agents/default", mainAgent("You look after the user.", 1)).as(AgentProfileView.class);
        userSays(acme, USER, "research flights to Tokyo");
        assertThat(settle()).isEqualTo(1);

        userSays(acme, USER, "research hotels in Tokyo");
        settle();

        assertThat(answers.getLast()).startsWith("error: Not delegated: 1 tasks are queued or being worked on, which is the limit.");
        assertThat(tasks(acme)).hasSize(1);
    }

    // ================================================================ closing a task

    @Test
    void theMainAgentClosesATaskItNoLongerNeeds_andNothingMoreGoesToItsWorker() {
        userSays(acme, USER, "research flights to Tokyo");
        untilQuiet();

        userSays(acme, USER, "close");
        untilQuiet();

        assertThat(answers.getLast()).isEqualTo("Closed.");
        assertThat(tasks(acme)).as("a closed task is not among the open ones").isEmpty();
        TaskView closed = acme.get("/v1/users/u42/tasks?status=all").as(TaskPage.class).tasks().getFirst();
        assertThat(closed).extracting(TaskView::status, TaskView::closedBy, TaskView::closedReason)
                .containsExactly("closed", "agent", "the user has what they wanted");
        assertThat(mainRead).as("the main agent closed it itself and is not told so").hasSize(1);

        userSays(acme, USER, "more and trains too");
        untilQuiet();

        assertThat(answers.getLast()).startsWith("error: Not sent: the task is closed.");
        assertThat(workerRead).hasSize(1);
    }

    @Test
    void theUserStopsATaskBeforeItsWorkerBegins_andTheMainAgentIsTold() {
        userSays(acme, USER, "research flights to Tokyo");
        settle();
        TestApi alice = user(acme, USER);

        TaskView closed = alice.post("/v1/users/u42/tasks/" + lastTask + "/close", null).as(TaskView.class);

        assertThat(closed).extracting(TaskView::status, TaskView::closedBy).containsExactly("closed", "user");
        assertThat(alice.post("/v1/users/u42/tasks/" + lastTask + "/close", null).as(TaskView.class).closedBy())
                .as("closing it again changes nothing").isEqualTo("user");
        untilQuiet();

        assertThat(cma.sessionIds()).as("the worker never got a session").hasSize(1);
        assertThat(workerRead).isEmpty();
        JsonNode told = mainRead.getFirst().path("reports").get(0);
        assertThat(told.path("from").asString()).isEqualTo("the service");
        assertThat(told.path("kind").asString()).isEqualTo("closed");
        assertThat(told.path("text").asString()).isEqualTo("The user closed this task.");
        assertThat(jdbc.sql("SELECT outcome FROM turn WHERE merge_key = :line").param("line", "work:" + lastTask)
                .query(String.class).single()).isEqualTo("closed");
    }

    @Test
    void aTaskThatIsClosedWhileItsWorkerWorks_stopsTheWorker_andNothingMoreIsReported() {
        userSays(acme, USER, "research flights to Tokyo");
        untilQuiet();
        cma.hold(sessionOf("work:" + lastTask));
        userSays(acme, USER, "more only direct ones");
        assertThat(settle()).as("the main agent's turn, which sends the worker more").isEqualTo(1);
        TestApi alice = user(acme, USER);
        hooks.once(Point.INPUT_RECORDED, run -> assertThat(alice.post("/v1/users/u42/tasks/" + lastTask + "/close", null)
                .as(TaskView.class).status()).isEqualTo("closed"));

        untilQuiet();

        assertThat(interrupts()).as("the worker's session is stopped").isEqualTo(1);
        assertThat(jdbc.sql("SELECT outcome FROM turn WHERE merge_key = :line ORDER BY created_at, run_id")
                .param("line", "work:" + lastTask).query(String.class).list()).containsExactly("reported", "closed");
        assertThat(acme.get("/v1/users/u42/tasks/" + lastTask).as(TaskView.class).notes()).extracting(TaskNoteView::kind)
                .as("a task that is over reports nothing").containsExactly("assignment", "report", "message", "closed");
        assertThat(mainRead.getLast().path("reports").get(0).path("text").asString()).isEqualTo("The user closed this task.");
    }

    @Test
    void tasksAreForTheApplicationAndTheUserToSee_butOnlyTheApplicationReadsWhatWasSaid() {
        userSays(acme, USER, "research flights to Tokyo");
        untilQuiet();
        TestApi alice = user(acme, USER);

        assertThat(alice.get("/v1/users/u42/tasks/" + lastTask).as(TaskView.class).notes())
                .as("a worker may have read what only the application is to see").isNull();
        assertThat(acme.get("/v1/users/u42/tasks/" + lastTask).as(TaskView.class).notes()).hasSize(2);
        assertThat(alice.get("/v1/users/u42/tasks/" + lastTask).as(TaskView.class).report())
                .as("the user hears of a worker only what the main agent tells them").isNull();
        assertThat(alice.get("/v1/users/u42/tasks").as(TaskPage.class).tasks().getFirst().report()).isNull();
        assertThat(acme.get("/v1/users/u42/tasks").as(TaskPage.class).tasks().getFirst().report()).startsWith("Researched: ");
        assertThat(alice.get("/v1/users/u42/tasks?conversation_id=default").as(TaskPage.class).tasks())
                .extracting(TaskView::taskId).containsExactly(lastTask);
        assertThat(acme.get("/v1/users/u42/tasks?conversation_id=other").as(TaskPage.class).tasks()).isEmpty();
        assertThat(acme.get("/v1/users/u42/tasks?status=done").status()).isEqualTo(400);
        assertThat(acme.get("/v1/users/u42/tasks/task_none").status()).isEqualTo(404);
        assertThat(acme.post("/v1/users/u42/tasks/task_none/close", null).status()).isEqualTo(404);
        assertThat(app(GLOBEX).get("/v1/users/u42/tasks/" + lastTask).status()).as("another application's task").isEqualTo(404);

        List<Update> updates = acme.get("/v1/users/u42/updates").as(UpdatePage.class).updates();
        assertThat(updates).extracting(Update::type).contains("task.created", "task.updated");
        assertThat(updates.stream().filter(update -> update.type().equals("task.created")).findFirst().orElseThrow()
                .data().path("task_id").asString()).isEqualTo(lastTask);
        assertThat(updates.stream().filter(update -> update.type().startsWith("task.")).noneMatch(update -> update.data().has("report")))
                .as("a user's own client reads the updates too, so they carry no report").isTrue();
    }

    // ================================================================ when things go wrong

    @Test
    void aWorkersTurnMayTakeLongerThanAConversations_becauseNobodyWaitsForIt() {
        userSays(acme, USER, "research flights to Tokyo");
        untilQuiet();

        assertThat(jdbc.sql("""
                SELECT split_part(merge_key, ':', 1) || ' ' || (extract(epoch FROM deadline_at - created_at) / 60)::int
                  FROM turn ORDER BY created_at, run_id
                """).query(String.class).list())
                .as("minutes that each turn had: the conversation's, the worker's, and the main agent's for the report")
                .containsExactly("conv 10", "work 20", "conv 10");
    }

    @Test
    void aWorkerThatEndsItsTurnWithoutAWord_isReportedInItsPlace_soThatTheMainAgentNeverWaitsForNothing() {
        workerScript = context -> context.step() == 0 ? ModelStep.of(custom("read_events", "{}")) : ModelStep.of();
        userSays(acme, USER, "research flights to Tokyo");

        untilQuiet();

        JsonNode told = mainRead.getFirst().path("reports").get(0);
        assertThat(told.path("kind").asString()).isEqualTo("report");
        assertThat(told.path("text").asString()).isEqualTo("The worker ended its turn without a report.");
        assertThat(tasks(acme).getFirst().status()).isEqualTo("idle");
    }

    @Test
    void aWorkersTurnThatFailsIsReportedToTheMainAgent() {
        userSays(acme, USER, "research flights to Tokyo");
        untilQuiet();
        cma.hold(sessionOf("work:" + lastTask));
        userSays(acme, USER, "more and trains too");

        untilQuiet();

        JsonNode failure = mainRead.getLast().path("reports").get(0);
        assertThat(failure.path("from").asString()).isEqualTo("the service");
        assertThat(failure.path("kind").asString()).isEqualTo("failure");
        assertThat(failure.path("text").asString()).startsWith("The worker's turn failed, and what it was doing is not done: "
                + "the turn took longer than PT20M.");
        assertThat(tasks(acme).getFirst().status()).as("the task stays open: the main agent decides what to do").isEqualTo("idle");
        Update failed = acme.get("/v1/users/u42/updates").as(UpdatePage.class).updates().stream()
                .filter(update -> update.type().equals("run.failed")).findFirst().orElseThrow();
        assertThat(failed.data().path("task_id").asString()).isEqualTo(lastTask);
    }

    @Test
    void aNewSessionOfTheMainAgentIsToldWhatWasSaidAndThatTasksAreOpen_alsoWhenItStartsInTheBackground() {
        userSays(acme, USER, "research flights to Tokyo");
        settle();
        acme.put("/v1/agents/default", mainAgent("You look after the user, briefly.", null)).as(AgentProfileView.class);

        untilQuiet();

        assertThat(cma.sessionIds()).as("the first session of the main agent, the worker's, and a new one").hasSize(3);
        String first = cma.userMessages(cma.sessionIds().getLast()).getFirst();
        assertThat(first).startsWith("[From the service that runs you, not from the user: this conversation continues in a new session.")
                .contains("User: research flights to Tokyo\nYou: On it: " + lastTask + "\n[End of the earlier conversation.]",
                        "The service has it on record: your tasks and how they stand. Call read_events to get it before you "
                                + "decide what to do.]",
                        "A worker has something for you on a task you delegated.");
        assertThat(mainRead.getLast().path("before_this_session").path("tasks").get(0).path("task_id").asString())
                .as("with the report that it is woken for, the new session is given its tasks").isEqualTo(lastTask);

        userSays(acme, USER, "list");
        settle();
        JsonNode listed = AgentJson.mapper().readTree(answers.getLast()).path("tasks").get(0);
        assertThat(listed.path("task_id").asString()).isEqualTo(lastTask);
        assertThat(listed.path("status").asString()).isEqualTo("idle");
        assertThat(listed.path("latest").path("text").asString()).isEqualTo("Researched: Research flights to Tokyo");
    }

    // ================================================================ who can be a main agent, and a worker

    @Test
    void aMainAgentHasTheToolsToDelegate_andItsWorkersDoNot() {
        AgentProfileView main = acme.get("/v1/agents/default").as(AgentProfileView.class);
        assertThat(main.coordinator()).as("with what was left out: the usual rule for when a task starts, and the usual limit")
                .isEqualTo(new CoordinatorConfig(
                        List.of(new WorkerConfig("researcher", "research", "Looks things up and reports what it found.", "ask")), 4));
        assertThat(acme.get("/v1/agents/research").as(AgentProfileView.class).coordinator()).isNull();

        userSays(acme, USER, "research flights to Tokyo");
        untilQuiet();
        AgentSpec mainSpec = cma.agentSpec(cma.sessionSpec(sessionOf("conv:default")).agent());
        AgentSpec workerSpec = cma.agentSpec(cma.sessionSpec(sessionOf("work:" + lastTask)).agent());

        assertThat(mainSpec.customTools()).extracting(AgentSpec.CustomTool::name)
                .contains("delegate_task", "message_worker", "list_tasks", "close_task");
        assertThat(mainSpec.customTools().stream().filter(tool -> tool.name().equals("delegate_task")).findFirst().orElseThrow()
                .description()).contains("- researcher: Looks things up and reports what it found.");
        assertThat(mainSpec.system()).contains("You are this user's main agent.");
        assertThat(workerSpec.customTools()).extracting(AgentSpec.CustomTool::name)
                .containsExactly("read_events", "notify_user", "ask_user", "schedule_followup");
        assertThat(workerSpec.system()).doesNotContain("main agent");
    }

    @Test
    void delegationIsOneLevelDeep_andAWorkerHasToExist() {
        WorkerConfig researcher = new WorkerConfig("researcher", "research", "Looks things up.");
        List<CoordinatorConfig> refused = List.of(
                new CoordinatorConfig(List.of(new WorkerConfig("ghost", "nobody", "Does not exist.")), null),
                new CoordinatorConfig(List.of(new WorkerConfig("self", "boss", "Itself.")), null),
                new CoordinatorConfig(List.of(new WorkerConfig("lead", "default", "A main agent.")), null),
                new CoordinatorConfig(List.of(researcher, researcher), null),
                new CoordinatorConfig(List.of(new WorkerConfig("Researcher", "research", "A role in capitals.")), null),
                new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research", " ")), null),
                new CoordinatorConfig(List.of(researcher), 0));
        for (CoordinatorConfig coordinator : refused) {
            TestApi.Reply reply = acme.put("/v1/agents/boss",
                    new AgentProfileRequest("Boss", null, null, "You lead.", null, null, null, null, coordinator));
            assertThat(reply.status()).as("%s", coordinator).isEqualTo(400);
            assertThat(reply.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        }
        TestApi.Reply workerAsMain = acme.put("/v1/agents/research",
                new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null, null, null,
                        new CoordinatorConfig(null, null)));
        assertThat(workerAsMain.status()).isEqualTo(400);
        assertThat(workerAsMain.body().path("detail").asString()).contains("is a worker of the main agent default");
        assertThat(acme.get("/v1/agents/boss").status()).as("nothing was saved").isEqualTo(404);
    }

    @Test
    void theDemoAgentWalksTheWholePath() {
        cma.brain(new DemoAgent());
        userSays(acme, USER, "task: compare the three plans");

        untilQuiet();

        TaskView task = acme.get("/v1/users/u42/tasks?status=all").as(TaskPage.class).tasks().getFirst();
        assertThat(repliesTo(acme, USER)).containsExactly(
                "I have handed that to a worker (" + task.taskId() + "). I will tell you what comes of it.",
                "[demo] Your task \"compare the three plans\" is back from the researcher: [demo] Done. You asked for: "
                        + "compare the three plans");
    }

    // ================================================================ helpers

    /** Lets the main agent, its workers and what they tell each other run until nothing is left to do. */
    private int untilQuiet() {
        int runs = 0;
        for (int settled = settle(); settled > 0; settled = settle()) {
            runs += settled;
        }
        return runs;
    }

    private long interrupts() {
        return cma.calls().stream().filter(call -> call.operation() == Operation.SEND)
                .filter(call -> ((List<?>) call.argument()).stream().anyMatch(Outgoing.Interrupt.class::isInstance)).count();
    }

    private List<TaskView> tasks(TestApi caller) {
        return caller.get("/v1/users/u42/tasks").as(TaskPage.class).tasks();
    }

    private String sessionOf(String line) {
        return cma.sessionIds().stream().filter(session -> line.equals(cma.sessionSpec(session).metadata().get("line")))
                .reduce((first, second) -> second).orElseThrow();
    }

    /** A JSON object of the given names and values, as the input of a tool call. */
    private static String args(String... namesAndValues) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            input.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return input.toString();
    }
}
