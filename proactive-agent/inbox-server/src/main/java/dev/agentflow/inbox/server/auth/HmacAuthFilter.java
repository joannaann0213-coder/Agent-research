package dev.agentflow.inbox.server.auth;

import dev.agentflow.inbox.api.HmacSigner;
import dev.agentflow.inbox.api.Problem;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.server.config.InboxProperties;
import dev.agentflow.inbox.server.time.InboxClock;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authenticates every {@code /v1/**} request by HMAC signature and checks the caller's role for the
 * path. A failed check stores nothing and never logs the request body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class HmacAuthFilter extends OncePerRequestFilter {

    private final ClientRegistry registry;
    private final InboxProperties.Auth config;
    private final InboxClock clock;
    private final JsonMapper json;

    public HmacAuthFilter(ClientRegistry registry, InboxProperties properties, InboxClock clock, JsonMapper json) {
        this.registry = registry;
        this.config = properties.getAuth();
        this.clock = clock;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] body = request.getInputStream().readNBytes(config.getMaxBodyBytes() + 1);
        if (body.length > config.getMaxBodyBytes()) {
            reject(response, HttpStatus.CONTENT_TOO_LARGE, ProblemCodes.PAYLOAD_TOO_LARGE,
                    "request body exceeds " + config.getMaxBodyBytes() + " bytes");
            return;
        }

        AuthenticatedClient client;
        if (!config.isEnabled()) {
            InboxProperties.Client dev = config.getDevClient();
            client = new AuthenticatedClient(dev.getId(), dev.getTenant(), Set.copyOf(dev.getRoles()));
        } else {
            client = authenticate(request, body);
            if (client == null) {
                reject(response, HttpStatus.UNAUTHORIZED, ProblemCodes.UNAUTHENTICATED,
                        "missing or invalid request signature");
                return;
            }
        }

        if (!allowed(client, request)) {
            reject(response, HttpStatus.FORBIDDEN, ProblemCodes.FORBIDDEN, "this client may not call this endpoint");
            return;
        }

        request.setAttribute(AuthenticatedClient.REQUEST_ATTRIBUTE, client);
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private AuthenticatedClient authenticate(HttpServletRequest request, byte[] body) {
        String clientId = request.getHeader(HmacSigner.HEADER_CLIENT_ID);
        String timestamp = request.getHeader(HmacSigner.HEADER_TIMESTAMP);
        String signature = request.getHeader(HmacSigner.HEADER_SIGNATURE);
        if (clientId == null || timestamp == null || signature == null) {
            return null;
        }
        long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        long skew = Math.abs(clock.now().getEpochSecond() - epochSeconds);
        if (skew > config.getClockSkew().toSeconds()) {
            return null;
        }
        Optional<ClientRegistry.Entry> entry = registry.find(clientId);
        if (entry.isEmpty()) {
            return null;
        }
        String path = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        if (!HmacSigner.verify(entry.get().secret(), signature, epochSeconds, request.getMethod(), path, body)) {
            return null;
        }
        return entry.get().client();
    }

    private static boolean allowed(AuthenticatedClient client, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/v1/admin/")) {
            return client.has(Role.OPERATOR);
        }
        if (path.startsWith("/v1/runs")) {
            return client.has(Role.RUNTIME) || ("GET".equals(request.getMethod()) && client.has(Role.OPERATOR));
        }
        if (path.startsWith("/v1/inbox/")) {
            return client.has(Role.PRODUCER) || client.has(Role.OPERATOR);
        }
        return true;
    }

    private void reject(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Problem problem = new Problem("about:blank", status.getReasonPhrase(), status.value(), detail, null, code);
        response.getOutputStream().write(json.writeValueAsBytes(problem));
    }
}
