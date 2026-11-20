package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class WalkingSkeletonIT extends IntegrationTestBase {

    @Test
    void healthIsUpWithDatabaseAndMigrations() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(baseUri() + "/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void metricsAreExposedForScraping() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(baseUri() + "/actuator/prometheus")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("jvm_");
    }
}
