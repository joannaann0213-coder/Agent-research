package dev.agentflow.agent.server;

import static dev.agentflow.agent.cma.fake.ModelStep.mcp;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.McpCredentialPage;
import dev.agentflow.agent.api.McpCredentialRequest;
import dev.agentflow.agent.api.McpCredentialView;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.OAuthTokens;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.fake.FakeManagedAgents.Operation;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The agent reaches third-party systems with credentials that the application, or each user, gives
 * it. The secrets go to the agent platform's vaults and nowhere else; sessions take on new vaults,
 * and a newly configured agent, at the next turn of their line.
 */
class CredentialsIT extends AgentTestBase {

    private static final String TICKETS = "https://mcp.acme.example/tickets";

    private TestApi acme;

    @BeforeEach
    void anAgentWithATicketSystem() {
        acme = app(ACME);
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null,
                List.of(new McpServerConfig("tickets", TICKETS, "always_allow", "app")))).as(AgentProfileView.class);
        cma.brain(context -> context.userMessage().contains("tickets") && context.step() == 0
                ? ModelStep.of(mcp("tickets", "list_tickets", "{}")) : say("Done: " + context.userMessage()));
    }

    @Test
    void theApplicationsCredentialGoesWithEveryCallOfItsServer_andIsNeverShown() {
        McpCredentialView stored = acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "app-secret-token", null))
                .as(McpCredentialView.class);

        assertThat(stored.name()).isEqualTo("tickets");
        assertThat(stored.kind()).isEqualTo("bearer");
        assertThat(stored.userId()).isNull();
        userSays(acme, "u42", "my tickets?");
        userSays(acme, "u43", "my tickets?");
        settle();

        assertThat(cma.executions()).extracting(FakeManagedAgents.Execution::credential).containsExactly("app-secret-token", "app-secret-token");
        TestApi.Reply listed = acme.get("/v1/credentials");
        assertThat(listed.as(McpCredentialPage.class).credentials()).containsExactly(stored);
        assertThat(listed.body().toString()).doesNotContain("app-secret-token");
        assertThat(count("SELECT count(*) FROM mcp_credential")).isEqualTo(1);
        assertThat(jdbc.sql("SELECT row_to_json(c)::text FROM mcp_credential c").query(String.class).single())
                .as("the service keeps no secret").doesNotContain("app-secret-token");
    }

    @Test
    void aUsersOwnCredentialComesFirst_forThatUserOnly_fromTheirNextTurnOn() {
        acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "app-token", null)).as(McpCredentialView.class);
        userSays(acme, "u42", "my tickets?");
        settle();

        TestApi alice = user(acme, "u42");
        McpCredentialView own = alice.put("/v1/users/u42/credentials/my-tickets", new McpCredentialRequest(TICKETS + "/", "u42-token", null))
                .as(McpCredentialView.class);
        assertThat(own.userId()).isEqualTo("u42");
        userSays(acme, "u42", "my tickets again?");
        userSays(acme, "u43", "my tickets?");
        settle();

        assertThat(cma.executions()).extracting(FakeManagedAgents.Execution::credential)
                .containsExactly("app-token", "u42-token", "app-token");
        assertThat(cma.sessionIds()).as("u42's conversation went on in a session that has the user's vault").hasSize(3);
        assertThat(cma.userMessages(cma.sessionIds().get(1)).getFirst()).as("and was told what was said before")
                .contains("User: my tickets?");
        assertThat(alice.get("/v1/users/u42/credentials").as(McpCredentialPage.class).credentials()).containsExactly(own);
        assertThat(user(acme, "u43").get("/v1/users/u42/credentials").status()).isEqualTo(403);
        assertThat(alice.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "x", null)).status())
                .as("the application's credentials are its back end's business").isEqualTo(403);
    }

    @Test
    void aUsersCredentialsFollowTheSameRules_andTheUserCanRemoveThem() {
        TestApi alice = user(acme, "u42");
        cma.failNext(Operation.CREATE_VAULT, CmaException.overloaded());
        assertThat(alice.put("/v1/users/u42/credentials/mine", new McpCredentialRequest(TICKETS, "t", null)).status()).isEqualTo(503);

        alice.put("/v1/users/u42/credentials/mine", new McpCredentialRequest(TICKETS, "t", null)).as(McpCredentialView.class);
        TestApi.Reply twice = alice.put("/v1/users/u42/credentials/mine-too", new McpCredentialRequest(TICKETS, "t2", null));
        assertThat(twice.status()).isEqualTo(409);
        assertThat(acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "app", null)).status())
                .as("the application's vault is another one").isEqualTo(200);

        assertThat(alice.delete("/v1/users/u42/credentials/mine").status()).isEqualTo(204);
        assertThat(alice.get("/v1/users/u42/credentials").as(McpCredentialPage.class).credentials()).isEmpty();
        assertThat(user(acme, "u43").delete("/v1/users/u42/credentials/mine").status()).isEqualTo(403);
        assertThat(acme.delete("/v1/users/bad%20user/credentials/mine").status()).isEqualTo(400);
    }

    @Test
    void aReplacedTokenIsUsedFromTheNextCallOn_withoutANewSession() {
        acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "first", null)).as(McpCredentialView.class);
        userSays(acme, "u42", "my tickets?");
        settle();

        acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "second", null)).as(McpCredentialView.class);
        userSays(acme, "u42", "my tickets now?");
        settle();

        assertThat(cma.executions()).extracting(FakeManagedAgents.Execution::credential).containsExactly("first", "second");
        assertThat(cma.sessionIds()).hasSize(1);
        assertThat(cma.count(Operation.CREATE_CREDENTIAL)).isEqualTo(1);
        assertThat(cma.count(Operation.UPDATE_CREDENTIAL)).isEqualTo(1);
    }

    @Test
    void anOAuthCredentialReplacesABearerToken_andADeletedOneIsGone() {
        acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "bearer", null)).as(McpCredentialView.class);

        McpCredentialView oauth = acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, null,
                new OAuthTokens("access-1", Instant.parse("2026-11-21T10:00:00Z"), "refresh-1", "acme-agent",
                        "https://auth.acme.example/token", "client_secret_post", "client-secret", "tickets.read")))
                .as(McpCredentialView.class);
        userSays(acme, "u42", "my tickets?");
        settle();

        assertThat(oauth.kind()).isEqualTo("oauth");
        assertThat(cma.executions()).extracting(FakeManagedAgents.Execution::credential).containsExactly("access-1");
        String vault = cma.vaultIds().getFirst();
        assertThat(cma.credentialsIn(vault)).extracting(FakeManagedAgents.StoredCredential::archived).containsExactly(true, false);

        assertThat(acme.delete("/v1/credentials/tickets").status()).isEqualTo(204);
        assertThat(acme.delete("/v1/credentials/tickets").status()).as("deleting again changes nothing").isEqualTo(204);
        userSays(acme, "u42", "my tickets now?");
        settle();

        assertThat(cma.executions()).extracting(FakeManagedAgents.Execution::credential).containsExactly("access-1", null);
        assertThat(acme.get("/v1/credentials").as(McpCredentialPage.class).credentials()).isEmpty();
    }

    @Test
    void aServerHasOneCredentialPerVault() {
        acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "one", null)).as(McpCredentialView.class);

        TestApi.Reply second = acme.put("/v1/credentials/tickets-again", new McpCredentialRequest("https://MCP.acme.example:443/tickets/",
                "two", null));

        assertThat(second.status()).isEqualTo(409);
        assertThat(second.code()).isEqualTo(ProblemCodes.CONFLICT);
        assertThat(second.body().path("detail").asString()).contains("credential tickets is for this server already");
    }

    @Test
    void aCredentialMustBeWhole() {
        assertThat(put(new McpCredentialRequest("http://mcp.acme.example/tickets", "t", null))).isEqualTo(400);
        assertThat(put(new McpCredentialRequest(TICKETS, null, null))).isEqualTo(400);
        assertThat(put(new McpCredentialRequest(TICKETS, "t", new OAuthTokens("a", null, null, null, null, null, null, null)))).isEqualTo(400);
        assertThat(put(new McpCredentialRequest(TICKETS, " ", null))).isEqualTo(400);
        assertThat(put(new McpCredentialRequest(TICKETS, null, new OAuthTokens("a", null, "r", null, null, null, null, null))))
                .as("a refresh token needs the client and the endpoint").isEqualTo(400);
        assertThat(put(new McpCredentialRequest(TICKETS, null, new OAuthTokens("a", null, "r", "c", "https://auth.example/token", "none",
                "secret", null)))).as("no secret with none").isEqualTo(400);
        assertThat(put(new McpCredentialRequest(TICKETS, null, new OAuthTokens("a", null, "r", "c", "https://auth.example/token",
                "client_secret_jwt", "secret", null)))).isEqualTo(400);
        assertThat(acme.put("/v1/credentials/bad%20name", new McpCredentialRequest(TICKETS, "t", null)).status()).isEqualTo(400);
        assertThat(acme.put("/v1/credentials/tickets", "{\"mcp_server_url\": \"" + TICKETS + "\", \"token\": \"t\", \"scope\": \"x\"}")
                .status()).isEqualTo(400);
        assertThat(count("SELECT count(*) FROM mcp_credential")).isZero();
    }

    @Test
    void whenThePlatformIsNotThere_nothingIsStored() {
        cma.failNext(Operation.CREATE_VAULT, CmaException.overloaded());

        TestApi.Reply reply = acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "t", null));

        assertThat(reply.status()).isEqualTo(503);
        assertThat(count("SELECT count(*) FROM mcp_credential")).isZero();
        assertThat(count("SELECT count(*) FROM vault")).isZero();
        assertThat(acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "t", null)).status()).isEqualTo(200);
    }

    @Test
    void aCredentialWhoseFirstStoreLostItsAnswerIsStoredOnce() {
        acme.put("/v1/credentials/other", new McpCredentialRequest("https://other.example/mcp", "o", null)).as(McpCredentialView.class);
        cma.failNextAfterEffect(Operation.CREATE_CREDENTIAL, CmaException.connection("connection reset", null));
        assertThat(acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "lost", null)).status()).isEqualTo(503);

        assertThat(acme.put("/v1/credentials/tickets", new McpCredentialRequest(TICKETS, "kept", null)).status()).isEqualTo(200);

        String vault = cma.vaultIds().getFirst();
        assertThat(cma.credentialsIn(vault)).filteredOn(stored -> !stored.archived())
                .extracting(FakeManagedAgents.StoredCredential::token).containsExactlyInAnyOrder("o", "kept");
    }

    @Test
    void aNewlyConfiguredAgentTakesOverAConversationAtItsNextTurn() {
        userSays(acme, "u42", "hello");
        settle();
        cma.brain(context -> say("I am " + context.agent().description() + ", told: " + context.userMessage().contains("User: hello")));

        acme.put("/v1/agents/default", new AgentProfileRequest("Helper", null, null, "You help better.", null,
                List.of(new McpServerConfig("tickets", TICKETS, "always_allow", "app")))).as(AgentProfileView.class);
        userSays(acme, "u42", "who are you?");
        settle();

        assertThat(cma.sessionIds()).hasSize(2);
        assertThat(repliesTo(acme, "u42").getLast()).isEqualTo("I am Helper, told: true");
        assertThat(count("SELECT count(*) FROM thread WHERE generation = 2")).isEqualTo(1);
    }

    @Test
    void aSessionCanHaveABudget_andOneThatIsSpentIsReplaced() {
        acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null, List.of(),
                new BigDecimal("2.5"))).as(AgentProfileView.class);
        AtomicBoolean spent = new AtomicBoolean();
        cma.brain(context -> spent.getAndSet(true) ? say("Fresh: " + context.userMessage().lines().reduce((a, b) -> b).orElse(""))
                : ModelStep.stop(StopReason.BUDGET_REACHED));

        userSays(acme, "u42", "hello");
        settle();

        assertThat(acme.get("/v1/agents/default").as(AgentProfileView.class).sessionBudgetUsd()).isEqualByComparingTo("2.50");
        assertThat(cma.sessionIds()).hasSize(2);
        assertThat(cma.sessionSpec(cma.sessionIds().getFirst()).budgetUsd()).isEqualByComparingTo("2.50");
        assertThat(repliesTo(acme, "u42")).containsExactly("Fresh: hello");
        assertThat(acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null, List.of(),
                new BigDecimal("0"))).status()).isEqualTo(400);
        assertThat(acme.put("/v1/agents/default", new AgentProfileRequest("Assistant", null, null, "You help.", null, List.of(),
                new BigDecimal("1.234"))).status()).isEqualTo(400);
    }

    private int put(McpCredentialRequest request) {
        return acme.put("/v1/credentials/tickets", request).status();
    }
}
