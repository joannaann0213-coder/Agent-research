package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.SandboxConfig;
import dev.agentflow.agent.cma.AgentRef;
import dev.agentflow.agent.cma.AgentSpec;
import dev.agentflow.agent.cma.AgentSpec.ToolPolicy;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.EnvironmentSpec;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.server.profile.AgentProfiles;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** An application describes its agent; the service keeps the description and sets the agent up on the platform. */
class AgentProfilesIT extends AgentTestBase {

    private static final AgentProfileRequest MINIMAL =
            new AgentProfileRequest("Support", null, null, "You help customers with their orders.", null, null);

    @Autowired
    AgentProfiles profiles;

    @Test
    void aNewAgentGetsEveryDefault_andIsSetUpOnThePlatformWithItsContainersClosedOff() {
        TestApi acme = app(ACME);

        TestApi.Reply reply = acme.put("/v1/agents/support", MINIMAL);

        assertThat(reply.status()).isEqualTo(201);
        AgentProfileView agent = reply.as(AgentProfileView.class);
        assertThat(agent).isEqualTo(new AgentProfileView("support", "Support", "claude-opus-5-5", null,
                "You help customers with their orders.", new SandboxConfig(false, false, false, "none", List.of()),
                List.of(), null, 1, clock.now(), clock.now()));

        AgentRef platformAgent = cma.agents().getFirst();
        assertThat(platformAgent.version()).isEqualTo(1);
        AgentSpec spec = cma.agentSpec(platformAgent);
        assertThat(spec.model()).isEqualTo("claude-opus-5-5");
        assertThat(spec.effort()).isNull();
        assertThat(spec.system()).startsWith("You help customers with their orders.")
                .contains("a note from the service that runs you");
        assertThat(spec.sandbox()).as("no container tools unless asked for").isNull();
        assertThat(spec.mcpServers()).isEmpty();
        assertThat(spec.metadata()).isEqualTo(Map.of("app", "acme", "agent", "support"));
        EnvironmentSpec environment = cma.environmentSpec(cma.environmentIds().getFirst());
        assertThat(environment.allowedHosts()).as("a container that can reach nothing").isEmpty();
        assertThat(environment.metadata()).isEqualTo(Map.of("app", "acme", "agent", "support"));
    }

    @Test
    void everythingAnApplicationConfiguresReachesThePlatform() {
        TestApi acme = app(ACME);
        AgentProfileRequest full = new AgentProfileRequest("Ops", "claude-sonnet-5-5", "high", "You run operations.",
                new SandboxConfig(true, true, false, "limited", List.of("api.acme.example", "docs.acme.example")),
                List.of(new McpServerConfig("tickets", "https://mcp.acme.example/tickets", null, null),
                        new McpServerConfig("wiki", "https://mcp.acme.example/wiki", "always_allow", "user"),
                        new McpServerConfig("deploy", "https://mcp.acme.example/deploy", "auto", "app")));

        AgentProfileView agent = acme.put("/v1/agents/ops", full).as(AgentProfileView.class);

        assertThat(agent.mcpServers()).as("approval and approver have defaults")
                .first().isEqualTo(new McpServerConfig("tickets", "https://mcp.acme.example/tickets", "always_ask", "app"));
        AgentSpec spec = cma.agentSpec(cma.agents().getFirst());
        assertThat(spec.model()).isEqualTo("claude-sonnet-5-5");
        assertThat(spec.effort()).isEqualTo("high");
        assertThat(spec.sandbox()).isEqualTo(new AgentSpec.Sandbox(ToolPolicy.AUTO, true, false));
        assertThat(spec.mcpServers()).containsExactly(
                new AgentSpec.McpServer("tickets", "https://mcp.acme.example/tickets", ToolPolicy.ALWAYS_ASK),
                new AgentSpec.McpServer("wiki", "https://mcp.acme.example/wiki", ToolPolicy.ALWAYS_ALLOW),
                new AgentSpec.McpServer("deploy", "https://mcp.acme.example/deploy", ToolPolicy.AUTO));
        assertThat(cma.environmentSpec(cma.environmentIds().getFirst()).allowedHosts())
                .containsExactly("api.acme.example", "docs.acme.example");

        acme.put("/v1/agents/open", new AgentProfileRequest("Open", null, null, "Research.",
                new SandboxConfig(true, true, true, "unrestricted", null), null)).as(AgentProfileView.class);
        assertThat(cma.environmentSpec(cma.environmentIds().get(1)).unrestricted()).isTrue();
    }

