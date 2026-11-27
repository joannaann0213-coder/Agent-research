package dev.agentflow.agent.cma;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.cma.fake.FakeBrains;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.sdk.SdkManagedAgents;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** What is particular to the SDK adapter: paging, retries, and events it has no type for. */
class SdkManagedAgentsTest {

    private FakeManagedAgents fake;
    private CmaWireStub stub;
    private SdkManagedAgents cma;
    private String session;

    @BeforeEach
    void connect() {
        fake = new FakeManagedAgents();
        fake.brain(FakeBrains.echo());
        stub = new CmaWireStub(fake);
        cma = SdkManagedAgents.connect("sk-test-key", stub.baseUrl(), Duration.ofSeconds(20));
        session = cma.createSession(new SessionSpec(cma.createAgent(ManagedAgentsContract.chatAgent()),
                cma.createEnvironment(new EnvironmentSpec("env", null, null, false, false, null)), null, null, null));
    }

    @AfterEach
    void disconnect() {
        cma.close();
        stub.close();
    }

    @Test
    void aLongEventListIsReadPageByPage_inOrder() {
        stub.pageSize(3);
        for (int i = 1; i <= 3; i++) {
            cma.send(session, List.of(new Outgoing.UserMessage("message " + i)));
        }

        List<CmaEvent> events = cma.listEvents(session, null);

        assertThat(events).hasSize(12);
        assertThat(events.stream().filter(CmaEvent.AgentMessage.class::isInstance).map(event -> ((CmaEvent.AgentMessage) event).text()))
                .containsExactly("You said: message 1", "You said: message 2", "You said: message 3");
        assertThat(events).extracting(CmaEvent::id).doesNotHaveDuplicates();
        assertThat(stub.requests().stream().filter(request -> request.equals("GET /v1/sessions/" + session + "/events")))
                .as("four pages of three").hasSize(4);
    }

    @Test
    void eventsWithoutATypeOfTheirOwnArriveAsOther_andPreviewsAreLeftOut() {
        try (EventStream stream = cma.openStream(session)) {
            stub.emitRaw(session, "{\"type\": \"span.model_request_start\", \"id\": \"sevt_span\", \"processed_at\": \"2026-11-20T10:00:01Z\"}");
            stub.emitRaw(session, "{\"type\": \"event_start\", \"event\": {\"type\": \"agent.message\", \"id\": \"sevt_preview\"}}");
            stub.emitRaw(session, "{\"type\": \"event_delta\", \"event_id\": \"sevt_preview\", \"delta\": {\"type\": \"content_delta\", \"index\": 0, \"content\": {\"type\": \"text\", \"text\": \"Hel\"}}}");
            stub.emitRaw(session, "{\"type\": \"session.invented_next_year\", \"id\": \"sevt_new\", \"processed_at\": \"2026-11-20T10:00:02Z\", \"anything\": {\"goes\": true}}");
            cma.send(session, List.of(new Outgoing.UserMessage("hi")));

            List<CmaEvent> events = new ArrayList<>();
            while (events.isEmpty() || !(events.getLast() instanceof CmaEvent.Idle)) {
                Optional<CmaEvent> next = stream.poll(Duration.ofSeconds(10));
                assertThat(next).as("the turn's events arrive").isPresent();
                events.add(next.get());
            }

            assertThat(events.get(0)).isEqualTo(new CmaEvent.Other("sevt_span", java.time.Instant.parse("2026-11-20T10:00:01Z"), "span.model_request_start"));
            assertThat(events.get(1)).as("a type that did not exist when this was written")
                    .isEqualTo(new CmaEvent.Other("sevt_new", java.time.Instant.parse("2026-11-20T10:00:02Z"), "session.invented_next_year"));
            assertThat(events.subList(2, events.size()).stream().map(CmaEvent::type)).containsExactly(
                    "session.status_running", "user.message", "agent.message", "session.status_idle");
        }
    }

    @Test
    void aReadIsRetriedByTheSdk_aSendIsNot() {
        long before = fake.count(Operation.GET_SESSION);
        fake.failNext(Operation.GET_SESSION, CmaException.overloaded());

        assertThat(cma.getSession(session).status()).isEqualTo(SessionInfo.Status.IDLE);
        assertThat(fake.count(Operation.GET_SESSION) - before).as("the failed attempt and the retry").isEqualTo(2);

        fake.failNext(Operation.SEND, CmaException.overloaded());
        assertThat(ManagedAgentsContract.kindOf(() -> cma.send(session, List.of(new Outgoing.UserMessage("hi")))))
                .isEqualTo(CmaException.Kind.OVERLOADED);
        assertThat(fake.count(Operation.SEND)).isEqualTo(1);
    }

    @Test
    void aServerThatCannotBeReachedIsAConnectionFailure() {
        try (SdkManagedAgents nowhere = SdkManagedAgents.connect("sk-test-key", "http://127.0.0.1:1", Duration.ofSeconds(5))) {
            CmaException failure = org.assertj.core.api.Assertions.catchThrowableOfType(CmaException.class,
                    () -> nowhere.send("sesn_1", List.of(new Outgoing.UserMessage("hi"))));

            assertThat(failure.kind()).isEqualTo(CmaException.Kind.CONNECTION);
            assertThat(failure.status()).isZero();
            assertThat(failure.retryable()).isTrue();
            assertThat(failure.maybeDelivered()).isTrue();
        }
    }

    @Test
    void everyRequestCarriesTheKeyAndTheBetaHeader() {
        // The stub refuses a request without either, as the service does; getting this far proves both.
        assertThat(stub.requests()).contains("POST /v1/environments", "POST /v1/agents", "POST /v1/sessions");
    }
}
