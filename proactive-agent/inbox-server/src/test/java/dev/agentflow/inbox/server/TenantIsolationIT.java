package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.EventDetail;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/** Invariant I8: tenants cannot see or collide with each other. Every key starts with the tenant. */
class TenantIsolationIT extends IntegrationTestBase {

    @Test
    void sameInboxIdAndKeyInTwoTenantsAreIndependent() {
        WriteEventResponse a = acme.writeEvent("u42", webhook("order-1", "{\"who\":\"acme\"}").build()).require();
        // Another tenant uses the same inbox id and the same idempotency key with a different payload.
        WriteEventResponse g = globex.writeEvent("u42", webhook("order-1", "{\"who\":\"globex\"}").build()).require();

        assertThat(g.deduped()).isFalse();
        assertThat(g.eventId()).isNotEqualTo(a.eventId());
        assertThat(count("SELECT count(*) FROM inbox WHERE inbox_id = 'u42'")).isEqualTo(2);
        // each tenant has its own sequence
        assertThat(acme.getEvent("u42", a.eventId()).require().event().seq()).isEqualTo(1);
        assertThat(globex.getEvent("u42", g.eventId()).require().event().seq()).isEqualTo(1);
    }

    @Test
    void anotherTenantsEventIsSimplyNotFound() {
        String eventId = acme.writeEvent("u42", webhook("order-1", "{}").build()).require().eventId();

        ApiResult<EventDetail> crossTenant = globex.getEvent("u42", eventId);
        assertThat(crossTenant.status()).isEqualTo(404);
        assertThat(crossTenant.code()).isEqualTo(ProblemCodes.NOT_FOUND);

        // a tenant client cannot address another tenant by naming it
        assertThat(globex.getEvent(ACME, "u42", eventId).status()).isEqualTo(403);
    }

    @Test
    void aProducerSeesItsOwnEvents_anOperatorSeesTheTenant() {
        String eventId = acme.writeEvent("u42", webhook("order-1", "{}").build()).require().eventId();

        assertThat(acme.getEvent("u42", eventId).status()).isEqualTo(200);
        assertThat(acme2.getEvent("u42", eventId).status()).as("a colleague producer of the same tenant").isEqualTo(404);
        assertThat(ops.getEvent(ACME, "u42", eventId).require().event().clientId()).isEqualTo("acme-bot");
        assertThat(ops.getEvent(GLOBEX, "u42", eventId).status()).isEqualTo(404);
        // a platform-scope caller must say which tenant it means
        assertThat(ops.getEvent("u42", eventId).status()).isEqualTo(400);
    }
}
