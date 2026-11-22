package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.ActionPage;
import dev.agentflow.agent.api.AgentJson;
import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.ResolveActionRequest;
import dev.agentflow.agent.api.StreamTicket;
import dev.agentflow.agent.api.Update;
import dev.agentflow.agent.api.UpdatePage;
import dev.agentflow.agent.api.WebhookRequest;
import dev.agentflow.agent.api.WebhookSignature;
import dev.agentflow.agent.api.WebhookView;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.delivery.WebhookDispatcher;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.HookReceiver;
import dev.agentflow.agent.server.support.SseReader;
import dev.agentflow.agent.server.support.TestApi;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * What happened for a user reaches the application: in a log that can be read page by page, as a
 * live stream that picks up where it left off, and at the application's webhook, signed, tried
 * again when it fails, and never at an address behind the service.
 */
class DeliveryIT extends AgentTestBase {

    private static final String USER = "u42";

    @Autowired
    private WebhookDispatcher dispatcher;

    @Autowired
    private AgentProperties properties;

    private TestApi acme;
    private TestApi alice;

    @BeforeEach
    void aConversation() {
        acme = app(ACME);
        defaultAgent(acme);
        alice = user(acme, USER);
        cma.brain(context -> say("You said: " + context.userMessage()));
    }

    @AfterEach
    void closeTheServicesNetworkAgain() {
        properties.getWebhooks().setAllowPrivateNetworks(false);
        properties.getWebhooks().setAllowPlainHttp(false);
    }

    // ================================================================ the log

