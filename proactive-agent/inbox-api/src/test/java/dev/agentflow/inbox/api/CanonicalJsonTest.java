package dev.agentflow.inbox.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CanonicalJsonTest {

    private static String canonical(String json) {
        return CanonicalJson.of(InboxJson.mapper().readTree(json));
    }

    @Test
    void keyOrderAndWhitespaceDoNotMatter() {
        assertThat(canonical("{ \"b\": 1, \"a\": { \"z\": [1, 2], \"y\": null } }"))
                .isEqualTo(canonical("{\"a\":{\"y\":null,\"z\":[1,2]},\"b\":1}"))
                .isEqualTo("{\"a\":{\"y\":null,\"z\":[1,2]},\"b\":1}");
    }

    @Test
    void arrayOrderMatters() {
        assertThat(canonical("[1,2]")).isNotEqualTo(canonical("[2,1]"));
    }

    @Test
    void stringsAreEscaped() {
        assertThat(canonical("{\"t\":\"a\\\"b\\n\\u0001中文\"}")).isEqualTo("{\"t\":\"a\\\"b\\n\\u0001中文\"}");
    }

    @Test
    void numbersAreNormalisedAndIntegersStayDistinctFromDecimals() {
        assertThat(canonical("{\"n\":299,\"f\":1.50,\"t\":true}")).isEqualTo("{\"f\":1.5,\"n\":299,\"t\":true}");
        assertThat(canonical("{\"n\":1}")).isNotEqualTo(canonical("{\"n\":1.0}"));
    }
}
