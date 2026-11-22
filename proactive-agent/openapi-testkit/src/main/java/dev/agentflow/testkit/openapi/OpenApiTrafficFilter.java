package dev.agentflow.testkit.openapi;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Servlet filter for tests: hands every exchange under a path prefix to an {@link OpenApiTraffic}.
 *
 * <p>Register it in front of every other filter of the application, so that it also sees the
 * responses those filters produce (a refused signature, a body that is too large). The exchange is
 * recorded before the response reaches the caller: once a test has its response, the verdict on that
 * exchange is in.
 */
public final class OpenApiTrafficFilter implements Filter {

    private final OpenApiTraffic traffic;
    private final String pathPrefix;

    /**
     * @param pathPrefix only requests whose path starts with this are looked at, for example {@code /v1/}
     */
    public OpenApiTrafficFilter(OpenApiTraffic traffic, String pathPrefix) {
        this.traffic = traffic;
        this.pathPrefix = pathPrefix;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (request.getDispatcherType() != DispatcherType.REQUEST || !request.getRequestURI().startsWith(pathPrefix)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        byte[] requestBody = request.getInputStream().readAllBytes();
        HttpServletRequest replayed = new ReplayedBodyRequest(request, requestBody);

        if (traffic.streams(request.getMethod(), request.getRequestURI())) {
            // A stream is passed through as it is; only the request and the status are checked.
            try {
                chain.doFilter(replayed, response);
            } catch (IOException | ServletException | RuntimeException e) {
                traffic.failed(request.getMethod(), request.getRequestURI(), e);
                throw e;
            }
            traffic.record(exchange(request, requestBody, response, null));
            return;
        }

        ContentCachingResponseWrapper buffered = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(replayed, buffered);
        } catch (IOException | ServletException | RuntimeException e) {
            traffic.failed(request.getMethod(), request.getRequestURI(), e);
            throw e;
        }
        traffic.record(exchange(request, requestBody, buffered, buffered.getContentAsByteArray()));
        buffered.copyBodyToResponse();
    }

    private static Exchange exchange(HttpServletRequest request, byte[] requestBody, HttpServletResponse response,
            byte[] responseBody) {
        Map<String, List<String>> requestHeaders = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            requestHeaders.put(name, Collections.list(request.getHeaders(name)));
        }
        Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            responseHeaders.put(name, new ArrayList<>(response.getHeaders(name)));
        }
        if (response.getContentType() != null) {
            responseHeaders.put("Content-Type", List.of(response.getContentType()));
        }
        return new Exchange(request.getMethod(), request.getRequestURI(), request.getQueryString(), requestHeaders,
                requestBody, response.getStatus(), responseHeaders, responseBody);
    }

    /** Serves a body that has already been read, to the rest of the chain, as often as it is asked for. */
    private static final class ReplayedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        ReplayedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return in.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("the body has already been read");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
