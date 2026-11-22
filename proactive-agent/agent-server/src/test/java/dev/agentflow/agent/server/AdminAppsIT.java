package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.api.AppView;
import dev.agentflow.agent.api.CreateAppRequest;
import dev.agentflow.agent.api.CreateKeyRequest;
import dev.agentflow.agent.api.CreatedKey;
import dev.agentflow.agent.api.KeyView;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.api.UpdateAppRequest;
import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.TestApi;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The operator registers applications and hands out their API keys. */
class AdminAppsIT extends AgentTestBase {

    @Test
    void anApplicationIsRegisteredListedReadAndRenamed() {
        AppView created = admin.post("/v1/admin/apps", new CreateAppRequest("acme", "Acme Support")).as(AppView.class);

        assertThat(created).isEqualTo(new AppView("acme", "Acme Support", "active", clock.now()));
        admin.post("/v1/admin/apps", new CreateAppRequest("globex", "Globex")).as(AppView.class);
        assertThat(admin.get("/v1/admin/apps").as(AppView[].class)).extracting(AppView::appId).containsExactly("acme", "globex");
        assertThat(admin.get("/v1/admin/apps/acme").as(AppView.class)).isEqualTo(created);

        AppView renamed = admin.patch("/v1/admin/apps/acme", new UpdateAppRequest("Acme Help Desk", null)).as(AppView.class);
        assertThat(renamed.name()).isEqualTo("Acme Help Desk");
        assertThat(renamed.status()).as("a field that is absent stays as it is").isEqualTo("active");
    }

    @Test
    void anIdIsTakenOnce() {
        admin.post("/v1/admin/apps", new CreateAppRequest("acme", "Acme")).as(AppView.class);

        TestApi.Reply again = admin.post("/v1/admin/apps", new CreateAppRequest("acme", "Another Acme"));

        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo(ProblemCodes.CONFLICT);
        assertThat(admin.get("/v1/admin/apps/acme").as(AppView.class).name()).isEqualTo("Acme");
    }

    @Test
    void invalidRequestsAreRefusedWithoutStoringAnything() {
        for (String id : new String[] {"A", "Acme", "-acme", "a", "acme!", "x".repeat(41)}) {
            assertThat(admin.post("/v1/admin/apps", new CreateAppRequest(id, "Name")).status()).as("id %s", id).isEqualTo(400);
        }
        assertThat(admin.post("/v1/admin/apps", new CreateAppRequest("acme", " ")).status()).isEqualTo(400);
        assertThat(admin.post("/v1/admin/apps", "{\"app_id\": \"acme\"}").status()).isEqualTo(400);
        TestApi.Reply typo = admin.post("/v1/admin/apps", "{\"app_id\": \"acme\", \"name\": \"Acme\", \"nmae\": \"x\"}");
        assertThat(typo.status()).as("a misspelt field is an error, not ignored").isEqualTo(400);
        assertThat(typo.code()).isEqualTo(ProblemCodes.INVALID_REQUEST);
        assertThat(admin.post("/v1/admin/apps", "{not json").status()).isEqualTo(400);
        assertThat(count("SELECT count(*) FROM app")).isZero();

        admin.post("/v1/admin/apps", new CreateAppRequest("acme", "Acme")).as(AppView.class);
        assertThat(admin.patch("/v1/admin/apps/acme", new UpdateAppRequest(null, null)).status()).isEqualTo(400);
        assertThat(admin.patch("/v1/admin/apps/acme", new UpdateAppRequest(null, "deleted")).status()).isEqualTo(400);
        assertThat(admin.post("/v1/admin/apps/acme/keys", new CreateKeyRequest("x".repeat(201))).status()).isEqualTo(400);
    }

