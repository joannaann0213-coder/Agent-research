package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.api.SandboxConfig;
import dev.agentflow.agent.cma.SessionSpec;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * An agent that remembers each user across sessions: it writes its notes in conversations, and only
 * reads them in the background, where what it works on comes from other systems.
 */
class MemoryIT extends AgentTestBase {

    private static final SandboxConfig SANDBOX = new SandboxConfig(true, false, false, "none", null);

    private TestApi acme;

    @BeforeEach
    void anAgentThatRemembers() {
        acme = app(ACME);
        acme.put("/v1/agents/default", profile("per_user")).as(AgentProfileView.class);
    }

    @Test
    void aConversationWritesTheUsersMemory_andTheBackgroundOnlyReadsIt() {
        userSays(acme, "u42", "I prefer email over phone calls.");
        acme.post("/v1/users/u42/events", new PostEventRequest(null, "order.delayed", "order-1234", null, null, null, null))
                .as(EventAccepted.class);
        settle();

        String store = jdbc.sql("SELECT cma_memory_store_id FROM memory_store WHERE user_id = 'u42'").query(String.class).single();
        assertThat(mountsOf("conv:default")).singleElement().satisfies(mount -> {
            assertThat(mount.storeId()).isEqualTo(store);
            assertThat(mount.readOnly()).isFalse();
            assertThat(mount.instructions()).contains("Never store secrets");
        });
        assertThat(mountsOf("task:order-1234")).singleElement().satisfies(mount -> {
            assertThat(mount.storeId()).as("the same store").isEqualTo(store);
            assertThat(mount.readOnly()).isTrue();
        });
        assertThat(cma.memoryStores()).containsEntry(store, "acme / u42 / default");
    }

    @Test
    void everyUserHasAMemoryOfTheirOwn() {
        userSays(acme, "u42", "hello");
        userSays(acme, "u43", "hello");
        settle();

        assertThat(cma.memoryStores()).hasSize(2);
        assertThat(count("SELECT count(DISTINCT cma_memory_store_id) FROM memory_store")).isEqualTo(2);
    }

    @Test
    void turningMemoryOnOrOffTakesEffectAtTheNextTurn_inANewSession() {
        acme.put("/v1/agents/default", profile("none")).as(AgentProfileView.class);
        userSays(acme, "u42", "hello");
        settle();
        assertThat(mountsOf("conv:default")).isEmpty();

        acme.put("/v1/agents/default", profile("per_user")).as(AgentProfileView.class);
        userSays(acme, "u42", "hello again");
        settle();

        assertThat(cma.sessionIds()).hasSize(2);
        assertThat(mountsOf("conv:default")).hasSize(1);
        assertThat(acme.get("/v1/agents/default").as(AgentProfileView.class).memory()).isEqualTo("per_user");
    }

    @Test
    void memoryNeedsTheSandbox_whereTheAgentKeepsItsNotes() {
        TestApi.Reply withoutSandbox = acme.put("/v1/agents/default",
                new AgentProfileRequest("Assistant", null, null, "You help.", null, List.of(), null, "per_user"));
        assertThat(withoutSandbox.status()).isEqualTo(400);
        assertThat(withoutSandbox.body().path("detail").asString()).contains("memory needs the sandbox");
        assertThat(acme.put("/v1/agents/default", profile("forever")).status()).isEqualTo(400);
        assertThat(acme.get("/v1/agents/default").as(AgentProfileView.class).memory()).isEqualTo("per_user");
    }

    private static AgentProfileRequest profile(String memory) {
        return new AgentProfileRequest("Assistant", null, null, "You help.", SANDBOX, List.of(), null, memory);
    }

    /** The mounts of the newest session of a line. */
    private List<SessionSpec.MemoryMount> mountsOf(String line) {
        return cma.sessionIds().stream().filter(session -> line.equals(cma.sessionSpec(session).metadata().get("line")))
                .reduce((first, second) -> second).map(session -> cma.sessionSpec(session).memory()).orElseThrow();
    }
}
