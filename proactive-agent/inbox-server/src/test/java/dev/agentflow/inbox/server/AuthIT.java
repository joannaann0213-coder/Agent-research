package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.HmacSigner;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Every /v1 request is signed; a failed check stores nothing. */
class AuthIT extends IntegrationTestBase {

    private static final String PATH = "/v1/inbox/u42/events";

    private byte[] body() {
        WriteEventRequest request = webhook("order-1", "{\"n\":1}").build();
        return InboxJson.mapper().writeValueAsBytes(request);
    }

    @Test
    void validSignatureIsAccepted() {
        assertThat(acme.send("POST", PATH, body(), WriteEventResponse.class).status()).isEqualTo(202);
    }

    @Test
    void missingOrWrongCredentialsAre401() {
        long now = clock.now().getEpochSecond();
        String good = HmacSigner.sign("acme-secret", now, "POST", PATH, body());

        assertUnauthenticated(acme.exchange("POST", PATH, body(), WriteEventResponse.class, null, null, null));
        assertUnauthenticated(acme.exchange("POST", PATH, body(), WriteEventResponse.class, "acme-bot", "" + now, null));
        assertUnauthenticated(acme.exchange("POST", PATH, body(), WriteEventResponse.class, "nobody", "" + now, good));
        assertUnauthenticated(acme.exchange("POST", PATH, body(), WriteEventResponse.class, "acme-bot", "soon", good));
        // signed with another client's secret
        String wrongSecret = HmacSigner.sign("globex-secret", now, "POST", PATH, body());
        assertUnauthenticated(acme.exchange("POST", PATH, body(), WriteEventResponse.class, "acme-bot", "" + now, wrongSecret));
        // a signature for another inbox cannot be replayed here
        String otherPath = HmacSigner.sign("acme-secret", now, "POST", "/v1/inbox/u43/events", body());
        assertUnauthenticated(acme.exchange("POST", PATH, body(), WriteEventResponse.class, "acme-bot", "" + now, otherPath));
        // the body is covered by the signature
        assertUnauthenticated(acme.exchange("POST", PATH, "{\"tampered\":true}".getBytes(), WriteEventResponse.class,
                "acme-bot", "" + now, good));

        assertThat(count("SELECT count(*) FROM event")).isZero();
    }

    @Test
    void staleTimestampIs401() {
        InboxClient slowClock = new InboxClient(baseUri(), "acme-bot", "acme-secret",
                () -> clock.now().minusSeconds(301), Duration.ofSeconds(10));
        assertUnauthenticated(slowClock.send("POST", PATH, body(), WriteEventResponse.class));

        InboxClient withinSkew = new InboxClient(baseUri(), "acme-bot", "acme-secret",
                () -> clock.now().minusSeconds(299), Duration.ofSeconds(10));
        assertThat(withinSkew.send("POST", PATH, body(), WriteEventResponse.class).status()).isEqualTo(202);
    }

    @Test
    void wrongRoleIs403() {
        ApiResult<WriteEventResponse> asRuntime = runtime.send("POST", PATH + "?tenant_id=acme", body(), WriteEventResponse.class);
        assertThat(asRuntime.status()).isEqualTo(403);
        assertThat(asRuntime.code()).isEqualTo(ProblemCodes.FORBIDDEN);

        // an operator may read events but not write them
        assertThat(ops.send("POST", PATH + "?tenant_id=acme", body(), WriteEventResponse.class).status()).isEqualTo(403);
        // a producer may not use the operator API
        assertThat(acme.get("/v1/admin/audit", Object.class).status()).isEqualTo(403);
        assertThat(count("SELECT count(*) FROM event")).isZero();
    }

    @Test
    void oversizedBodyIs413BeforeParsing() {
        byte[] huge = new byte[128 * 1024 + 1];
        java.util.Arrays.fill(huge, (byte) 'x');
        assertThat(acme.send("POST", PATH, huge, WriteEventResponse.class).status()).isEqualTo(413);
    }

    private static void assertUnauthenticated(ApiResult<?> result) {
        assertThat(result.status()).isEqualTo(401);
        assertThat(result.code()).isEqualTo(ProblemCodes.UNAUTHENTICATED);
    }
}
