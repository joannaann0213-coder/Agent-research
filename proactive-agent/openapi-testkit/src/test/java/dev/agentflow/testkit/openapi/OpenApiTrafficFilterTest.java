package dev.agentflow.testkit.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OpenApiTrafficFilterTest {

    private OpenApiTraffic traffic;
    private OpenApiTrafficFilter filter;

    @BeforeEach
    void load() {
        traffic = OpenApiTraffic.forSpec("things.yaml", OpenApiTrafficTest.read("things.yaml"));
        filter = new OpenApiTrafficFilter(traffic, "/v1/");
    }

    @Test
    void theApplicationStillReadsTheBody_andTheCallerStillGetsTheResponse() throws Exception {
        MockHttpServletRequest request = post("/v1/things", "{\"name\":\"lamp\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenByTheApplication = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            seenByTheApplication.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            respond((HttpServletResponse) res, 201, "application/json", "{\"thing_id\":\"t1\",\"name\":\"lamp\"}");
        });

        assertThat(seenByTheApplication.get()).isEqualTo("{\"name\":\"lamp\"}");
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).isEqualTo("{\"thing_id\":\"t1\",\"name\":\"lamp\"}");
        assertThat(response.getHeader("X-Trace")).isEqualTo("abc");
        traffic.assertNoViolations();
        assertThat(traffic.exchanges()).isEqualTo(1);
    }

    @Test
    void aBadExchangeIsRecordedButNotChanged() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(post("/v1/things", "{\"name\":\"lamp\"}"), response,
                (req, res) -> respond((HttpServletResponse) res, 201, "application/json", "{\"name\":\"lamp\",\"extra\":1}"));

        assertThat(response.getContentAsString()).isEqualTo("{\"name\":\"lamp\",\"extra\":1}");
        assertThat(traffic.drainViolations()).singleElement().asString()
                .contains("POST /v1/things -> 201", "thing_id", "extra", "request body:  {\"name\":\"lamp\"}");
    }

    @Test
    void theQueryStringIsChecked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/things/t1");
        request.setQueryString("view=sideways");

        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> respond((HttpServletResponse) res, 200, "application/json", "{\"thing_id\":\"t1\",\"name\":\"lamp\"}"));

        assertThat(traffic.drainViolations()).singleElement().asString().contains("GET /v1/things/t1?view=sideways -> 200", "view");
    }

    @Test
    void requestsOutsideThePrefixAreLeftAlone() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response,
                (req, res) -> respond((HttpServletResponse) res, 200, "application/json", "{\"status\":\"UP\"}"));

        assertThat(response.getContentAsString()).isEqualTo("{\"status\":\"UP\"}");
        assertThat(traffic.exchanges()).isZero();
    }

    @Test
    void aStreamIsPassedThroughUnbuffered_andOnlyItsStatusIsChecked() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Object> responseSeenByTheApplication = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest("GET", "/v1/things/t1/changes"), response, (req, res) -> {
            responseSeenByTheApplication.set(res);
            respond((HttpServletResponse) res, 200, "text/event-stream", "data: one\n\n");
        });

        assertThat(responseSeenByTheApplication.get()).isSameAs(response);
        traffic.assertNoViolations();
        assertThat(traffic.coverageReport()).containsPattern("GET /v1/things/\\{thing_id}/changes\\s+listed \\[200]\\s+observed \\[200]");
    }

    @Test
    void anExceptionFromTheApplicationIsAViolation_andStillPropagates() {
        FilterChain failing = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(post("/v1/things", "{}"), new MockHttpServletResponse(), failing))
                .isInstanceOf(ServletException.class);
        assertThat(traffic.drainViolations()).singleElement().asString()
                .contains("POST /v1/things", "ended with an exception", "boom");
    }

    private static MockHttpServletRequest post(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static void respond(HttpServletResponse response, int status, String type, String body) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(type);
        response.setHeader("X-Trace", "abc");
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