    @Test
    void whatDoesNotExistIs404() {
        assertThat(admin.get("/v1/admin/apps/nope").status()).isEqualTo(404);
        assertThat(admin.get("/v1/admin/apps/nope").code()).isEqualTo(ProblemCodes.NOT_FOUND);
        assertThat(admin.patch("/v1/admin/apps/nope", new UpdateAppRequest("Name", null)).status()).isEqualTo(404);
        assertThat(admin.post("/v1/admin/apps/nope/keys", new CreateKeyRequest(null)).status()).isEqualTo(404);
        assertThat(admin.get("/v1/admin/apps/nope/keys").status()).isEqualTo(404);

        admin.post("/v1/admin/apps", new CreateAppRequest("acme", "Acme")).as(AppView.class);
        assertThat(admin.delete("/v1/admin/apps/acme/keys/key_nope").status()).isEqualTo(404);
    }

    @Test
    void aKeyIsShownOnce_listedWithoutItsSecret_andRevokedForGood() {
        admin.post("/v1/admin/apps", new CreateAppRequest("acme", "Acme")).as(AppView.class);
        admin.post("/v1/admin/apps", new CreateAppRequest("globex", "Globex")).as(AppView.class);

        CreatedKey created = admin.post("/v1/admin/apps/acme/keys", new CreateKeyRequest("production")).as(CreatedKey.class);
        clock.advance(Duration.ofSeconds(1));
        CreatedKey second = admin.post("/v1/admin/apps/acme/keys", new CreateKeyRequest(null)).as(CreatedKey.class);

        assertThat(created.apiKey()).startsWith("afk." + created.keyId() + ".").hasSizeGreaterThan(70);
        assertThat(second.apiKey()).isNotEqualTo(created.apiKey());
        TestApi.Reply listed = admin.get("/v1/admin/apps/acme/keys");
        assertThat(listed.body().toString()).doesNotContain(created.apiKey().substring(created.apiKey().lastIndexOf('.') + 1));
        KeyView[] keys = listed.as(KeyView[].class);
        assertThat(keys).extracting(KeyView::keyId).containsExactly(created.keyId(), second.keyId());
        assertThat(keys[0].label()).isEqualTo("production");
        assertThat(keys[0].lastUsedAt()).as("never used yet").isNull();
        assertThat(admin.get("/v1/admin/apps/globex/keys").as(KeyView[].class)).isEmpty();

        assertThat(admin.delete("/v1/admin/apps/globex/keys/" + created.keyId()).status())
                .as("a key is revoked through its own application only").isEqualTo(404);
        assertThat(admin.delete("/v1/admin/apps/acme/keys/" + created.keyId()).status()).isEqualTo(204);
        clock.advance(Duration.ofMinutes(5));
        assertThat(admin.delete("/v1/admin/apps/acme/keys/" + created.keyId()).status()).as("twice is fine").isEqualTo(204);
        KeyView revoked = admin.get("/v1/admin/apps/acme/keys").as(KeyView[].class)[0];
        assertThat(revoked.revokedAt()).as("the time of the first revocation").isEqualTo(clock.now().minus(Duration.ofMinutes(5)));
    }

    @Test
    void theLastUseOfAKeyIsRecordedToTheMinute() {
        TestApi acme = app(ACME);
        String keyId = admin.get("/v1/admin/apps/acme/keys").as(KeyView[].class)[0].keyId();

        acme.get("/v1/whoami");
        java.time.Instant first = clock.now();
        clock.advance(Duration.ofSeconds(20));
        acme.get("/v1/whoami");
        assertThat(admin.get("/v1/admin/apps/acme/keys").as(KeyView[].class)[0].lastUsedAt())
                .as("a second use within the minute writes nothing").isEqualTo(first);

        clock.advance(Duration.ofSeconds(50));
        acme.get("/v1/whoami");
        assertThat(admin.get("/v1/admin/apps/acme/keys").as(KeyView[].class)[0].lastUsedAt()).isEqualTo(clock.now());
        assertThat(keyId).startsWith("key_");
    }
}
