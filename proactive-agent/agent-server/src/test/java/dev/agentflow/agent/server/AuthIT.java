package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.CreateAppRequest;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.UpdateAppRequest;
import dev.agentflow.agent.api.Whoami;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import org.junit.jupiter.api.Test;

/** Every /v1 request carries a credential, and each kind of credential opens its own part of the API. */
class AuthIT extends AgentTestBase {

    @Test
    void theServiceSaysWhoItTakesTheCallerFor() {
        TestApi acme = app(ACME);

        assertThat(acme.get("/v1/whoami").as(Whoami.class)).isEqualTo(new Whoami("app", ACME, null));
        assertThat(user(acme, "u42").get("/v1/whoami").as(Whoami.class)).isEqualTo(new Whoami("user", ACME, "u42"));
    }

    @Test
    void aRequestWithoutAValidCredentialIs401() {
        app(ACME);

        for (TestApi caller : new TestApi[] {anonymous, anonymous.as("nonsense"), anonymous.as("afk.key_unknown.secret"),
                anonymous.as("afk.missing-the-secret"), anonymous.as("aft.bm90IGEgdG9rZW4.c2lnbmF0dXJl"),
                anonymous.as("aft.no-signature"), anonymous.as("aft.!!!.???")}) {
            TestApi.Reply reply = caller.get("/v1/whoami");
            assertThat(reply.status()).isEqualTo(401);
            assertThat(reply.code()).isEqualTo(ProblemCodes.UNAUTHENTICATED);
            assertThat(reply.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
        }
        assertThat(anonymous.post("/v1/admin/apps", new CreateAppRequest("evil", "Evil")).status()).isEqualTo(401);
    }

    @Test
    void anApiKeyWithAWrongSecretIs401_alsoWhenItsIdIsReal() {
        String key = newKeyOfANewApp();
        String keyId = key.split("\\.")[1];

        assertThat(anonymous.as("afk." + keyId + ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").get("/v1/whoami").status())
                .isEqualTo(401);
        assertThat(anonymous.as(key).get("/v1/whoami").status()).isEqualTo(200);
    }

    @Test
    void theAdministrationApiTakesOnlyAnAdministrationKey_andThatKeyOpensNothingElse() {
        TestApi acme = app(ACME);

        assertThat(acme.get("/v1/admin/apps").status()).isEqualTo(403);
        assertThat(acme.get("/v1/admin/apps").code()).isEqualTo(ProblemCodes.FORBIDDEN);
        assertThat(user(acme, "u42").get("/v1/admin/apps").status()).isEqualTo(403);
        assertThat(admin.get("/v1/admin/apps").status()).isEqualTo(200);

        assertThat(admin.get("/v1/whoami").status()).isEqualTo(403);
        assertThat(admin.post("/v1/users/u42/tokens", null).status()).isEqualTo(403);
    }

    @Test
    void aRevokedKeyStopsWorkingAtOnce_theOtherKeysOfTheApplicationGoOn() {
        TestApi acme = app(ACME);
        String second = newKey(ACME);
        String secondId = second.split("\\.")[1];
        assertThat(anonymous.as(second).get("/v1/whoami").status()).isEqualTo(200);

        assertThat(admin.delete("/v1/admin/apps/acme/keys/" + secondId).status()).isEqualTo(204);

        assertThat(anonymous.as(second).get("/v1/whoami").status()).isEqualTo(401);
        assertThat(acme.get("/v1/whoami").status()).isEqualTo(200);
    }

    @Test
    void aSuspendedApplicationIsRefused_withItsKeysAndItsUsersTokens_untilItIsActiveAgain() {
        TestApi acme = app(ACME);
        TestApi globex = app(GLOBEX);
        TestApi user = user(acme, "u42");

        admin.patch("/v1/admin/apps/acme", new UpdateAppRequest(null, "suspended")).as(Object.class);

        assertThat(acme.get("/v1/whoami").status()).isEqualTo(403);
        assertThat(acme.get("/v1/whoami").code()).isEqualTo(ProblemCodes.FORBIDDEN);
        assertThat(user.get("/v1/whoami").status()).isEqualTo(403);
        assertThat(globex.get("/v1/whoami").status()).as("another application is not affected").isEqualTo(200);

        admin.patch("/v1/admin/apps/acme", new UpdateAppRequest(null, "active")).as(Object.class);
        assertThat(acme.get("/v1/whoami").status()).isEqualTo(200);
        assertThat(user.get("/v1/whoami").status()).isEqualTo(200);
    }

    @Test
    void onlyHashesOfApiKeysAreStored() {
        String key = newKeyOfANewApp();
        String secret = key.substring(key.lastIndexOf('.') + 1);

        assertThat(count("SELECT count(*) FROM api_key WHERE length(secret_hash) = 32")).isEqualTo(1);
        // encode(..., 'escape') turns any byte into text, also the NUL that a random hash may hold.
        assertThat(jdbc.sql("SELECT count(*) FROM api_key WHERE encode(secret_hash, 'escape') LIKE :secret OR label LIKE :secret")
                .param("secret", "%" + secret + "%").query(Long.class).single()).isZero();
    }

    private String newKeyOfANewApp() {
        admin.post("/v1/admin/apps", new CreateAppRequest(ACME, "Acme")).as(Object.class);
        return newKey(ACME);
    }
}
