package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.CreateScheduleRequest;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.SchedulePage;
import dev.agentflow.agent.api.ScheduleView;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.bridge.TurnHooks.Point;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import dev.agentflow.agent.server.support.SimulatedCrash;
import dev.agentflow.agent.server.support.TestApi;
import dev.agentflow.inbox.server.timer.TimerScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The agent at a given time: schedules the application sets up, once or on a cron rule in the
 * user's time zone, and follow-ups the agent sets itself. The Inbox keeps the time; the agent works
 * in the background when it comes, and the user hears of it only if the agent tells them.
 */
class SchedulesIT extends AgentTestBase {

    private static final String USER = "u42";
    private static final String INSTRUCTIONS = "Remind the user that invoice 77 is due today.";

    private TestApi acme;
    /** What the agent was given in the background, turn by turn. */
    private final List<String> woken = new ArrayList<>();

    @BeforeEach
    void anAgentThatDoesWhatItIsScheduledFor() {
        acme = app(ACME);
        defaultAgent(acme);
        woken.clear();
        cma.brain(this::assistant);
    }

    /**
     * In a conversation: sets a follow-up when asked to remind, and otherwise repeats. In the
     * background: does what a schedule's instructions say, or what its own note says.
     */
    private ModelStep assistant(ModelContext context) {
        String said = context.userMessage();
        if (said.startsWith("[From the service")) {
            if (context.step() == 0) {
                woken.add(said);
                if (said.contains("Call read_events")) {
                    return ModelStep.of(custom("read_events", "{}"));
                }
                String instructions = said.substring(said.indexOf("instructions for it: ") + "instructions for it: ".length(),
                        said.indexOf("\nIf the user needs"));
                return ModelStep.of(custom("notify_user", text("Reminder: " + instructions)));
            }
            ToolOutcome read = context.resultOf("read_events");
            if (read != null) {
                JsonNode notes = AgentJson.mapper().readTree(read.text()).path("followups");
                return ModelStep.of(custom("notify_user", text("As promised: " + notes.get(0).path("note").asString())));
            }
            return say("Done.");
        }
        if (said.contains("remind me")) {
            return context.step() == 0 ? ModelStep.of(custom("schedule_followup", "{\"in_minutes\": 60, \"note\": \"stretch\"}"))
                    : say("I will remind you: " + context.resultOf("schedule_followup").text());
        }
        return say("You said: " + said);
    }

    // ================================================================ the application's schedules

