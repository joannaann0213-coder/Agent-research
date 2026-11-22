package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.MessageAccepted;
import dev.agentflow.agent.api.MessagePage;
import dev.agentflow.agent.api.MessageView;
import dev.agentflow.agent.api.PostMessageRequest;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A user's message goes in, one run of the Inbox works on it, and the agent's reply comes out. */
class ChatTurnIT extends AgentTestBase {

    @Test
    void aMessageIsAnswered() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say("Your order 1234 is on its way."));

        MessageAccepted accepted = acme.post("/v1/users/u42/messages",
                new PostMessageRequest("m-1", null, null, "Where is my order 1234?")).as(MessageAccepted.class);

        assertThat(accepted).isEqualTo(new MessageAccepted("m-1", "default", 1, clock.now(), false));
        assertThat(messagesOf(acme, "u42")).as("stored, not answered yet").extracting(MessageView::kind).containsExactly("message");

        assertThat(settle()).isEqualTo(1);

        List<MessageView> messages = messagesOf(acme, "u42");
        assertThat(messages).extracting(MessageView::seq, MessageView::role, MessageView::kind, MessageView::text)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "user", "message", "Where is my order 1234?"),
                        org.assertj.core.groups.Tuple.tuple(2L, "agent", "reply", "Your order 1234 is on its way."));
        assertThat(messages.get(1).conversationId()).isEqualTo("default");
        assertThat(messages.get(1).createdAt()).isEqualTo(clock.now());
        assertThat(cma.userMessages(cma.sessionIds().getFirst())).containsExactly("Where is my order 1234?");
        assertThat(count("SELECT count(*) FROM turn WHERE state = 'done' AND outcome = 'replied'")).isEqualTo(1);
        assertThat(settle()).as("nothing is left to do").isZero();
    }

    @Test
    void messagesThatArriveCloseTogetherAreAnsweredTogether_inOrder() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say("Got " + context.userMessage().split("\n\n").length + " messages."));

        for (String part : List.of("I need help", "with my order", "number 1234", "it is late", "can you check?")) {
            userSays(acme, "u42", part);
        }

        assertThat(settle()).as("one run for the five of them").isEqualTo(1);
        assertThat(cma.userMessages(cma.sessionIds().getFirst()))
                .containsExactly("I need help\n\nwith my order\n\nnumber 1234\n\nit is late\n\ncan you check?");
        assertThat(messagesOf(acme, "u42")).filteredOn(message -> message.role().equals("agent"))
                .extracting(MessageView::text).containsExactly("Got 5 messages.");
    }

    @Test
    void aConversationGoesOnInOneSession_soTheAgentRemembers() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say("That makes " + context.userMessages().size() + "."));

        userSays(acme, "u42", "one");
        settle();
        userSays(acme, "u42", "two");
        settle();
        userSays(acme, "u42", "three");
        settle();

        assertThat(cma.sessionIds()).hasSize(1);
        assertThat(messagesOf(acme, "u42")).filteredOn(message -> message.role().equals("agent"))
                .extracting(MessageView::text).containsExactly("That makes 1.", "That makes 2.", "That makes 3.");
        assertThat(cma.count(Operation.CREATE_SESSION)).isEqualTo(1);
        assertThat(cma.sessionSpec(cma.sessionIds().getFirst()).metadata())
                .containsEntry("app", ACME).containsEntry("user", "u42").containsEntry("line", "conv:default");
    }

    @Test
    void sendingAMessageAgainIsHarmless_andAnIdIsNotReusedForAnotherText() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        PostMessageRequest message = new PostMessageRequest("m-1", null, null, "hello");

        MessageAccepted first = acme.post("/v1/users/u42/messages", message).as(MessageAccepted.class);
        MessageAccepted retry = acme.post("/v1/users/u42/messages", message).as(MessageAccepted.class);

        assertThat(retry).isEqualTo(new MessageAccepted("m-1", "default", first.seq(), first.createdAt(), true));
        assertThat(count("SELECT count(*) FROM message")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM inbox_relay")).isEqualTo(1);

        TestApi.Reply reused = acme.post("/v1/users/u42/messages", new PostMessageRequest("m-1", null, null, "something else"));
        assertThat(reused.status()).isEqualTo(409);
        assertThat(reused.code()).isEqualTo(ProblemCodes.CONFLICT);

        settle();
        MessageAccepted late = acme.post("/v1/users/u42/messages", message).as(MessageAccepted.class);
        assertThat(late.deduped()).as("also after it was answered").isTrue();
        assertThat(settle()).isZero();
        assertThat(messagesOf(acme, "u42")).hasSize(2);
        assertThat(acme.post("/v1/users/u42/messages", new PostMessageRequest(null, null, null, "no id given")).as(MessageAccepted.class)
                .messageId()).startsWith("msg_");
    }

    @Test
    void eachConversationOfAUserHasItsOwnSession_andItsOwnReplies() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        cma.brain(context -> say("re: " + context.userMessage()));

        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "orders", null, "about my order")).as(MessageAccepted.class);
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "billing", null, "about my bill")).as(MessageAccepted.class);
        assertThat(settle()).isEqualTo(2);

        assertThat(cma.sessionIds()).hasSize(2);
        MessagePage orders = acme.get("/v1/users/u42/messages?conversation_id=orders").as(MessagePage.class);
        assertThat(orders.messages()).extracting(MessageView::text).containsExactly("about my order", "re: about my order");
        assertThat(acme.get("/v1/users/u42/messages?conversation_id=billing").as(MessagePage.class).messages())
                .extracting(MessageView::text).containsExactly("about my bill", "re: about my bill");
        assertThat(messagesOf(acme, "u42")).as("one stream of positions across the user's conversations")
                .extracting(MessageView::seq).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void aConversationKeepsTheAgentOfItsFirstMessage() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        acme.put("/v1/agents/sales", new AgentProfileRequest("Sales", null, null, "You sell.", null, null)).as(AgentProfileView.class);
        cma.brain(context -> say("I am " + context.agent().description()));

        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "c1", "sales", "hi")).as(MessageAccepted.class);
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "c1", null, "still you?")).as(MessageAccepted.class);
        acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "c2", null, "and here?")).as(MessageAccepted.class);
        settle();

        assertThat(acme.get("/v1/users/u42/messages?conversation_id=c1").as(MessagePage.class).messages().getLast().text())
                .isEqualTo("I am Sales");
        assertThat(acme.get("/v1/users/u42/messages?conversation_id=c2").as(MessagePage.class).messages().getLast().text())
                .as("without a name, the agent called default").isEqualTo("I am Assistant");

        TestApi.Reply other = acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "c1", "default", "switch"));
        assertThat(other.status()).isEqualTo(409);
        assertThat(other.code()).isEqualTo(ProblemCodes.CONFLICT);
        TestApi.Reply unknown = acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "c3", "nobody", "hello"));
        assertThat(unknown.status()).isEqualTo(400);
        assertThat(unknown.body().path("detail").asString()).contains("PUT /v1/agents/nobody");
    }

    @Test
    void aUsersClientCanTalkForItsUser_andForNobodyElse() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        TestApi alice = user(acme, "alice");
        userSays(acme, "bob", "bob's secret");

        assertThat(userSays(alice, "alice", "hello from my phone").seq()).isEqualTo(1);
        settle();
        assertThat(messagesOf(alice, "alice")).extracting(MessageView::text)
                .containsExactly("hello from my phone", "You said: hello from my phone");

        assertThat(alice.post("/v1/users/bob/messages", new PostMessageRequest(null, null, null, "as bob")).status()).isEqualTo(403);
        TestApi.Reply peek = alice.get("/v1/users/bob/messages");
        assertThat(peek.status()).isEqualTo(403);
        assertThat(peek.code()).isEqualTo(ProblemCodes.FORBIDDEN);
        assertThat(peek.body().toString()).doesNotContain("secret");
    }

    @Test
    void twoApplicationsWithTheSameUserAndConversationIdsDoNotMeet() {
        TestApi acme = app(ACME);
        TestApi globex = app(GLOBEX);
        defaultAgent(acme);
        defaultAgent(globex);
        cma.brain(context -> say("for " + context.sessionMetadata().get("app")));

        acme.post("/v1/users/u42/messages", new PostMessageRequest("m-1", "c1", null, "from acme")).as(MessageAccepted.class);
        globex.post("/v1/users/u42/messages", new PostMessageRequest("m-1", "c1", null, "from globex")).as(MessageAccepted.class);
        assertThat(settle()).isEqualTo(2);

        assertThat(messagesOf(acme, "u42")).extracting(MessageView::text).containsExactly("from acme", "for acme");
        assertThat(messagesOf(globex, "u42")).extracting(MessageView::text).containsExactly("from globex", "for globex");
        assertThat(cma.sessionIds()).hasSize(2);
    }

    @Test
    void messagesAreReadPageByPage() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        for (int i = 1; i <= 3; i++) {
            userSays(acme, "u42", "message " + i);
            settle();
        }

        MessagePage first = acme.get("/v1/users/u42/messages?limit=4").as(MessagePage.class);
        assertThat(first.messages()).extracting(MessageView::seq).containsExactly(1L, 2L, 3L, 4L);
        assertThat(first.nextAfter()).isEqualTo(4);
        MessagePage second = acme.get("/v1/users/u42/messages?limit=4&after=" + first.nextAfter()).as(MessagePage.class);
        assertThat(second.messages()).extracting(MessageView::seq).containsExactly(5L, 6L);
        MessagePage nothingNew = acme.get("/v1/users/u42/messages?after=" + second.nextAfter()).as(MessagePage.class);
        assertThat(nothingNew.messages()).isEmpty();
        assertThat(nothingNew.nextAfter()).as("ask again with the same value").isEqualTo(6);
        assertThat(acme.get("/v1/users/nobody-yet/messages").as(MessagePage.class).messages()).isEmpty();
    }

    @Test
    void invalidMessagesAreRefusedWithoutStoringAnything() {
        TestApi acme = app(ACME);
        defaultAgent(acme);

        assertThat(acme.post("/v1/users/u42/messages", new PostMessageRequest(null, null, null, " ")).status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/u42/messages", "{}").status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/u42/messages", new PostMessageRequest("bad id", null, null, "x")).status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/u42/messages", new PostMessageRequest(null, "bad/conversation", null, "x")).status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/u42/messages", new PostMessageRequest(null, null, "Bad Agent", "x")).status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/bad%20user/messages", new PostMessageRequest(null, null, null, "x")).status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/u42/messages", "{\"text\": \"x\", \"txet\": \"y\"}").status()).isEqualTo(400);
        TestApi.Reply tooLong = acme.post("/v1/users/u42/messages", new PostMessageRequest(null, null, null, "x".repeat(32_001)));
        assertThat(tooLong.status()).isEqualTo(413);
        assertThat(tooLong.code()).isEqualTo(ProblemCodes.PAYLOAD_TOO_LARGE);
        assertThat(acme.get("/v1/users/u42/messages?after=soon").status()).isEqualTo(400);
        assertThat(acme.get("/v1/users/u42/messages?conversation_id=bad/id").status()).isEqualTo(400);

        assertThat(count("SELECT count(*) FROM message")).isZero();
        assertThat(count("SELECT count(*) FROM inbox_relay")).isZero();
        assertThat(settle()).isZero();
    }

    @Test
    void anApplicationWithoutAnAgentIsToldHowToMakeOne() {
        TestApi acme = app(ACME);

        TestApi.Reply reply = acme.post("/v1/users/u42/messages", new PostMessageRequest(null, null, null, "hello"));

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.body().path("detail").asString()).contains("no agent default").contains("PUT /v1/agents/default");
    }

    @Test
    void aToolCallNobodyCanApproveIsDenied_andTheAgentSaysSoInsteadOfHanging() {
        TestApi acme = app(ACME);
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null,
                List.of(new McpServerConfig("tickets", "https://mcp.acme.example/tickets", "always_ask", "app")))).as(AgentProfileView.class);
        cma.brain(context -> context.step() == 0 ? ModelStep.of(mcp("tickets", "close_ticket", "{\"id\": \"T-1\"}"))
                : say(context.toolResults().getFirst().error() ? "I could not close it: " + context.toolResults().getFirst().text()
                        : "Closed."));

        userSays(acme, "u42", "close ticket T-1");
        settle();

        assertThat(messagesOf(acme, "u42").getLast().text()).startsWith("I could not close it: This action needs an approval");
        assertThat(cma.executions()).as("the ticket system was not touched").isEmpty();
    }

    @Test
    void theTextOfAMessageStaysInThisService_theInboxOnlyLearnsThatThereIsOne() {
        TestApi acme = app(ACME);
        defaultAgent(acme);
        userSays(acme, "u42", "my card number is 4111 1111 1111 1111");
        relay.runOnce();

        String stored = dev.agentflow.agent.server.support.EmbeddedInbox.jdbc()
                .sql("SELECT payload::text || ' ' || source_event_id || ' ' || type FROM event").query(String.class).single();

        assertThat(stored).doesNotContain("4111").contains("message_id", "conversation_id", "agent.user_message.v1");
    }
}
