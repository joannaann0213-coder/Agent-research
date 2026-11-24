package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.GrantPage;
import dev.agentflow.agent.api.GrantView;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.TaskView;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.UpdatePage;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.agent.server.tasks.Tasks;
import dev.agentflow.agent.server.tools.DemoAgent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * What a main agent begins on its own initiative, and who lets it start. The user does, by
 * confirming the task in the application, by telling the main agent to go ahead, or by a standing
 * authorisation for the role; or the application does, by its rule for the role. What the user asks
 * for in a conversation starts at once.
 */
class TaskConfirmationIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String TASKS = "/v1/users/u42/tasks";
    private static final String GRANT = "/v1/users/u42/grants/default/researcher";

    private TestApi acme;
    private TestApi alice;
    /** What read_events gave the main agent in the background, call by call. */
    private final List<JsonNode> mainRead = new ArrayList<>();
    /** What read_events gave the workers, call by call. */
    private final List<JsonNode> workerRead = new ArrayList<>();
    /** What the tools for tasks answered the main agent, call by call. */
    private final List<String> answers = new ArrayList<>();
    /** The task the main agent delegated last. */
    private String lastTask;

    /** The service's own hand on the tasks, for what its sweep does. */
    @Autowired
    private Tasks taskDesk;

    @BeforeEach
    void aMainAgentWithAResearcher() {
        acme = app(ACME);
        acme.put("/v1/agents/research", new AgentProfileRequest("Researcher", null, null, "You look things up.", null, null))
                .as(AgentProfileView.class);
        mainAgent(null);
        alice = user(acme, USER);
        mainRead.clear();
        workerRead.clear();
        answers.clear();
        lastTask = null;
        cma.brain(context -> context.sessionMetadata().get("line").startsWith("work:") ? researching(context)
                : context.userMessage().contains("The user is not here") ? inTheBackground(context) : inConversation(context));
    }

    /** The main agent, with the rule {@code start} for when a task of its researcher may start; the usual one when {@code null}. */
    private void mainAgent(String start) {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(new WorkerConfig("researcher", "research",
                        "Looks things up and reports what it found.", start)), null))).as(AgentProfileView.class);
    }

    /**
     * The main agent in the background. An event whose data says {@code look_into} makes it hand that
     * to the researcher by itself, or to the {@code role} the data names, with a summary for the user
     * unless the data says {@code bare}; one that says {@code push_through} makes it try to start the
     * last task itself. A report it tells the user about.
     */
    private ModelStep inTheBackground(ModelContext context) {
        if (context.step() == 0) {
            return context.userMessage().contains("Call read_events") ? ModelStep.of(custom("read_events", "{}")) : say("Looked around.");
        }
        ToolOutcome read = context.resultOf("read_events");
        if (read == null) {
            ToolOutcome answer = context.toolResults().isEmpty() ? null : context.toolResults().getFirst();
            if (answer != null && !"notify_user".equals(answer.tool())) {
                heard(answer);
            }
            return say("Done for now.");
        }
        JsonNode waiting = AgentJson.mapper().readTree(read.text());
        mainRead.add(waiting);
        for (JsonNode event : waiting.path("events")) {
            JsonNode data = event.path("data");
            if (data.path("push_through").asBoolean(false)) {
                return ModelStep.of(custom("start_task", args("task_id", lastTask)));
            }
            String what = data.path("look_into").asString(null);
            if (what != null) {
                ObjectNode input = AgentJson.mapper().createObjectNode();
                input.put("role", data.path("role").asString("researcher")).put("title", what).put("instructions", "Research " + what);
                if (!data.path("bare").asBoolean(false)) {
                    input.put("summary", "I can look into " + what + " for you.");
                }
                return ModelStep.of(custom("delegate_task", input.toString()));
            }
        }
        for (JsonNode report : waiting.path("reports")) {
            // That a person confirmed a task is something to know, not something to tell the user.
            if (!"decided".equals(report.path("kind").asString())) {
                return ModelStep.of(custom("notify_user", args("text", "About " + report.path("title").asString() + ": "
                        + report.path("text").asString())));
            }
        }
        return say("Nothing to tell.");
    }

    /**
     * The main agent in a conversation. "research X" hands X to the researcher, "maybe X" does so but
     * asks the user first, "go ahead" starts the last task, "more X" sends its worker a further word,
     * "close" closes it. Anything else it repeats.
     */
    private ModelStep inConversation(ModelContext context) {
        String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
        if (context.step() == 0) {
            if (said.startsWith("research ") || said.startsWith("maybe ")) {
                String what = said.substring(said.indexOf(' ') + 1);
                ObjectNode input = AgentJson.mapper().createObjectNode();
                input.put("role", "researcher").put("title", what).put("instructions", "Research " + what)
                        .put("summary", "I would look into " + what + " for you.");
                if (said.startsWith("maybe ")) {
                    input.put("ask_first", true);
                }
                return ModelStep.of(custom("delegate_task", input.toString()));
            }
            if (said.equals("go ahead")) {
                return ModelStep.of(custom("start_task", args("task_id", lastTask)));
            }
            if (said.startsWith("more ")) {
                return ModelStep.of(custom("message_worker", args("task_id", lastTask, "text", said.substring("more ".length()))));
            }
            if (said.equals("close")) {
                return ModelStep.of(custom("close_task", args("task_id", lastTask)));
            }
            return say("You said: " + said);
        }
        ToolOutcome answer = context.toolResults().getFirst();
        boolean proposed = heard(answer);
        if ("delegate_task".equals(answer.tool()) && !answer.error()) {
            return say((proposed ? "Waiting for you: " : "On it: ") + lastTask);
        }
        return say(answer.error() ? "That did not work." : "Done.");
    }

    /** Notes what a tool for tasks answered, and the task it named. Returns whether that task waits for a confirmation. */
    private boolean heard(ToolOutcome answer) {
        answers.add((answer.error() ? "error: " : "") + answer.text());
        if (!"delegate_task".equals(answer.tool()) || answer.error()) {
            return false;
        }
        JsonNode done = AgentJson.mapper().readTree(answer.text());
        lastTask = done.path("task_id").asString();
        return done.path("status").asString().equals("proposed");
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

    // ================================================================ the user confirms, or declines

    @Test
    void whatTheMainAgentBeginsByItselfWaitsForTheUsersConfirmation_andStartsWhenTheyGiveIt() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");

        assertThat(settle()).as("the main agent's turn in the background").isEqualTo(1);

        TaskView proposed = tasks(alice, "proposed").getFirst();
        assertThat(proposed.taskId()).startsWith("task_").isEqualTo(lastTask);
        assertThat(proposed).extracting(TaskView::status, TaskView::title, TaskView::summary, TaskView::authorizedBy)
                .containsExactly("proposed", "the delayed order", "I can look into the delayed order for you.", null);
        assertThat(proposed.expiresAt()).as("three days to confirm it").isAfter(clock.now().plus(Duration.ofDays(2)))
                .isBefore(clock.now().plus(Duration.ofDays(4)));
        assertThat(answers.getLast()).contains("Not started: the user has to confirm it first");
        assertThat(settle()).as("nothing runs before the user has confirmed").isZero();
        assertThat(cma.sessionIds()).as("the worker has no session yet").hasSize(1);
        assertThat(count("SELECT count(*) FROM inbox_relay WHERE request ->> 'type' = 'agent.task_message.v1'"))
                .as("the worker has not been woken").isZero();
        JsonNode announced = updates("task.created").getFirst().data();
        assertThat(announced.path("status").asString()).isEqualTo("proposed");
        assertThat(announced.path("summary").asString()).as("what the application shows the user to decide on")
                .isEqualTo("I can look into the delayed order for you.");

        TaskView started = alice.post(TASKS + "/" + lastTask + "/start", null).as(TaskView.class);

        assertThat(started).extracting(TaskView::status, TaskView::authorizedBy, TaskView::expiresAt)
                .containsExactly("queued", "user", null);
        assertThat(acme.post(TASKS + "/" + lastTask + "/start", null).as(TaskView.class).authorizedBy())
                .as("confirming it again changes nothing").isEqualTo("user");
        assertThat(count("SELECT count(*) FROM inbox_relay WHERE request ->> 'type' = 'agent.task_message.v1'")).isEqualTo(1);
        untilQuiet();

        assertThat(workerRead).hasSize(1);
        assertThat(workerRead.getFirst().path("messages").get(0).path("text").asString()).isEqualTo("Research the delayed order");
        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text).containsExactly(
                tuple("proposal", "I can look into the delayed order for you."),
                tuple("notification", "About the delayed order: Researched: Research the delayed order"));
        assertThat(jdbc.sql("SELECT merge_key || ' ' || outcome FROM turn ORDER BY created_at, run_id").query(String.class).list())
                .containsExactly("conv:default silent", "work:" + lastTask + " reported", "conv:default notified");
        JsonNode confirmed = updates("task.updated").getFirst().data();
        assertThat(confirmed.path("status").asString()).isEqualTo("queued");
        assertThat(confirmed.path("authorized_by").asString()).isEqualTo("user");
    }

    @Test
    void whatWaitsForTheUserShowsInTheConversation_soThatAChatAloneCanConfirmIt() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();

        assertThat(messagesOf(alice, USER)).extracting(MessageView::role, MessageView::kind, MessageView::text, MessageView::taskId)
                .as("the main agent's summary, as a message that names the task")
                .containsExactly(tuple("agent", "proposal", "I can look into the delayed order for you.", lastTask));
        assertThat(updates(Update.MESSAGE_CREATED)).extracting(update -> update.data().path("task_id").asString())
                .containsExactly(lastTask);
        assertThat(answers.getLast()).contains("put before them as a message in the conversation");

        userSays(alice, USER, "go ahead");
        untilQuiet();

        assertThat(acme.get(TASKS + "/" + lastTask).as(TaskView.class)).extracting(TaskView::status, TaskView::authorizedBy)
                .containsExactly("idle", "conversation");
        assertThat(messagesOf(alice, USER)).extracting(MessageView::kind)
                .containsExactly("proposal", "message", "reply", "notification");
    }

    @Test
    void aTaskThatTheUserDeclinesNeverStarts_andTheMainAgentIsTold() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();

        TaskView declined = alice.post(TASKS + "/" + lastTask + "/close", null).as(TaskView.class);

        assertThat(declined).extracting(TaskView::status, TaskView::closedBy, TaskView::authorizedBy)
                .containsExactly("closed", "user", null);
        untilQuiet();
        assertThat(workerRead).isEmpty();
        assertThat(cma.sessionIds()).as("the worker never got a session").hasSize(1);
        JsonNode told = mainRead.getLast().path("reports").get(0);
        assertThat(told.path("kind").asString()).isEqualTo("closed");
        assertThat(told.path("text").asString()).isEqualTo("The user declined this task, which had not started.");
        TestApi.Reply tooLate = alice.post(TASKS + "/" + lastTask + "/start", null);
        assertThat(tooLate.status()).isEqualTo(409);
        assertThat(tooLate.code()).isEqualTo(ProblemCodes.CONFLICT);
        assertThat(acme.post(TASKS + "/task_none/start", null).status()).isEqualTo(404);
    }

    @Test
    void whenTheUserSaysGoAheadInTheConversation_theMainAgentStartsWhatItProposed() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();

        userSays(alice, USER, "go ahead");
        untilQuiet();

        assertThat(answers).contains("Started. The worker's report wakes you.");
        assertThat(tasks(acme, "all").getFirst()).extracting(TaskView::status, TaskView::authorizedBy)
                .containsExactly("idle", "conversation");
        assertThat(workerRead).hasSize(1);
    }

    @Test
    void theMainAgentCannotConfirmItsOwnProposal() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();

        somethingHappens("{\"push_through\": true}");
        untilQuiet();

        assertThat(answers.getLast()).startsWith("error: Not started: only the user can confirm a task");
        assertThat(tasks(acme, "proposed")).hasSize(1);
        assertThat(workerRead).isEmpty();
    }

    // ================================================================ a standing authorisation

    @Test
    void withTheUsersStandingAuthorisationTheTaskStartsAtOnce_untilTheUserTakesItBack() {
        GrantView granted = alice.put(GRANT, null).as(GrantView.class);

        assertThat(granted).extracting(GrantView::agent, GrantView::role, GrantView::grantedBy)
                .containsExactly("default", "researcher", "user");
        assertThat(acme.put(GRANT, null).as(GrantView.class).grantedBy()).as("giving it again changes nothing").isEqualTo("user");
        assertThat(acme.get("/v1/users/u42/grants").as(GrantPage.class).grants()).hasSize(1);

        somethingHappens("{\"look_into\": \"the delayed order\"}");
        untilQuiet();

        assertThat(tasks(acme, "all").getFirst()).extracting(TaskView::status, TaskView::authorizedBy)
                .containsExactly("idle", "grant");
        assertThat(answers.getFirst()).contains("The user has allowed you to start tasks of this role without asking.");
        assertThat(workerRead).hasSize(1);

        assertThat(alice.delete(GRANT).status()).isEqualTo(204);
        assertThat(alice.delete(GRANT).status()).as("taking it back again changes nothing").isEqualTo(204);
        assertThat(alice.get("/v1/users/u42/grants").as(GrantPage.class).grants()).isEmpty();
        somethingHappens("{\"look_into\": \"the late parcel\"}");
        untilQuiet();

        assertThat(tasks(acme, "proposed")).extracting(TaskView::title).containsExactly("the late parcel");
        assertThat(workerRead).as("what had started went on, and nothing new started").hasSize(1);
    }

    @Test
    void withAStandingAuthorisationOnlySoMuchStartsByItselfInADay() {
        alice.put(GRANT, null).as(GrantView.class);
        for (int i = 1; i <= 10; i++) {
            somethingHappens("{\"look_into\": \"order " + i + "\"}");
            untilQuiet();
        }
        assertThat(tasks(acme, "all")).extracting(TaskView::authorizedBy).as("ten in a day start unasked")
                .hasSize(10).containsOnly("grant");

        somethingHappens("{\"look_into\": \"order 11\"}");
        untilQuiet();

        TaskView eleventh = tasks(acme, "proposed").getFirst();
        assertThat(eleventh).extracting(TaskView::title, TaskView::status, TaskView::authorizedBy)
                .as("the eleventh waits for the user, grant or not").containsExactly("order 11", "proposed", null);
        assertThat(answers.getLast()).contains("You have started 10 tasks without asking the user in the last 24 hours");
        assertThat(alice.post(TASKS + "/" + eleventh.taskId() + "/start", null).as(TaskView.class).authorizedBy())
                .as("and starts when they confirm it").isEqualTo("user");
        untilQuiet();

        clock.advance(Duration.ofHours(24));
        somethingHappens("{\"look_into\": \"order 12\"}");
        untilQuiet();
        assertThat(tasks(acme, "all").getFirst()).extracting(TaskView::title, TaskView::authorizedBy)
                .as("a day later it starts by itself again").containsExactly("order 12", "grant");
    }

    @Test
    void anAuthorisationIsTheUsersOwn_andNamesARoleOfAMainAgent() {
        TestApi bob = user(acme, "u43");

        assertThat(acme.put("/v1/users/u42/grants/default/nobody", null).status()).as("no such role").isEqualTo(404);
        assertThat(acme.put("/v1/users/u42/grants/research/researcher", null).status()).as("not a main agent").isEqualTo(404);
        assertThat(acme.put("/v1/users/u42/grants/ghost/researcher", null).status()).as("no such agent").isEqualTo(404);
        assertThat(bob.put(GRANT, null).status()).as("another user's").isEqualTo(403);
        assertThat(bob.get("/v1/users/u42/grants").status()).isEqualTo(403);

        bob.put("/v1/users/u43/grants/default/researcher", null).as(GrantView.class);

        assertThat(acme.get("/v1/users/u42/grants").as(GrantPage.class).grants()).as("each user gives their own").isEmpty();
        assertThat(acme.get("/v1/users/u43/grants").as(GrantPage.class).grants()).extracting(GrantView::grantedBy)
                .containsExactly("user");
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        untilQuiet();
        assertThat(tasks(acme, "proposed")).as("u42 has not allowed anything").hasSize(1);
    }

    // ================================================================ the application's rule for a role

    @Test
    void theApplicationLetsARoleStartUnasked() {
        mainAgent("always_allow");

        somethingHappens("{\"look_into\": \"the delayed order\"}");
        untilQuiet();

        assertThat(tasks(acme, "all").getFirst()).extracting(TaskView::status, TaskView::authorizedBy)
                .containsExactly("idle", "policy");
        assertThat(acme.get("/v1/agents/default").as(AgentProfileView.class).coordinator().workers().getFirst().start())
                .isEqualTo("always_allow");
    }

    @Test
    void theApplicationHasEveryTaskOfARoleConfirmed_alsoWhatTheUserAsksFor_andAllowsNoStandingAuthorisation() {
        mainAgent("always_ask");

        userSays(alice, USER, "research flights to Tokyo");
        untilQuiet();

        assertThat(tasks(alice, "proposed")).extracting(TaskView::title).containsExactly("flights to Tokyo");
        assertThat(repliesTo(acme, USER)).as("what is proposed, and the main agent's answer to the user")
                .containsExactly("I would look into flights to Tokyo for you.", "Waiting for you: " + lastTask);
        TestApi.Reply refused = alice.put(GRANT, null);
        assertThat(refused.status()).isEqualTo(400);
        assertThat(refused.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        assertThat(refused.body().path("detail").asString()).contains("allows no standing authorisation");

        userSays(alice, USER, "go ahead");
        untilQuiet();

        assertThat(answers.getLast()).startsWith("error: Not started: tasks of the role researcher are confirmed by the user in the "
                + "application");
        assertThat(alice.post(TASKS + "/" + lastTask + "/start", null).as(TaskView.class).authorizedBy()).isEqualTo("user");
    }

    @Test
    void oneAgentCanStandBehindSeveralRoles_eachWithARuleOfItsOwn() {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You look after the user.", null, null,
                null, null, new CoordinatorConfig(List.of(
                        new WorkerConfig("researcher", "research", "Looks things up and reports what it found.", null),
                        new WorkerConfig("digest", "research", "Sums up what happened, for the user to read later.", "always_allow")),
                        null))).as(AgentProfileView.class);

        somethingHappens("{\"look_into\": \"the week\", \"role\": \"digest\"}");
        untilQuiet();
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        untilQuiet();

        assertThat(tasks(acme, "all")).extracting(TaskView::role, TaskView::agent, TaskView::status, TaskView::authorizedBy)
                .containsExactly(tuple("researcher", "research", "proposed", null), tuple("digest", "research", "idle", "policy"));
    }

    @Test
    void whatTheUserAsksForStartsAtOnce_andTheMainAgentMayStillAskFirst() {
        userSays(alice, USER, "research flights to Tokyo");
        assertThat(settle()).isEqualTo(1);

        assertThat(tasks(acme, "open").getFirst()).extracting(TaskView::status, TaskView::authorizedBy)
                .containsExactly("queued", "conversation");
        untilQuiet();

        userSays(alice, USER, "maybe a trip to Lisbon");
        untilQuiet();

        TaskView waiting = tasks(alice, "proposed").getFirst();
        assertThat(waiting).extracting(TaskView::title, TaskView::summary)
                .containsExactly("a trip to Lisbon", "I would look into a trip to Lisbon for you.");
        assertThat(repliesTo(acme, USER)).endsWith("Waiting for you: " + lastTask);

        userSays(alice, USER, "more only in May");
        untilQuiet();
        assertThat(answers.getLast()).as("what the user confirms is what was proposed").startsWith("error: Not sent: the task has not started");

        userSays(alice, USER, "close");
        untilQuiet();
        assertThat(acme.get(TASKS + "/" + lastTask).as(TaskView.class)).extracting(TaskView::status, TaskView::closedBy)
                .containsExactly("closed", "agent");
    }

    // ================================================================ within limits

    @Test
    void aTaskThatNobodyConfirmsInTimeCanNoLongerBeStarted() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();
        clock.advance(Duration.ofDays(3));
        // The user's token ran out long ago; their client has a new one by now.
        alice = user(acme, USER);

        TestApi.Reply late = alice.post(TASKS + "/" + lastTask + "/start", null);

        assertThat(late.status()).isEqualTo(409);
        assertThat(late.body().path("detail").asString()).contains("waited too long for a confirmation");
        assertThat(acme.get(TASKS + "/" + lastTask).as(TaskView.class))
                .extracting(TaskView::status, TaskView::closedBy, TaskView::closedReason)
                .containsExactly("closed", "service", "it was not confirmed in time");
        assertThat(settle()).as("the main agent is not woken for it").isZero();
    }

    @Test
    void theSweepClosesWhatNobodyConfirmedInTime_soThatWhoeverShowsItLearnsOfIt() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();
        clock.advance(Duration.ofDays(3).minusSeconds(60));
        assertThat(taskDesk.closeUnconfirmed(10)).as("it can still be confirmed").isZero();
        clock.advance(Duration.ofSeconds(60));

        assertThat(taskDesk.closeUnconfirmed(10)).isEqualTo(1);

        assertThat(tasks(acme, "proposed")).isEmpty();
        assertThat(acme.get(TASKS + "/" + lastTask).as(TaskView.class))
                .extracting(TaskView::status, TaskView::closedBy, TaskView::closedReason)
                .containsExactly("closed", "service", "it was not confirmed in time");
        assertThat(updates(Update.TASK_UPDATED)).extracting(update -> update.data().path("status").asString())
                .as("whoever shows the task learns that it is over").containsExactly("closed");
        assertThat(settle()).as("the main agent is not woken for it").isZero();
        assertThat(taskDesk.closeUnconfirmed(10)).as("it is closed once").isZero();
    }

    @Test
    void theMainAgentHearsOfATaskThatTheServiceClosed_whenSomethingElseWakesIt() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();
        clock.advance(Duration.ofDays(3));
        taskDesk.closeUnconfirmed(10);

        somethingHappens("{\"carrier\": \"a new one\"}");
        settle();

        JsonNode told = mainRead.getLast().path("reports");
        assertThat(told.size()).isEqualTo(1);
        assertThat(told.path(0).path("from").asString()).isEqualTo("the service");
        assertThat(told.path(0).path("kind").asString()).isEqualTo("closed");
        assertThat(told.path(0).path("title").asString()).isEqualTo("the delayed order");
        assertThat(told.path(0).path("text").asString()).isEqualTo("The service closed this task: it was not confirmed in time");

        somethingHappens("{\"carrier\": \"yet another\"}");
        settle();
        assertThat(mainRead.getLast().path("reports").size()).as("it is told once").isZero();
    }

    @Test
    void whatWasNotConfirmedInTimeMakesRoomForWhatTheMainAgentProposesNext() {
        somethingHappens("{\"look_into\": \"the delayed order\"}");
        settle();
        clock.advance(Duration.ofDays(3));

        somethingHappens("{\"look_into\": \"the late parcel\"}");
        settle();

        assertThat(tasks(acme, "all")).extracting(TaskView::title, TaskView::status)
                .containsExactly(tuple("the late parcel", "proposed"), tuple("the delayed order", "closed"));
    }

    @Test
    void aTaskToConfirmNeedsWordsForTheUser_andOnlySoManyWaitAtATime() {
        somethingHappens("{\"look_into\": \"something\", \"bare\": true}");
        settle();

        assertThat(answers.getLast()).startsWith("error: Not delegated: the user has to confirm this task before it starts");
        assertThat(tasks(acme, "all")).isEmpty();

        for (int i = 1; i <= 5; i++) {
            somethingHappens("{\"look_into\": \"thing " + i + "\"}");
            settle();
        }
        somethingHappens("{\"look_into\": \"thing 6\"}");
        settle();

        assertThat(answers.getLast()).startsWith("error: Not delegated: 5 tasks already wait for the user's confirmation");
        assertThat(tasks(acme, "proposed")).hasSize(5);
        assertThat(tasks(acme, "open")).hasSize(5);
    }

    // ================================================================ the demo agent

    @Test
    void theDemoAgentTakesUpWorkByItself_andWaitsForTheUser() {
        cma.brain(new DemoAgent());
        somethingHappens("{\"task\": \"find a new delivery slot\"}");

        untilQuiet();

        TaskView waiting = tasks(alice, "proposed").getFirst();
        assertThat(waiting.summary()).isEqualTo("[demo] I can do this for you: find a new delivery slot");
        assertThat(repliesTo(acme, USER)).as("the user is shown what is proposed, and nothing else")
                .containsExactly("[demo] I can do this for you: find a new delivery slot");

        alice.post(TASKS + "/" + waiting.taskId() + "/start", null).as(TaskView.class);
        untilQuiet();

        assertThat(repliesTo(acme, USER)).containsExactly("[demo] I can do this for you: find a new delivery slot",
                "[demo] Your task \"find a new delivery slot\" is back from the "
                        + "researcher: [demo] Done. You asked for: find a new delivery slot");
    }

    // ================================================================ helpers

    /** Something happens in the application's systems that concerns the user, with these details. */
    private void somethingHappens(String data) {
        acme.post("/v1/users/u42/events", new PostEventRequest(null, "order.delayed", "order-1234", null, null,
                AgentJson.mapper().readTree(data), null)).as(EventAccepted.class);
    }

    /** Lets the main agent, its workers and what they tell each other run until nothing is left to do. */
    private int untilQuiet() {
        int runs = 0;
        for (int settled = settle(); settled > 0; settled = settle()) {
            runs += settled;
        }
        return runs;
    }

    private List<TaskView> tasks(TestApi caller, String which) {
        return caller.get(TASKS + "?status=" + which).as(TaskPage.class).tasks();
    }

    /** The user's updates of one type, oldest first. */
    private List<Update> updates(String type) {
        return acme.get("/v1/users/u42/updates").as(UpdatePage.class).updates().stream()
                .filter(update -> update.type().equals(type)).toList();
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