    @Test
    void everythingThatHappensForAUserIsInItsLog_inOrder() {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null,
                List.of(new McpServerConfig("tickets", "https://mcp.acme.example/tickets", "always_ask", "user")))).as(AgentProfileView.class);
        cma.brain(context -> context.userMessage().startsWith("close") && context.step() == 0
                ? ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}")) : say("Done: " + context.userMessage()));
        userSays(alice, USER, "close ticket T-1");
        settle();
        String action = alice.get("/v1/users/u42/actions").as(ActionPage.class).actions().getFirst().actionId();
        alice.post("/v1/users/u42/actions/" + action + "/resolve", new ResolveActionRequest("approve", null, null)).as(Object.class);
        settle();

        UpdatePage page = alice.get("/v1/users/u42/updates").as(UpdatePage.class);

        assertThat(page.updates()).extracting(Update::seq, Update::type).containsExactly(
                org.assertj.core.groups.Tuple.tuple(1L, "message.created"),
                org.assertj.core.groups.Tuple.tuple(2L, "action.created"),
                org.assertj.core.groups.Tuple.tuple(3L, "action.resolved"),
                org.assertj.core.groups.Tuple.tuple(4L, "message.created"));
        assertThat(page.updates().get(0).data().path("text").asString()).isEqualTo("close ticket T-1");
        assertThat(page.updates().get(1).data().path("status").asString()).isEqualTo("pending");
        assertThat(page.updates().get(2).data().path("status").asString()).isEqualTo("approved");
        assertThat(page.updates().get(3).data().path("kind").asString()).isEqualTo("reply");
        assertThat(page.updates()).allSatisfy(update -> {
            assertThat(update.userId()).isEqualTo(USER);
            assertThat(update.id()).startsWith("upd_");
        });
        assertThat(page.nextAfter()).isEqualTo(4);
        assertThat(alice.get("/v1/users/u42/updates?after=2&limit=1").as(UpdatePage.class).updates()).extracting(Update::seq)
                .containsExactly(3L);
        assertThat(alice.get("/v1/users/u42/updates?after=4").as(UpdatePage.class)).isEqualTo(new UpdatePage(List.of(), 4));
    }

    @Test
    void aRunThatFailsIsInTheLogToo() {
        userSays(alice, USER, "hello");
        settle();
        cma.hold(cma.sessionIds().getFirst());
        userSays(alice, USER, "a question that takes forever");
        settle();

        Update failed = alice.get("/v1/users/u42/updates").as(UpdatePage.class).updates().getLast();

        assertThat(failed.type()).isEqualTo("run.failed");
        assertThat(failed.data().path("conversation_id").asString()).isEqualTo("default");
        assertThat(failed.data().path("error").asString()).isEqualTo("the turn took longer than PT10M");
        assertThat(failed.data().path("run_id").asString())
                .isEqualTo(jdbc.sql("SELECT run_id FROM turn WHERE state = 'failed'").query(String.class).single());
    }

    @Test
    void eachUserReadsTheirOwnLog_andApplicationsDoNotMeet() {
        TestApi globex = app(GLOBEX);
        defaultAgent(globex);
        userSays(alice, USER, "mine");
        userSays(globex, USER, "theirs");
        settle();

        assertThat(alice.get("/v1/users/u42/updates").as(UpdatePage.class).updates())
                .extracting(update -> update.data().path("text").asString()).containsExactly("mine", "You said: mine");
        assertThat(globex.get("/v1/users/u42/updates").as(UpdatePage.class).updates())
                .extracting(update -> update.data().path("text").asString()).containsExactly("theirs", "You said: theirs");
        assertThat(user(acme, "bob").get("/v1/users/u42/updates").status()).isEqualTo(403);
        assertThat(acme.get("/v1/users/u42/updates?after=soon").status()).isEqualTo(400);
    }

    @Test
    void aReaderThatFollowsTheLogWhileManyWriteMissesNothing() throws Exception {
        int writers = 8;
        int each = 10;
        java.util.concurrent.atomic.AtomicBoolean writing = new java.util.concurrent.atomic.AtomicBoolean(true);
        List<Long> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
        Thread reader = Thread.ofVirtual().start(() -> {
            long after = 0;
            while (true) {
                boolean last = !writing.get();
                UpdatePage page = acme.get("/v1/users/u42/updates?limit=7&after=" + after).as(UpdatePage.class);
                page.updates().forEach(update -> seen.add(update.seq()));
                after = page.nextAfter();
                if (last && page.updates().isEmpty()) {
                    return;
                }
            }
        });
        try (java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int w = 0; w < writers; w++) {
                int writer = w;
                pool.submit(() -> {
                    for (int i = 0; i < each; i++) {
                        acme.post("/v1/users/u42/messages", new PostMessageRequest("w" + writer + "-" + i, null, null, "message " + i))
                                .as(MessageAccepted.class);
                    }
                    return null;
                });
            }
        }
        writing.set(false);
        reader.join(Duration.ofSeconds(30));

        List<Long> all = java.util.stream.LongStream.rangeClosed(1, writers * each).boxed().toList();
        assertThat(seen).as("every position once, in order, although the writes committed concurrently").isEqualTo(all);
    }

    // ================================================================ the stream

    @Test
    void theStreamShowsWhatHappens_asItHappens() {
        try (SseReader stream = SseReader.open(streamUri(""), "Authorization", "Bearer " + bearerOf(alice))) {
            assertThat(stream.status()).isEqualTo(200);
            assertThat(stream.headers().get("content-type").getFirst()).startsWith("text/event-stream");

            userSays(alice, USER, "hello");
            settle();

            List<SseReader.Event> events = stream.next(2);
            assertThat(events).extracting(SseReader.Event::id, SseReader.Event::name)
                    .containsExactly(org.assertj.core.groups.Tuple.tuple("1", "message.created"),
                            org.assertj.core.groups.Tuple.tuple("2", "message.created"));
            JsonNode reply = AgentJson.mapper().readTree(events.get(1).data());
            assertThat(reply.path("type").asString()).isEqualTo("message.created");
            assertThat(reply.path("data").path("text").asString()).isEqualTo("You said: hello");
        }
    }

    @Test
    void aClientThatConnectsAgainWithTheLastIdItGot_missesNothing() {
        userSays(alice, USER, "one");
        settle();
        try (SseReader first = SseReader.open(streamUri("?after=0"), "Authorization", "Bearer " + bearerOf(alice))) {
            assertThat(first.next(2)).extracting(SseReader.Event::id).containsExactly("1", "2");
        }
        userSays(alice, USER, "two");
        settle();

        try (SseReader again = SseReader.open(streamUri("?after=0"), "Authorization", "Bearer " + bearerOf(alice), "Last-Event-ID", "2")) {
            List<SseReader.Event> missed = again.next(2);
            assertThat(missed).extracting(SseReader.Event::id).containsExactly("3", "4");
            assertThat(AgentJson.mapper().readTree(missed.get(1).data()).path("data").path("text").asString())
                    .isEqualTo("You said: two");
        }
    }

    @Test
    void aStreamWithoutAStartingPointShowsOnlyWhatHappensFromNowOn() {
        userSays(alice, USER, "before");
        settle();

        try (SseReader stream = SseReader.open(streamUri(""), "Authorization", "Bearer " + bearerOf(alice))) {
            assertThat(stream.quietFor(Duration.ofMillis(300))).isTrue();
            userSays(alice, USER, "after");
            assertThat(stream.next().id()).isEqualTo("3");
        }
    }

    @Test
    void aBrowserOpensTheStreamWithATicket_onceAndForItsUserOnly() {
        StreamTicket ticket = alice.post("/v1/users/u42/stream-tickets", null).as(StreamTicket.class);
        assertThat(ticket.ticket()).startsWith("stk_");
        assertThat(ticket.expiresAt()).isEqualTo(clock.now().plus(Duration.ofMinutes(1)));

        try (SseReader stream = SseReader.open(streamUri("?ticket=" + ticket.ticket()))) {
            assertThat(stream.status()).isEqualTo(200);
            userSays(alice, USER, "hello");
            assertThat(stream.next().name()).isEqualTo("message.created");
        }
        try (SseReader again = SseReader.open(streamUri("?ticket=" + ticket.ticket()))) {
            assertThat(again.status()).as("a ticket is good once").isEqualTo(401);
        }

        StreamTicket forBob = acme.post("/v1/users/bob/stream-tickets", null).as(StreamTicket.class);
        try (SseReader wrongUser = SseReader.open(streamUri("?ticket=" + forBob.ticket()))) {
            assertThat(wrongUser.status()).as("bob's ticket does not open u42's stream").isEqualTo(403);
        }
        StreamTicket late = alice.post("/v1/users/u42/stream-tickets", null).as(StreamTicket.class);
        clock.advance(Duration.ofMinutes(2));
        try (SseReader expired = SseReader.open(streamUri("?ticket=" + late.ticket()))) {
            assertThat(expired.status()).isEqualTo(401);
        }
        assertThat(anonymous.get("/v1/users/u42/updates?ticket=" + late.ticket()).status())
                .as("a ticket opens the stream and nothing else").isEqualTo(401);
        assertThat(user(acme, "bob").post("/v1/users/u42/stream-tickets", null).status()).isEqualTo(403);
    }

    // ================================================================ the webhook

    @Test
    void updatesAreDeliveredToTheWebhook_signed() {
        properties.getWebhooks().setAllowPrivateNetworks(true);
        properties.getWebhooks().setAllowPlainHttp(true);
        try (HookReceiver receiver = new HookReceiver()) {
            WebhookView created = acme.put("/v1/webhook", new WebhookRequest(receiver.url("/hooks/agent?source=agentflow"), null, null, null))
                    .as(WebhookView.class);
            assertThat(created.state()).isEqualTo("active");
            assertThat(created.signingSecret()).startsWith("whsec_");
            assertThat(acme.get("/v1/webhook").as(WebhookView.class).signingSecret()).as("shown once").isNull();

            userSays(alice, USER, "hello");
            settle();
            assertThat(dispatcher.runOnce()).isEqualTo(2);

            assertThat(receiver.deliveries()).hasSize(2);
            HookReceiver.Delivery reply = receiver.deliveries().get(1);
            assertThat(reply.path()).isEqualTo("/hooks/agent?source=agentflow");
            assertThat(reply.headers()).containsEntry("content-type", "application/json");
            assertThat(WebhookSignature.verify(created.signingSecret(), reply.headers().get("webhook-id"),
                    reply.headers().get("webhook-timestamp"), reply.body(), reply.headers().get("webhook-signature"), clock.now(),
                    Duration.ofMinutes(5))).isTrue();
            assertThat(WebhookSignature.verify(created.signingSecret(), reply.headers().get("webhook-id"),
                    reply.headers().get("webhook-timestamp"), "{\"forged\": true}".getBytes(), reply.headers().get("webhook-signature"),
                    clock.now(), Duration.ofMinutes(5))).isFalse();
            Update update = AgentJson.mapper().readValue(reply.body(), Update.class);
            assertThat(update.id()).isEqualTo(reply.headers().get("webhook-id"));
            assertThat(update.userId()).isEqualTo(USER);
            assertThat(update.seq()).isEqualTo(2);
            assertThat(update.data().path("text").asString()).isEqualTo("You said: hello");
            assertThat(dispatcher.runOnce()).as("each once").isZero();
        }
    }

    @Test
    void theWebhookGetsTheTypesItAsksFor() {
        properties.getWebhooks().setAllowPrivateNetworks(true);
        properties.getWebhooks().setAllowPlainHttp(true);
        try (HookReceiver receiver = new HookReceiver()) {
            acme.put("/v1/webhook", new WebhookRequest(receiver.url("/"), List.of("run.failed"), null, null)).as(WebhookView.class);
            userSays(alice, USER, "hello");
            settle();
            cma.hold(cma.sessionIds().getFirst());
            userSays(alice, USER, "a question that takes forever");
            settle();

            dispatcher.runOnce();

            assertThat(receiver.deliveries()).singleElement()
                    .satisfies(delivery -> assertThat(AgentJson.mapper().readValue(delivery.body(), Update.class).type())
                            .isEqualTo("run.failed"));
        }
    }

    @Test
    void aDeliveryThatFailsIsTriedAgain_withTheSameId() {
        properties.getWebhooks().setAllowPrivateNetworks(true);
        properties.getWebhooks().setAllowPlainHttp(true);
        try (HookReceiver receiver = new HookReceiver().answer(503, 302)) {
            acme.put("/v1/webhook", new WebhookRequest(receiver.url("/"), List.of("message.created"), null, null)).as(WebhookView.class);
            userSays(alice, USER, "hello");

            assertThat(dispatcher.runOnce()).isZero();
            assertThat(dispatcher.runOnce()).as("not before its time").isZero();
            clock.advance(Duration.ofSeconds(10));
            assertThat(dispatcher.runOnce()).as("a redirect is not followed: it counts as a failure").isZero();
            clock.advance(Duration.ofMinutes(1));
            assertThat(dispatcher.runOnce()).isEqualTo(1);

            assertThat(receiver.deliveries()).hasSize(3)
                    .extracting(delivery -> delivery.headers().get("webhook-id")).containsOnly(receiver.deliveries().getFirst().headers().get("webhook-id"));
            assertThat(jdbc.sql("SELECT status || ' ' || attempts FROM webhook_delivery").query(String.class).single()).isEqualTo("delivered 3");
        }
    }

    @Test
    void aWebhookThatKeepsFailingIsTurnedOff_untilTheApplicationTurnsItOnAgain() {
        properties.getWebhooks().setAllowPrivateNetworks(true);
        properties.getWebhooks().setAllowPlainHttp(true);
        try (HookReceiver receiver = new HookReceiver().otherwise(500)) {
            acme.put("/v1/webhook", new WebhookRequest(receiver.url("/"), List.of("message.created"), null, null)).as(WebhookView.class);
            userSays(alice, USER, "hello");

            for (Duration delay : properties.getWebhooks().getRetryDelays()) {
                dispatcher.runOnce();
                clock.advance(delay);
            }
            dispatcher.runOnce();

            assertThat(receiver.deliveries()).hasSize(8);
            WebhookView off = acme.get("/v1/webhook").as(WebhookView.class);
            assertThat(off.state()).isEqualTo("disabled");
            assertThat(off.disabledReason()).isEqualTo("a delivery failed 8 times, the last time because the receiver answered 500");
            // A day has passed: the user's token has long expired. The application's back end writes on.
            userSays(acme, USER, "while it is off");
            dispatcher.runOnce();
            assertThat(receiver.deliveries()).as("nothing more while it is off").hasSize(8);

            receiver.otherwise(200);
            assertThat(acme.put("/v1/webhook", new WebhookRequest(receiver.url("/"), List.of("message.created"), true, null))
                    .as(WebhookView.class).state()).isEqualTo("active");
            userSays(acme, USER, "after it is on again");
            assertThat(dispatcher.runOnce()).isEqualTo(1);
            assertThat(AgentJson.mapper().readValue(receiver.deliveries().getLast().body(), Update.class).data().path("text").asString())
                    .isEqualTo("after it is on again");
        }
    }

    @Test
    void aWebhookCannotReachWhatIsBehindTheService() {
        for (String url : List.of("https://127.0.0.1/hook", "https://localhost/hook", "https://10.0.0.5/hook", "https://192.168.1.10/hook",
                "https://169.254.169.254/latest/meta-data/", "https://[::1]/hook", "https://[::ffff:127.0.0.1]/hook",
                "https://[fd00:ec2::254]/hook", "http://8.8.8.8/hook", "https://user:pass@8.8.8.8/hook", "ftp://8.8.8.8/hook")) {
            TestApi.Reply refused = acme.put("/v1/webhook", new WebhookRequest(url, null, null, null));
            assertThat(refused.status()).as(url).isEqualTo(400);
            assertThat(refused.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        }
        assertThat(acme.get("/v1/webhook").status()).as("nothing was set up").isEqualTo(404);
    }

    @Test
    void anAddressThatTurnsPrivateAfterwards_isNotDeliveredTo() {
        properties.getWebhooks().setAllowPrivateNetworks(true);
        properties.getWebhooks().setAllowPlainHttp(true);
        try (HookReceiver receiver = new HookReceiver()) {
            acme.put("/v1/webhook", new WebhookRequest(receiver.url("/"), List.of("message.created"), null, null)).as(WebhookView.class);
            properties.getWebhooks().setAllowPrivateNetworks(false);
            userSays(alice, USER, "hello");

            assertThat(dispatcher.runOnce()).isZero();

            assertThat(receiver.deliveries()).isEmpty();
            assertThat(jdbc.sql("SELECT last_error FROM webhook_delivery").query(String.class).single())
                    .startsWith("not sent: the host 127.0.0.1 has the address 127.0.0.1, which is the host itself");
        }
    }

    @Test
    void theWebhookIsTheApplicationsBackEndsBusiness() {
        properties.getWebhooks().setAllowPrivateNetworks(true);
        properties.getWebhooks().setAllowPlainHttp(true);
        assertThat(alice.put("/v1/webhook", new WebhookRequest("http://127.0.0.1:9/", null, null, null)).status()).isEqualTo(403);
        assertThat(alice.get("/v1/webhook").status()).isEqualTo(403);
        assertThat(acme.get("/v1/webhook").status()).isEqualTo(404);
        assertThat(acme.put("/v1/webhook", new WebhookRequest("http://127.0.0.1:9/", List.of("message.deleted"), null, null)).status())
                .isEqualTo(400);
        assertThat(acme.put("/v1/webhook", "{\"url\": \"http://127.0.0.1:9/\", \"retries\": 3}").status()).isEqualTo(400);

        WebhookView first = acme.put("/v1/webhook", new WebhookRequest("http://127.0.0.1:9/", null, null, null)).as(WebhookView.class);
        WebhookView unchanged = acme.put("/v1/webhook", new WebhookRequest("http://127.0.0.1:9/other", null, null, null)).as(WebhookView.class);
        WebhookView rotated = acme.put("/v1/webhook", new WebhookRequest("http://127.0.0.1:9/other", null, null, true)).as(WebhookView.class);
        assertThat(unchanged.signingSecret()).isNull();
        assertThat(rotated.signingSecret()).startsWith("whsec_").isNotEqualTo(first.signingSecret());
        WebhookView paused = acme.put("/v1/webhook", new WebhookRequest("http://127.0.0.1:9/other", null, false, null)).as(WebhookView.class);
        assertThat(paused.state()).isEqualTo("disabled");
        assertThat(paused.disabledReason()).isEqualTo("turned off by the application");

        assertThat(acme.delete("/v1/webhook").status()).isEqualTo(204);
        assertThat(acme.get("/v1/webhook").status()).isEqualTo(404);
        assertThat(acme.delete("/v1/webhook").status()).isEqualTo(204);
    }

    // ================================================================ limits

    @Test
    void aCallerThatMakesTooManyRequestsIsToldToWait() {
        properties.getLimits().setUserBurst(3);
        properties.getLimits().setUserPerSecond(1);
        properties.getLimits().setAppBurst(5);
        properties.getLimits().setAppPerSecond(2);
        try {
            limiter.reset();
            for (int i = 0; i < 3; i++) {
                assertThat(alice.get("/v1/users/u42/messages").status()).isEqualTo(200);
            }
            TestApi.Reply limited = alice.get("/v1/users/u42/messages");
            assertThat(limited.status()).isEqualTo(429);
            assertThat(limited.code()).isEqualTo(ProblemCodes.RATE_LIMITED);
            assertThat(limited.headers().firstValue("Retry-After")).hasValue("1");
            assertThat(user(acme, "bob").get("/v1/users/bob/messages").status()).as("every user has a bucket of their own").isEqualTo(200);

            clock.advance(Duration.ofSeconds(1));
            assertThat(alice.get("/v1/users/u42/messages").status()).isEqualTo(200);

            int ok = 0;
            while (acme.post("/v1/users/u42/messages", new PostMessageRequest(null, null, null, "hi")).status() == 202) {
                ok++;
            }
            assertThat(ok).as("the application's bucket, less what the token requests above took from it").isLessThanOrEqualTo(5);
            assertThat(admin.get("/v1/admin/apps").status()).as("operators are not limited").isEqualTo(200);
        } finally {
            properties.getLimits().setUserBurst(1_000_000);
            properties.getLimits().setUserPerSecond(1_000_000);
            properties.getLimits().setAppBurst(1_000_000);
            properties.getLimits().setAppPerSecond(1_000_000);
            limiter.reset();
        }
    }

    // ================================================================ helpers

    private java.net.URI streamUri(String query) {
        return baseUri().resolve("/v1/users/" + USER + "/stream" + query);
    }

    private static String bearerOf(TestApi caller) {
        return caller.credential();
    }

    @SuppressWarnings("unused")
    private MessageAccepted post(TestApi caller, String text) {
        return caller.post("/v1/users/" + USER + "/messages", new PostMessageRequest(null, null, null, text)).as(MessageAccepted.class);
    }
}
