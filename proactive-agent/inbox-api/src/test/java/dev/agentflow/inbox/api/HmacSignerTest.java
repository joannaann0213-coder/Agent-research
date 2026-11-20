package dev.agentflow.inbox.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HmacSignerTest {

    private static final byte[] BODY = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);

    @Test
    void signatureVerifiesAndIsBoundToEveryPart() {
        String sig = HmacSigner.sign("secret", 1_800_000_000L, "post", "/v1/inbox/u1/events", BODY);
        assertThat(sig).startsWith("hmac-sha256=").hasSize("hmac-sha256=".length() + 64);

        assertThat(HmacSigner.verify("secret", sig, 1_800_000_000L, "POST", "/v1/inbox/u1/events", BODY)).isTrue();
        assertThat(HmacSigner.verify("other", sig, 1_800_000_000L, "POST", "/v1/inbox/u1/events", BODY)).isFalse();
        assertThat(HmacSigner.verify("secret", sig, 1_800_000_001L, "POST", "/v1/inbox/u1/events", BODY)).isFalse();
        assertThat(HmacSigner.verify("secret", sig, 1_800_000_000L, "GET", "/v1/inbox/u1/events", BODY)).isFalse();
        assertThat(HmacSigner.verify("secret", sig, 1_800_000_000L, "POST", "/v1/inbox/u2/events", BODY)).isFalse();
        assertThat(HmacSigner.verify("secret", sig, 1_800_000_000L, "POST", "/v1/inbox/u1/events",
                "{\"a\":2}".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(HmacSigner.verify("secret", null, 1_800_000_000L, "POST", "/v1/inbox/u1/events", BODY)).isFalse();
    }

    @Test
    void emptyBodyIsSigned() {
        String sig = HmacSigner.sign("secret", 1L, "GET", "/v1/x?a=1", null);
        assertThat(HmacSigner.verify("secret", sig, 1L, "GET", "/v1/x?a=1", new byte[0])).isTrue();
    }
}
