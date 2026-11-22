package dev.agentflow.agent.server.auth;

import dev.agentflow.agent.api.Problem;
import dev.agentflow.agent.api.ProblemCodes;
import dev.agentflow.agent.server.app.Apps;
import dev.agentflow.agent.server.config.AgentProperties;
import dev.agentflow.agent.server.delivery.StreamTickets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authenticates every {@code /v1/**} request by its bearer credential and decides the one thing
 * that follows from the path alone: {@code /v1/admin} is for administrators, everything else for
 * applications and their users. What a user token may do beyond that is decided where it is done.
 *
 * <p>Three kinds of credential are told apart by their prefix: an API key of an application
 * ({@code afk.}), a user token ({@code aft.}), and anything else, which is compared with the
 * configured administration keys.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class ApiAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    /** The one request that may be authenticated by a ticket in its query, because a browser's EventSource cannot send a header. */
    private static final Pattern STREAM = Pattern.compile("/v1/users/[^/]+/stream");

    private final List<byte[]> adminKeys;
    private final ApiKeys apiKeys;
    private final UserTokens userTokens;
    private final Apps apps;
    private final StreamTickets tickets;
    private final RateLimiter limiter;
    private final JsonMapper json;

    public ApiAuthFilter(AgentProperties properties, ApiKeys apiKeys, UserTokens userTokens, Apps apps, StreamTickets tickets,
            RateLimiter limiter, JsonMapper json) {
        for (String key : properties.getAdmin().getKeys()) {
            if (key == null || key.length() < 32) {
                throw new IllegalStateException("every entry of agent.admin.keys needs at least 32 characters");
            }
        }
        this.adminKeys = properties.getAdmin().getKeys().stream().map(key -> key.getBytes(StandardCharsets.UTF_8)).toList();
        this.apiKeys = apiKeys;
        this.userTokens = userTokens;
        this.apps = apps;
        this.tickets = tickets;
        this.limiter = limiter;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String ticket = request.getParameter("ticket");
        if (header == null && ticket != null && "GET".equals(request.getMethod()) && STREAM.matcher(request.getRequestURI()).matches()) {
            Optional<StreamTickets.Holder> holder = tickets.use(ticket);
            if (holder.isEmpty()) {
                reject(response, HttpStatus.UNAUTHORIZED, ProblemCodes.UNAUTHENTICATED, "unknown, expired or used stream ticket");
                return;
            }
            // A ticket opens the stream of the one user it was issued for, and nothing else.
            proceed(request, response, chain, new Caller(Caller.Kind.USER, holder.get().appId(), holder.get().userId(), null));
            return;
        }
        if (header == null || !header.startsWith(BEARER)) {
            reject(response, HttpStatus.UNAUTHORIZED, ProblemCodes.UNAUTHENTICATED,
                    "send the credential as Authorization: Bearer <credential>");
            return;
        }
        String credential = header.substring(BEARER.length()).trim();

        Caller caller;
        if (credential.startsWith(ApiKeys.PREFIX)) {
            Optional<ApiKeys.Verified> key = apiKeys.verify(credential);
            if (key.isEmpty()) {
                reject(response, HttpStatus.UNAUTHORIZED, ProblemCodes.UNAUTHENTICATED, "unknown or revoked API key");
                return;
            }
            if (!key.get().appActive()) {
                reject(response, HttpStatus.FORBIDDEN, ProblemCodes.FORBIDDEN, "the application is suspended");
                return;
            }
            caller = new Caller(Caller.Kind.APP, key.get().appId(), null, key.get().keyId());
        } else if (credential.startsWith(UserTokens.PREFIX)) {
            Optional<UserTokens.Verified> token = userTokens.verify(credential);
            if (token.isEmpty()) {
                reject(response, HttpStatus.UNAUTHORIZED, ProblemCodes.UNAUTHENTICATED, "invalid, expired or revoked user token");
                return;
            }
            if (apps.find(token.get().appId()).map(app -> !"active".equals(app.status())).orElse(true)) {
                reject(response, HttpStatus.FORBIDDEN, ProblemCodes.FORBIDDEN, "the application is suspended");
                return;
            }
            caller = new Caller(Caller.Kind.USER, token.get().appId(), token.get().userId(), null);
        } else if (isAdminKey(credential)) {
            caller = new Caller(Caller.Kind.ADMIN, null, null, null);
        } else {
            reject(response, HttpStatus.UNAUTHORIZED, ProblemCodes.UNAUTHENTICATED, "unknown credential");
            return;
        }

        boolean adminPath = request.getRequestURI().startsWith("/v1/admin/");
        if (adminPath != (caller.kind() == Caller.Kind.ADMIN)) {
            reject(response, HttpStatus.FORBIDDEN, ProblemCodes.FORBIDDEN, adminPath
                    ? "the administration API needs an administration key"
                    : "an administration key is only good for the administration API");
            return;
        }

        proceed(request, response, chain, caller);
    }

    private void proceed(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Caller caller)
            throws ServletException, IOException {
        Optional<Duration> wait = limiter.take(caller);
        if (wait.isPresent()) {
            response.setHeader("Retry-After", Long.toString(Math.max(1, (wait.get().toMillis() + 999) / 1000)));
            reject(response, HttpStatus.TOO_MANY_REQUESTS, ProblemCodes.RATE_LIMITED,
                    "too many requests; try again after the time in Retry-After");
            return;
        }
        request.setAttribute(Caller.REQUEST_ATTRIBUTE, caller);
        chain.doFilter(request, response);
    }

    private boolean isAdminKey(String credential) {
        byte[] presented = credential.getBytes(StandardCharsets.UTF_8);
        boolean match = false;
        for (byte[] key : adminKeys) {
            match |= MessageDigest.isEqual(presented, key);
        }
        return match;
    }

    private void reject(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Problem problem = new Problem("about:blank", status.getReasonPhrase(), status.value(), detail, null, code);
        response.getOutputStream().write(json.writeValueAsBytes(problem));
    }
}
