package dev.agentflow.agent.cma;

import dev.agentflow.agent.cma.fake.FakeManagedAgents;

/** The contract, on the in-process fake itself. */
class FakeManagedAgentsContractTest extends ManagedAgentsContract {

    @Override
    protected ManagedAgents connect(FakeManagedAgents fake) {
        return fake;
    }
}