    @Test
    void sendingTheSameConfigurationAgainChangesNothing() {
        TestApi acme = app(ACME);
        acme.put("/v1/agents/support", MINIMAL).as(AgentProfileView.class);
        int callsSoFar = cma.calls().size();
        clock.advance(Duration.ofMinutes(5));

        TestApi.Reply again = acme.put("/v1/agents/support", MINIMAL);

        assertThat(again.status()).isEqualTo(200);
        assertThat(again.as(AgentProfileView.class).revision()).isEqualTo(1);
        assertThat(again.as(AgentProfileView.class).updatedAt()).isEqualTo(clock.now().minus(Duration.ofMinutes(5)));
        assertThat(cma.calls()).as("the platform is not called at all").hasSize(callsSoFar);
    }

    @Test
    void aChangeMakesANewRevision_andANewVersionOfThePlatformsAgent_inTheSameEnvironment() {
        TestApi acme = app(ACME);
        acme.put("/v1/agents/support", MINIMAL).as(AgentProfileView.class);
        AgentRef first = cma.agents().getFirst();
        clock.advance(Duration.ofMinutes(5));

        AgentProfileView changed = acme.put("/v1/agents/support",
                new AgentProfileRequest("Support", null, "low", "You help customers. Keep it short.", null, null))
                .as(AgentProfileView.class);

        assertThat(changed.revision()).isEqualTo(2);
        assertThat(changed.createdAt()).isEqualTo(clock.now().minus(Duration.ofMinutes(5)));
        assertThat(changed.updatedAt()).isEqualTo(clock.now());
        assertThat(cma.agents()).containsExactly(new AgentRef(first.id(), 2));
        assertThat(cma.agentSpec(new AgentRef(first.id(), 2)).system()).startsWith("You help customers. Keep it short.");
        assertThat(cma.agentSpec(first).system()).as("the first version is still what it was").startsWith("You help customers with");
        assertThat(cma.environmentIds()).hasSize(1);
        assertThat(acme.get("/v1/agents/support").as(AgentProfileView.class)).isEqualTo(changed);
    }

    @Test
    void agentsAreListedByKey_andBelongToTheirApplication() {
        TestApi acme = app(ACME);
        TestApi globex = app(GLOBEX);
        acme.put("/v1/agents/support", MINIMAL).as(AgentProfileView.class);
        acme.put("/v1/agents/billing", MINIMAL).as(AgentProfileView.class);
        globex.put("/v1/agents/support", new AgentProfileRequest("Globex", null, null, "Other.", null, null)).as(AgentProfileView.class);

        assertThat(acme.get("/v1/agents").as(AgentProfileView[].class)).extracting(AgentProfileView::agentKey)
                .containsExactly("billing", "support");
        assertThat(globex.get("/v1/agents").as(AgentProfileView[].class)).extracting(AgentProfileView::name).containsExactly("Globex");
        assertThat(globex.get("/v1/agents/billing").status()).isEqualTo(404);
        assertThat(globex.get("/v1/agents/billing").code()).isEqualTo(ProblemCodes.NOT_FOUND);
        assertThat(cma.agents()).as("the same key in two applications is two agents").hasSize(3);
        assertThat(cma.agents().stream().map(agent -> cma.agentSpec(agent).metadata().get("app")))
                .containsExactly("acme", "acme", "globex");
    }

    @Test
    void onlyAnApplicationsBackEndManagesAgents() {
        TestApi acme = app(ACME);
        TestApi user = user(acme, "u42");

        assertThat(user.put("/v1/agents/support", MINIMAL).status()).isEqualTo(403);
        assertThat(user.get("/v1/agents/support").status()).isEqualTo(403);
        assertThat(user.get("/v1/agents").status()).isEqualTo(403);
        assertThat(cma.calls()).isEmpty();
    }