    @Test
    void aScheduleRunsOnceAtItsTime_inTheBackground() {
        Instant at = clock.now().plus(Duration.ofHours(1));
        TestApi.Reply reply = acme.post("/v1/users/u42/schedules", new CreateScheduleRequest("invoice-77", "Invoice reminder",
                INSTRUCTIONS, at, null, null, null, null, null));

        assertThat(reply.status()).isEqualTo(201);
        ScheduleView created = reply.as(ScheduleView.class);
        assertThat(created).isEqualTo(new ScheduleView("invoice-77", "app", "once", "Invoice reminder", INSTRUCTIONS, null, at, null,
                null, "schedule-invoice-77", "default", "default", "active", null, clock.now()));
        assertThat(list(acme)).containsExactly(created);
        assertThat(settle()).as("not yet").isZero();

        clock.advance(Duration.ofHours(1));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isEqualTo(1);
        assertThat(settle()).isEqualTo(1);

        assertThat(woken).containsExactly("[From the service that runs you, not from the user. The user is not here and sees "
                + "nothing of what you write in this turn.]\nIt is time for \"Invoice reminder\", which the application scheduled. "
                + "The application's instructions for it: " + INSTRUCTIONS + "\nIf the user needs to know or to do something, tell "
                + "them with notify_user. If not, end the turn without it.");
        assertThat(messagesOf(acme, USER)).extracting(MessageView::kind, MessageView::text)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("notification", "Reminder: " + INSTRUCTIONS));
        assertThat(list(acme)).as("it ran, and is done").isEmpty();
        assertThat(jdbc.sql("SELECT state FROM schedule").query(String.class).single()).isEqualTo("done");
    }

    @Test
    void aRepeatingScheduleRunsAtItsTimeInTheUsersZone_withAnAgentThatRemembers_untilItIsCancelled() {
        ScheduleView created = acme.post("/v1/users/u42/schedules", new CreateScheduleRequest("brief", "Morning brief", INSTRUCTIONS,
                null, "0 8 * * *", "Asia/Shanghai", "mornings", null, null)).as(ScheduleView.class);
        assertThat(created.cron()).isEqualTo("0 8 * * *");
        assertThat(created.timeZone()).isEqualTo("Asia/Shanghai");
        assertThat(created.subject()).isEqualTo("mornings");

        // 10:00 UTC is 18:00 in Shanghai. 8:00 in Shanghai is 0:00 UTC.
        clock.advance(Duration.ofHours(14).minusMinutes(1));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isZero();
        clock.advance(Duration.ofMinutes(1));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isEqualTo(1);
        assertThat(settle()).isEqualTo(1);
        clock.advance(Duration.ofDays(1));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isEqualTo(1);
        assertThat(settle()).isEqualTo(1);

        assertThat(repliesTo(acme, USER)).hasSize(2);
        assertThat(cma.sessionIds()).as("one line of work, one agent that remembers").hasSize(1);
        assertThat(list(acme)).singleElement().extracting(ScheduleView::state, ScheduleView::lastFiredAt)
                .containsExactly("active", clock.now());

        assertThat(acme.delete("/v1/users/u42/schedules/brief").status()).isEqualTo(204);
        assertThat(list(acme)).isEmpty();
        clock.advance(Duration.ofDays(1));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isZero();
        assertThat(settle()).isZero();
        assertThat(acme.delete("/v1/users/u42/schedules/brief").status()).as("cancelling again changes nothing").isEqualTo(204);
    }

    @Test
    void settingUpTheSameScheduleAgainIsHarmless_andAnIdIsNotReusedForAnotherOne() {
        CreateScheduleRequest request = new CreateScheduleRequest("s-1", "Reminder", INSTRUCTIONS, clock.now().plus(Duration.ofHours(1)),
                null, null, null, null, null);

        ScheduleView first = acme.post("/v1/users/u42/schedules", request).as(ScheduleView.class);
        TestApi.Reply again = acme.post("/v1/users/u42/schedules", request);

        assertThat(again.status()).isEqualTo(200);
        assertThat(again.as(ScheduleView.class)).isEqualTo(first);
        TestApi.Reply other = acme.post("/v1/users/u42/schedules", new CreateScheduleRequest("s-1", "Reminder", "Something else.",
                clock.now().plus(Duration.ofHours(1)), null, null, null, null, null));
        assertThat(other.status()).isEqualTo(409);
        assertThat(other.code()).isEqualTo(ProblemCodes.CONFLICT);
        assertThat(EmbeddedInbox.jdbc().sql("SELECT count(*) FROM timer").query(Long.class).single()).as("one timer").isEqualTo(1);
    }

    @Test
    void aScheduleMustSayWhatAndWhen_andTheSchedulerHasTheLastWordOnTheRule() {
        Instant later = clock.now().plus(Duration.ofHours(1));
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, later, "0 8 * * *", null, null, null, null))).isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, null, null, null, null, null, null))).isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, clock.now().minusSeconds(1), null, null, null, null, null)))
                .isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, clock.now().plus(Duration.ofDays(400)), null, null, null,
                null, null))).isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, later, null, "Asia/Shanghai", null, null, null)))
                .as("a zone goes with a rule").isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, null, "0 8 * * *", "Mars/Olympus", null, null, null)))
                .isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, " ", INSTRUCTIONS, later, null, null, null, null, null))).isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", null, later, null, null, null, null, null))).isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, later, null, null, "bad subject", null, null))).isEqualTo(400);
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, later, null, null, null, "nobody", null))).isEqualTo(400);
        assertThat(acme.post("/v1/users/u42/schedules", "{\"name\": \"x\", \"instructions\": \"y\", \"cron\": \"0 8 * * *\", \"often\": true}")
                .status()).isEqualTo(400);

        TestApi.Reply notARule = acme.post("/v1/users/u42/schedules", new CreateScheduleRequest(null, "x", INSTRUCTIONS, null,
                "every morning", null, null, null, null));
        assertThat(notARule.status()).isEqualTo(400);
        assertThat(notARule.body().path("detail").asString()).startsWith("the schedule was refused: ");
        assertThat(create(new CreateScheduleRequest(null, "x", INSTRUCTIONS, null, "* * * * * *", null, null, null, null)))
                .as("more often than once a minute").isEqualTo(400);

        assertThat(count("SELECT count(*) FROM schedule")).as("nothing refused is kept").isZero();
        assertThat(EmbeddedInbox.jdbc().sql("SELECT count(*) FROM timer").query(Long.class).single()).isZero();
    }

    @Test
    void onlyTheApplicationSetsSchedulesUp_butUsersSeeTheirsAndCanCancelThem() {
        TestApi alice = user(acme, "alice");
        TestApi bob = user(acme, "bob");
        acme.post("/v1/users/alice/schedules", new CreateScheduleRequest("weekly", "Weekly", INSTRUCTIONS, null, "0 9 * * 1",
                "Europe/Berlin", null, null, null)).as(ScheduleView.class);

        TestApi.Reply byUser = alice.post("/v1/users/alice/schedules", new CreateScheduleRequest(null, "Mine", "Do as I say.",
                clock.now().plus(Duration.ofHours(1)), null, null, null, null, null));
        assertThat(byUser.status()).isEqualTo(403);
        assertThat(alice.get("/v1/users/alice/schedules").as(SchedulePage.class).schedules()).extracting(ScheduleView::scheduleId)
                .containsExactly("weekly");
        assertThat(bob.get("/v1/users/alice/schedules").status()).isEqualTo(403);
        assertThat(bob.delete("/v1/users/alice/schedules/weekly").status()).isEqualTo(403);
        assertThat(alice.delete("/v1/users/alice/schedules/nothing").status()).isEqualTo(404);

        assertThat(alice.delete("/v1/users/alice/schedules/weekly").status()).isEqualTo(204);
        assertThat(alice.get("/v1/users/alice/schedules").as(SchedulePage.class).schedules()).isEmpty();
        assertThat(EmbeddedInbox.jdbc().sql("SELECT state FROM timer").query(String.class).single()).isEqualTo("cancelled");
    }

    // ================================================================ the agent's follow-ups

    @Test
    void theAgentSetsAFollowUpForItself_andComesBackToTheConversationWhenItIsDue() {
        userSays(acme, USER, "remind me in an hour to stretch");
        settle();

        Instant due = clock.now().plus(Duration.ofMinutes(60));
        assertThat(repliesTo(acme, USER)).containsExactly("I will remind you: Scheduled for " + due + ".");
        ScheduleView followup = list(acme).getFirst();
        assertThat(followup.createdBy()).isEqualTo("agent");
        assertThat(followup.note()).isEqualTo("stretch");
        assertThat(followup.at()).isEqualTo(due);
        assertThat(followup.subject()).as("it runs in the conversation").isNull();
        assertThat(followup.conversationId()).isEqualTo("default");

        clock.advance(Duration.ofMinutes(60));
        EmbeddedInbox.bean(TimerScheduler.class).runOnce();
        assertThat(settle()).isEqualTo(1);

        assertThat(woken).containsExactly("[From the service that runs you, not from the user. The user is not here and sees "
                + "nothing of what you write in this turn.]\nA follow-up you set for yourself is due. Call read_events to see your "
                + "note, then decide what to do.\nIf the user needs to know or to do something, tell them with notify_user. If not, "
                + "end the turn without it.");
        assertThat(messagesOf(acme, USER).getLast()).extracting(MessageView::kind, MessageView::text)
                .containsExactly("notification", "As promised: stretch");
        assertThat(cma.sessionIds()).as("the conversation's own session, which knows what was promised").hasSize(1);
        assertThat(list(acme)).isEmpty();
    }

    @Test
    void followUpsInARowStopAtTheLimit() {
        List<String> answers = new ArrayList<>();
        cma.brain(context -> {
            ToolOutcome answer = context.resultOf("schedule_followup");
            if (answer != null) {
                answers.add((answer.error() ? "error: " : "") + answer.text());
                return say("Done.");
            }
            return ModelStep.of(custom("schedule_followup", "{\"in_minutes\": 1, \"note\": \"look again\"}"));
        });
        acme.post("/v1/users/u42/events", new PostEventRequest(null, "build.started", "build-9", null, null, null, null))
                .as(EventAccepted.class);
        settle();

        for (int round = 1; round <= 6; round++) {
            clock.advance(Duration.ofMinutes(1));
            EmbeddedInbox.bean(TimerScheduler.class).runOnce();
            settle();
        }

        assertThat(answers).hasSize(6);
        assertThat(answers.subList(0, 5)).allMatch(answer -> answer.startsWith("Scheduled for "));
        assertThat(answers.getLast()).isEqualTo("error: Not scheduled: this would be follow-up number 6 in a row, and 5 is the "
                + "limit. Tell the user what there is to tell, or end the turn.");
        assertThat(jdbc.sql("SELECT chain FROM schedule ORDER BY chain").query(Integer.class).list()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void aFollowUpIsSetOnce_whenTheWorkerDiesRightAfter() {
        userSays(acme, USER, "remind me in an hour to stretch");
        deliver();
        hooks.crashAt(Point.TOOLS_RUN);

        assertThatThrownBy(() -> worker.runOnce()).isInstanceOf(SimulatedCrash.class);
        leaseRunsOut();
        assertThat(worker.runOnce()).isTrue();

        assertThat(count("SELECT count(*) FROM schedule")).isEqualTo(1);
        assertThat(EmbeddedInbox.jdbc().sql("SELECT count(*) FROM timer").query(Long.class).single()).isEqualTo(1);
        assertThat(repliesTo(acme, USER)).singleElement().asString().startsWith("I will remind you: Scheduled for");
    }

    @Test
    void aFollowUpThatWasCancelled_doesNotWakeTheAgent_evenWhenItsTimerStillFires() {
        userSays(acme, USER, "remind me in an hour to stretch");
        settle();
        // Cancelled here, but the Inbox was not told: its timer fires all the same.
        jdbc.sql("UPDATE schedule SET state = 'cancelled'").update();
        long sessionsBefore = cma.sessionIds().size();

        clock.advance(Duration.ofMinutes(60));
        assertThat(EmbeddedInbox.bean(TimerScheduler.class).runOnce()).isEqualTo(1);
        assertThat(settle()).as("the run is taken note of, nothing more").isEqualTo(1);

        assertThat(woken).isEmpty();
        assertThat(cma.userMessages(cma.sessionIds().getFirst())).hasSize(1);
        assertThat(cma.sessionIds()).hasSize((int) sessionsBefore);
        assertThat(messagesOf(acme, USER)).hasSize(2);
    }

    @Test
    void aFollowUpMustSayWhatAndWhen() {
        List<String> answers = new ArrayList<>();
        List<String> inputs = List.of("{\"in_minutes\": 5}", "{\"note\": \"x\"}", "{\"note\": \"x\", \"in_minutes\": 5, \"at\": \"2026-11-21T10:00:00Z\"}",
                "{\"note\": \"x\", \"in_minutes\": 0}", "{\"note\": \"x\", \"at\": \"tomorrow\"}", "{\"note\": \"x\", \"at\": \"2026-11-20T10:00:30Z\"}",
                "{\"note\": \"x\", \"at\": \"2027-01-01T00:00:00Z\"}", "{\"note\": \"x\", \"at\": \"2026-11-21T18:00:00+08:00\"}");
        cma.brain(context -> {
            ToolOutcome answer = context.resultOf("schedule_followup");
            if (answer != null) {
                answers.add((answer.error() ? "error: " : "") + answer.text());
            }
            return context.step() < inputs.size() ? ModelStep.of(custom("schedule_followup", inputs.get(context.step()))) : say("Done.");
        });

        userSays(acme, USER, "plan something");
        settle();

        assertThat(answers).containsExactly(
                "error: Not scheduled: note is required, at most 1000 characters: what you are to do then.",
                "error: Not scheduled: give either in_minutes or at, exactly one of them.",
                "error: Not scheduled: give either in_minutes or at, exactly one of them.",
                "error: Not scheduled: in_minutes is a whole number of minutes, at least 1.",
                "error: Not scheduled: at is a moment in ISO-8601, for example 2026-11-23T09:00:00Z.",
                "error: Not scheduled: at is at least a minute from now (it is 2026-11-20T10:00:02Z).",
                "error: Not scheduled: a follow-up is at most 30 days ahead.",
                "Scheduled for 2026-11-21T10:00:00Z.");
    }

    @Test
    void onlySoManyFollowUpsWaitOnALine() {
        List<String> answers = new ArrayList<>();
        cma.brain(context -> {
            ToolOutcome answer = context.resultOf("schedule_followup");
            if (answer != null) {
                answers.add((answer.error() ? "error: " : "") + answer.text());
            }
            return context.step() < 6 ? ModelStep.of(custom("schedule_followup", "{\"in_minutes\": " + (10 + context.step())
                    + ", \"note\": \"check " + context.step() + "\"}")) : say("Done.");
        });

        userSays(acme, USER, "keep an eye on things");
        settle();

        assertThat(answers).hasSize(6);
        assertThat(answers.getLast()).isEqualTo("error: Not scheduled: 5 follow-ups already wait here, which is the limit.");
        assertThat(list(acme)).hasSize(5);
    }

    @Test
    void everyAgentCanSetFollowUps() {
        assertThat(cma.agentSpec(cma.agents().getFirst()).customTools()).extracting(tool -> tool.name()).contains("schedule_followup");
        assertThat(cma.agentSpec(cma.agents().getFirst()).system()).contains("schedule_followup");
    }

    // ================================================================ helpers

    private List<ScheduleView> list(TestApi caller) {
        return caller.get("/v1/users/" + USER + "/schedules").as(SchedulePage.class).schedules();
    }

    private int create(CreateScheduleRequest request) {
        return acme.post("/v1/users/" + USER + "/schedules", request).status();
    }

    private static String text(String text) {
        ObjectNode input = AgentJson.mapper().createObjectNode();
        input.put("text", text);
        return input.toString();
    }
}
