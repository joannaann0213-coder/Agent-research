package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.IssueTokenRequest;
import dev.agentflow.agent.api.IssuedToken;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.Whoami;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** An application's back end gets tokens for its users; a token is good for its user, for a while. */
class UserTokensIT extends AgentTestBase {

    @Test
    void aTokenIsGoodForItsUserUntilItExpires() {
        TestApi acme = app(ACME);

        IssuedToken token = acme.post("/v1/users/u42/tokens", null).as(IssuedToken.class);

        assertThat(token.token()).startsWith("aft.");
        assertThat(token.expiresAt()).as("15 minutes when the request does not say").isEqualTo(clock.now().plus(Duration.ofMinutes(15)));
        TestApi user = anonymous.as(token.token());
        assertThat(user.get("/v1/whoami").as(Whoami.class)).isEqualTo(new Whoami("user", ACME, "u42"));

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));
        assertThat(user.get("/v1/whoami").status()).isEqualTo(200);
        clock.advance(Duration.ofSeconds(1));
        TestApi.Reply expired = user.get("/v1/whoami");
        assertThat(expired.status()).isEqualTo(401);
        assertThat(expired.code()).isEqualTo(ProblemCodes.UNAUTHENTICATED);
    }

    @Test
    void theLifetimeCanBeChosen_upToTheMaximum() {
        TestApi acme = app(ACME);

        assertThat(acme.post("/v1/users/u42/tokens", new IssueTokenRequest(60L)).as(IssuedToken.class).expiresAt())
                .isEqualTo(clock.now().plusSeconds(60));
        assertThat(acme.post("/v1/users/u42/tokens", new IssueTokenRequest(3600L)).status()).isEqualTo(201);
        for (long ttl : new long[] {3601, 0, -5}) {
            TestApi.Reply refused = acme.post("/v1/users/u42/tokens", new IssueTokenRequest(ttl));
            assertThat(refused.status()).as("ttl %d", ttl).isEqualTo(400);
            assertThat(refused.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        }
        assertThat(acme.post("/v1/users/u42/tokens", "{\"ttl\": 60}").status()).as("a misspelt field").isEqualTo(400);
    }

    @Test
    void aUserIdMustHaveTheShapeOfAnInboxId() {
        TestApi acme = app(ACME);

        assertThat(acme.post("/v1/users/bad%20id/tokens", null).status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/" + "x".repeat(129) + "/tokens", null).status()).isEqualTo(400);
        assertThat(acme.delete("/v1/users/bad%20id/tokens").status()).isEqualTo(400);
        assertThat(acme.post("/v1/users/user_1.a:b@example.com/tokens", null).status()).isEqualTo(201);
        assertThat(count("SELECT count(*) FROM app_user")).isEqualTo(1);
    }

    @Test
    void onlyTheBackEndIssuesAndRevokesTokens() {
        TestApi acme = app(ACME);
        TestApi user = user(acme, "u42");

        TestApi.Reply selfService = user.post("/v1/users/u42/tokens", null);
        assertThat(selfService.status()).isEqualTo(403);
        assertThat(selfService.code()).isEqualTo(ProblemCodes.FORBIDDEN);
        assertThat(user.post("/v1/users/u43/tokens", null).status()).isEqualTo(403);
        assertThat(user.delete("/v1/users/u42/tokens").status()).isEqualTo(403);
    }

    @Test
    void revokingEndsEveryTokenOfThatUserThatIsOut_andNoOtherUsers() {
        TestApi acme = app(ACME);
        TestApi first = user(acme, "u42");
        TestApi second = user(acme, "u42");
        TestApi other = user(acme, "u43");

        assertThat(acme.delete("/v1/users/u42/tokens").status()).isEqualTo(204);

        assertThat(first.get("/v1/whoami").status()).isEqualTo(401);
        assertThat(second.get("/v1/whoami").status()).isEqualTo(401);
        assertThat(other.get("/v1/whoami").status()).isEqualTo(200);
        assertThat(user(acme, "u42").get("/v1/whoami").status()).as("a token issued afterwards works").isEqualTo(200);
        assertThat(acme.delete("/v1/users/never-seen/tokens").status()).as("a user nobody mentioned has no tokens").isEqualTo(204);
    }

    @Test
    void aTokenCannotBeAlteredOrMovedToAnotherApplication() {
        TestApi acme = app(ACME);
        app(GLOBEX);
        String token = acme.post("/v1/users/u42/tokens", null).as(IssuedToken.class).token();
        String[] parts = token.split("\\.");
        String claims = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertThat(claims).startsWith("v1|acme|u42|");

        for (String forged : new String[] {claims.replace("|u42|", "|u43|"), claims.replace("|acme|", "|globex|"),
                claims.replaceFirst("\\|(\\d{10})\\|", "|9999999999|")}) {
            String altered = "aft." + Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes()) + "." + parts[2];
            assertThat(anonymous.as(altered).get("/v1/whoami").status()).as(forged).isEqualTo(401);
        }
        assertThat(anonymous.as(token).get("/v1/whoami").status()).isEqualTo(200);
    }

    @Test
    void twoApplicationsMayUseTheSameUserId() {
        TestApi acme = app(ACME);
        TestApi globex = app(GLOBEX);

        assertThat(user(acme, "u42").get("/v1/whoami").as(Whoami.class).appId()).isEqualTo(ACME);
        assertThat(user(globex, "u42").get("/v1/whoami").as(Whoami.class).appId()).isEqualTo(GLOBEX);

        acme.delete("/v1/users/u42/tokens");
        assertThat(user(globex, "u42").get("/v1/whoami").status()).isEqualTo(200);
        assertThat(count("SELECT count(*) FROM app_user WHERE user_id = 'u42'")).isEqualTo(2);
    }
}
