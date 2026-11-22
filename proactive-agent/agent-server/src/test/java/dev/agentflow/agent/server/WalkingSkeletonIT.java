package dev.agentflow.agent.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.server.support.AgentTestBase;
import dev.agentflow.agent.server.support.EmbeddedInbox;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/** Both services start in one JVM, each on its own port, database and configuration. */
class WalkingSkeletonIT extends AgentTestBase {

    @Test
    void theAgentServiceIsUpWithItsDatabaseAndMigrations() throws Exception {
        HttpResponse<String> health = get(baseUri().resolve("/actuator/health"));

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("UP");
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isPositive();
    }

    @Test
    void theInboxNextToItIsUp_andEachHasItsOwnTables() throws Exception {
        assertThat(get(EmbeddedInbox.baseUri().resolve("/actuator/health")).body()).contains("UP");

        assertThat(count("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'app'"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'event'"))
                .as("the Inbox's tables are not in the agent service's database").isZero();
        assertThat(EmbeddedInbox.jdbc().sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                + "AND table_name IN ('event', 'app')").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void metricsAreExposedForScraping() throws Exception {
        HttpResponse<String> metrics = get(baseUri().resolve("/actuator/prometheus"));

        assertThat(metrics.statusCode()).isEqualTo(200);
        assertThat(metrics.body()).contains("jvm_");
    }

    private static HttpResponse<String> get(URI uri) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
    }
}
