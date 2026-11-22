package dev.agentflow.agent.cma;

import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.sdk.SdkManagedAgents;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;

/**
 * The contract, on the SDK adapter. The adapter talks HTTP to a stub that answers in the API's wire
 * format; behind the stub is the same fake the other run uses. This proves the adapter builds its
 * requests and reads its responses as the API documents them. It cannot prove that the documents
 * match the service; the live smoke test is for that.
 */
class SdkManagedAgentsContractTest extends ManagedAgentsContract {

    private CmaWireStub stub;
    private SdkManagedAgents adapter;

    @Override
    protected ManagedAgents connect(FakeManagedAgents fake) {
        stub = new CmaWireStub(fake);
        adapter = SdkManagedAgents.connect("sk-test-key", stub.baseUrl(), Duration.ofSeconds(20));
        return adapter;
    }

    @AfterEach
    void disconnect() {
        adapter.close();
        stub.close();
    }
}
