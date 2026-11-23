package dev.agentflow.agent.server.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CmaConfigTest {

    private static AgentProperties properties(String adminKey, String tokenSecret, String inboxSecret) {
        AgentProperties properties = new AgentProperties();
        properties.getAdmin().setKeys(List.of(adminKey));
        properties.getTokens().setSecrets(List.of(tokenSecret));
        properties.getInbox().setSecret(inboxSecret);
        return properties;
    }

    @Test
    void liveModeRefusesTheExampleSecretsOfTheDocumentation() {
        assertThatThrownBy(() -> CmaConfig.refuseExampleSecrets(properties("change-me-admin-key-0123456789abcdef",
                "change-me-token-secret-0123456789abcdef", "change-me-inbox-secret-0123456789abcdef")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AGENT_ADMIN_KEYS_0")
                .hasMessageContaining("AGENT_TOKENS_SECRETS_0")
                .hasMessageContaining("AGENT_INBOX_SECRET");
        assertThatThrownBy(() -> CmaConfig.refuseExampleSecrets(properties("a".repeat(64), "b".repeat(64),
                "change-me-inbox-secret-0123456789abcdef")))
                .hasMessageContaining("AGENT_INBOX_SECRET")
                .hasMessageNotContaining("AGENT_ADMIN_KEYS_0");
    }

    @Test
    void liveModeStartsWithSecretsOfItsOwn() {
        assertThatCode(() -> CmaConfig.refuseExampleSecrets(properties("a".repeat(64), "b".repeat(64), "c".repeat(64))))
                .doesNotThrowAnyException();
    }
}