    @Test
    void aConfigurationThatMakesNoSenseIsRefusedBeforeThePlatformIsAsked() {
        TestApi acme = app(ACME);
        String instructions = "Do things.";
        SandboxConfig off = null;
        List<Object> wrong = List.of(
                new AgentProfileRequest(null, null, null, instructions, off, null),
                new AgentProfileRequest("A", null, null, " ", off, null),
                new AgentProfileRequest("A", null, null, "x".repeat(50_001), off, null),
                new AgentProfileRequest("A", "not a model", null, instructions, off, null),
                new AgentProfileRequest("A", null, "extreme", instructions, off, null),
                new AgentProfileRequest("A", null, null, instructions, new SandboxConfig(false, true, null, null, null), null),
                new AgentProfileRequest("A", null, null, instructions, new SandboxConfig(true, true, null, "none", null), null),
                new AgentProfileRequest("A", null, null, instructions, new SandboxConfig(true, null, null, "limited", null), null),
                new AgentProfileRequest("A", null, null, instructions, new SandboxConfig(true, null, null, "unrestricted", List.of("a.example.com")), null),
                new AgentProfileRequest("A", null, null, instructions, new SandboxConfig(true, null, null, "limited", List.of("https://a.example.com")), null),
                new AgentProfileRequest("A", null, null, instructions, new SandboxConfig(true, null, null, "limited", List.of("localhost")), null),
                new AgentProfileRequest("A", null, null, instructions, new SandboxConfig(true, null, null, "sometimes", null), null),
                new AgentProfileRequest("A", null, null, instructions, off, List.of(new McpServerConfig("Tickets", "https://m.example.com", null, null))),
                new AgentProfileRequest("A", null, null, instructions, off, List.of(new McpServerConfig("t", "http://m.example.com", null, null))),
                new AgentProfileRequest("A", null, null, instructions, off, List.of(new McpServerConfig("t", "https://user:pw@m.example.com", null, null))),
                new AgentProfileRequest("A", null, null, instructions, off, List.of(new McpServerConfig("t", "https://m.example.com", "sometimes", null))),
                new AgentProfileRequest("A", null, null, instructions, off, List.of(new McpServerConfig("t", "https://m.example.com", null, "nobody"))),
                new AgentProfileRequest("A", null, null, instructions, off, List.of(new McpServerConfig("t", "https://a.example.com", null, null),
                        new McpServerConfig("t", "https://b.example.com", null, null))),
                "{\"name\": \"A\", \"instructions\": \"x\", \"tools\": []}");

        for (Object request : wrong) {
            TestApi.Reply reply = acme.put("/v1/agents/support", request);
            assertThat(reply.status()).as(String.valueOf(request)).isEqualTo(400);
            assertThat(reply.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        }
        assertThat(acme.put("/v1/agents/Support", MINIMAL).status()).as("the key has a shape too").isEqualTo(400);
        assertThat(acme.put("/v1/agents/" + "k".repeat(65), MINIMAL).status()).isEqualTo(400);
        assertThat(count("SELECT count(*) FROM agent_profile")).isZero();
        assertThat(cma.calls()).isEmpty();
    }

    @Test
    void whatThePlatformRefusesIsRefusedHere_andNothingIsSaved() {
        TestApi acme = app(ACME);
        cma.failNext(Operation.CREATE_AGENT, CmaException.invalid("model: claude-opus-9 is not a known model"));

        TestApi.Reply reply = acme.put("/v1/agents/support",
                new AgentProfileRequest("Support", "claude-opus-9", null, "Help.", null, null));

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        assertThat(reply.body().path("detail").asString()).contains("claude-opus-9 is not a known model");
        assertThat(acme.get("/v1/agents/support").status()).isEqualTo(404);

        acme.put("/v1/agents/support", MINIMAL).as(AgentProfileView.class);
        cma.failNext(Operation.UPDATE_AGENT, CmaException.invalid("no"));
        assertThat(acme.put("/v1/agents/support", new AgentProfileRequest("Support", "claude-opus-9", null, "Help.", null, null)).status())
                .isEqualTo(400);
        assertThat(acme.get("/v1/agents/support").as(AgentProfileView.class).model()).as("the agent is as it was").isEqualTo("claude-opus-5-5");
        assertThat(acme.get("/v1/agents/support").as(AgentProfileView.class).revision()).isEqualTo(1);
    }

    @Test
    void whileThePlatformIsDownNothingIsSaved_andTheSameRequestWorksLater() {
        TestApi acme = app(ACME);
        cma.failNext(Operation.CREATE_ENVIRONMENT, CmaException.overloaded());

        TestApi.Reply down = acme.put("/v1/agents/support", MINIMAL);

        assertThat(down.status()).isEqualTo(503);
        assertThat(down.code()).isEqualTo(ProblemCodes.UNAVAILABLE);
        assertThat(acme.get("/v1/agents/support").status()).isEqualTo(404);

        cma.failNext(Operation.CREATE_AGENT, CmaException.rateLimited(Duration.ofSeconds(5)));
        assertThat(acme.put("/v1/agents/support", MINIMAL).status()).isEqualTo(503);

        assertThat(acme.put("/v1/agents/support", MINIMAL).status()).isEqualTo(201);
        assertThat(cma.agents()).hasSize(1);
    }

    @Test
    void anUpdateWhoseAnswerWasLostDoesNotLeaveTheAgentStuck() {
        TestApi acme = app(ACME);
        acme.put("/v1/agents/support", MINIMAL).as(AgentProfileView.class);
        AgentRef first = cma.agents().getFirst();
        AgentProfileRequest changed = new AgentProfileRequest("Support", null, null, "Second instructions.", null, null);
        // the platform takes the update, but the answer never arrives
        cma.failNextAfterEffect(Operation.UPDATE_AGENT, new CmaException(CmaException.Kind.SERVER, "lost"));
        assertThat(acme.put("/v1/agents/support", changed).status()).isEqualTo(503);
        assertThat(cma.agents()).containsExactly(new AgentRef(first.id(), 2));
        assertThat(acme.get("/v1/agents/support").as(AgentProfileView.class).revision()).as("nothing was saved here").isEqualTo(1);

        AgentProfileView saved = acme.put("/v1/agents/support", changed).as(AgentProfileView.class);

        assertThat(saved.revision()).isEqualTo(2);
        assertThat(profiles.ready(ACME, "support").agent()).as("the platform's agent and this service agree again")
                .isEqualTo(cma.agents().getFirst());
        assertThat(cma.agentSpec(cma.agents().getFirst()).system()).startsWith("Second instructions.");
    }

    @Test
    void anAgentIsBroughtUpToDateBeforeItIsUsed_whenThisServiceWouldSetItUpDifferentlyNow() {
        TestApi acme = app(ACME);
        acme.put("/v1/agents/support", MINIMAL).as(AgentProfileView.class);
        AgentProfiles.Ready ready = profiles.ready(ACME, "support");
        assertThat(ready.agent()).isEqualTo(cma.agents().getFirst());
        assertThat(ready.environmentId()).isEqualTo(cma.environmentIds().getFirst());
        assertThat(ready.revision()).isEqualTo(1);
        long updates = cma.count(Operation.UPDATE_AGENT);

        // as after an upgrade of the service that gives agents a new tool
        jdbc.sql("UPDATE agent_profile SET provisioned_hash = 'what-an-older-version-sent'").update();
        profiles.ready(ACME, "support");

        assertThat(cma.count(Operation.UPDATE_AGENT)).isEqualTo(updates + 1);
        assertThat(jdbc.sql("SELECT provisioned_hash FROM agent_profile").query(String.class).single()).hasSize(64);
        profiles.ready(ACME, "support");
        assertThat(cma.count(Operation.UPDATE_AGENT)).as("once is enough").isEqualTo(updates + 1);
    }
}
