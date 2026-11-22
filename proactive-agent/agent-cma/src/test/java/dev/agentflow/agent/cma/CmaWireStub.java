package dev.agentflow.agent.cma;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.agentflow.agent.cma.AgentSpec.ToolPolicy;
import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A local HTTP server that answers like the Managed Agents API, in its wire format, with a
 * {@link FakeManagedAgents} behind it. The SDK adapter is tested against it: what the adapter sends
 * is parsed back into the port's types and handed to the fake, and what the fake does is rendered as
 * the JSON and the server-sent events the API documents.
 *
 * <p>It checks what the real API checks first: an API key, and the beta header without which the
 * endpoints do not exist.
 */
final class CmaWireStub implements AutoCloseable {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String BETA = "managed-agents-2026-04-01";
    private static final Instant CREATED = Instant.parse("2026-11-20T10:00:00Z");

    private final FakeManagedAgents fake;
    private final HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final Map<String, List<Sse>> streams = new ConcurrentHashMap<>();
    private volatile int pageSize = 1000;

    CmaWireStub(FakeManagedAgents fake) {
        this.fake = fake;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Every request received, as {@code METHOD /path}. */
    List<String> requests() {
        return List.copyOf(requests);
    }

    /** Caps the pages of the event list, so that a short list already needs several requests. */
    void pageSize(int size) {
        this.pageSize = size;
    }

    /** Puts an event on the open streams of a session exactly as given: for event types the fake does not have. */
    void emitRaw(String sessionId, String json) {
        streams.getOrDefault(sessionId, List.of()).forEach(sse -> sse.write(JSON.readTree(json)));
    }

    @Override
    public void close() {
        server.stop(0);
    }

    // ================================================================ routing

    private static final Pattern ENVIRONMENT = Pattern.compile("/v1/environments/([^/]+)");
    private static final Pattern AGENT = Pattern.compile("/v1/agents/([^/]+)");
    private static final Pattern SESSION = Pattern.compile("/v1/sessions/([^/]+)");
    private static final Pattern EVENTS = Pattern.compile("/v1/sessions/([^/]+)/events");
    private static final Pattern STREAM = Pattern.compile("/v1/sessions/([^/]+)/events/stream");
    private static final Pattern CREDENTIALS = Pattern.compile("/v1/vaults/([^/]+)/credentials");
    private static final Pattern CREDENTIAL = Pattern.compile("/v1/vaults/([^/]+)/credentials/([^/]+)");
    private static final Pattern ARCHIVE = Pattern.compile("/v1/vaults/([^/]+)/credentials/([^/]+)/archive");

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        requests.add(method + " " + path);
        try {
            // Current SDK releases send the key as a bearer token; older ones used x-api-key.
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (exchange.getRequestHeaders().getFirst("x-api-key") == null
                    && (authorization == null || !authorization.startsWith("Bearer "))) {
                error(exchange, 401, "authentication_error", "an API key is required", null);
                return;
            }
            if (!String.join(",", exchange.getRequestHeaders().getOrDefault("anthropic-beta", List.of())).contains(BETA)) {
                error(exchange, 404, "not_found_error", "not found (the beta header is missing)", null);
                return;
            }
            Matcher match;
            if (method.equals("POST") && path.equals("/v1/environments")) {
                EnvironmentSpec spec = environmentSpec(body(exchange));
                respond(exchange, 200, environment(fake.createEnvironment(spec), spec));
            } else if (method.equals("POST") && (match = ENVIRONMENT.matcher(path)).matches()) {
                EnvironmentSpec spec = environmentSpec(body(exchange));
                fake.updateEnvironment(match.group(1), spec);
                respond(exchange, 200, environment(match.group(1), spec));
            } else if (method.equals("POST") && path.equals("/v1/agents")) {
                AgentSpec spec = agentSpec(body(exchange));
                respond(exchange, 200, agent(fake.createAgent(spec), spec));
            } else if (method.equals("POST") && (match = AGENT.matcher(path)).matches()) {
                JsonNode body = body(exchange);
                AgentSpec spec = agentSpec(body);
                respond(exchange, 200, agent(fake.updateAgent(new AgentRef(match.group(1), body.path("version").asInt()), spec), spec));
            } else if (method.equals("GET") && (match = AGENT.matcher(path)).matches()) {
                AgentRef latest = fake.getAgent(match.group(1));
                respond(exchange, 200, agent(latest, fake.agentSpec(latest)));
            } else if (method.equals("POST") && path.equals("/v1/sessions")) {
                respond(exchange, 200, session(fake.getSession(fake.createSession(sessionSpec(body(exchange))))));
            } else if (method.equals("GET") && (match = SESSION.matcher(path)).matches()) {
                respond(exchange, 200, session(fake.getSession(match.group(1))));
            } else if (method.equals("POST") && (match = EVENTS.matcher(path)).matches()) {
                ObjectNode sent = JSON.createObjectNode();
                ArrayNode data = sent.putArray("data");
                fake.send(match.group(1), outgoing(body(exchange))).forEach(event -> data.add(event(event)));
                respond(exchange, 200, sent);
            } else if (method.equals("GET") && (match = EVENTS.matcher(path)).matches()) {
                respond(exchange, 200, page(match.group(1), query(exchange)));
            } else if (method.equals("GET") && (match = STREAM.matcher(path)).matches()) {
                stream(exchange, match.group(1));
            } else if (method.equals("POST") && path.equals("/v1/vaults")) {
                JsonNode body = body(exchange);
                String id = fake.createVault(body.path("display_name").asString(null), strings(body.path("metadata")));
                ObjectNode vault = JSON.createObjectNode();
                vault.put("id", id).put("type", "vault").put("display_name", body.path("display_name").asString())
                        .put("created_at", "2026-11-20T10:00:00Z").put("updated_at", "2026-11-20T10:00:00Z");
                vault.set("metadata", body.path("metadata").isMissingNode() ? JSON.createObjectNode() : body.path("metadata"));
                respond(exchange, 200, vault);
            } else if (method.equals("POST") && (match = CREDENTIALS.matcher(path)).matches()) {
                JsonNode body = body(exchange);
                JsonNode auth = body.path("auth");
                McpCredential credential = switch (auth.path("type").asString("")) {
                    case "static_bearer" -> new McpCredential(auth.path("mcp_server_url").asString(null), body.path("display_name").asString(null),
                            auth.path("token").asString(null), null);
                    case "mcp_oauth" -> new McpCredential(auth.path("mcp_server_url").asString(null), body.path("display_name").asString(null),
                            null, new McpCredential.OAuth(auth.path("access_token").asString(null),
                                    auth.has("expires_at") ? Instant.parse(auth.path("expires_at").asString()) : null,
                                    auth.has("refresh") ? new McpCredential.Refresh(auth.path("refresh").path("refresh_token").asString(null),
                                            auth.path("refresh").path("client_id").asString(null),
                                            auth.path("refresh").path("token_endpoint").asString(null),
                                            auth.path("refresh").path("token_endpoint_auth").path("type").asString(null),
                                            auth.path("refresh").path("token_endpoint_auth").path("client_secret").asString(null),
                                            auth.path("refresh").path("scope").asString(null)) : null));
                    default -> throw CmaException.invalid("auth.type must be static_bearer or mcp_oauth");
                };
                expect(credential.bearerToken() == null || !credential.bearerToken().isEmpty(), "a token is required");
                String id = fake.createCredential(match.group(1), credential);
                respond(exchange, 200, credentialJson(id, match.group(1), auth.path("type").asString(), credential.mcpServerUrl(), false));
            } else if (method.equals("GET") && (match = CREDENTIALS.matcher(path)).matches()) {
                ObjectNode page = JSON.createObjectNode();
                ArrayNode data = page.putArray("data");
                String vaultId = match.group(1);
                fake.listCredentials(vaultId).forEach(stored ->
                        data.add(credentialJson(stored.id(), vaultId, "static_bearer", stored.mcpServerUrl(), false)));
                page.putNull("next_page");
                respond(exchange, 200, page);
            } else if (method.equals("POST") && (match = ARCHIVE.matcher(path)).matches()) {
                fake.archiveCredential(match.group(1), match.group(2));
                respond(exchange, 200, credentialJson(match.group(2), match.group(1), "static_bearer", "https://archived.example", true));
            } else if (method.equals("POST") && (match = CREDENTIAL.matcher(path)).matches()) {
                JsonNode auth = body(exchange).path("auth");
                expect("static_bearer".equals(auth.path("type").asString("")), "only a static bearer token is updated here");
                fake.updateCredential(match.group(1), match.group(2), auth.path("token").asString(null));
                respond(exchange, 200, credentialJson(match.group(2), match.group(1), "static_bearer", "https://updated.example", false));
            } else {
                error(exchange, 404, "not_found_error", "no such endpoint: " + method + " " + path, null);
            }
        } catch (CmaException e) {
            error(exchange, e.status() == 0 ? 500 : e.status(), switch (e.kind()) {
                case INVALID_REQUEST -> "invalid_request_error";
                case UNAUTHORIZED -> "authentication_error";
                case NOT_FOUND -> "not_found_error";
                case CONFLICT -> "conflict_error";
                case RATE_LIMITED -> "rate_limit_error";
                case OVERLOADED -> "overloaded_error";
                case SERVER, CONNECTION -> "api_error";
            }, e.getMessage(), e.retryAfter());
        } catch (RuntimeException e) {
            error(exchange, 500, "api_error", String.valueOf(e), null);
        } finally {
            exchange.close();
        }
    }

    // ================================================================ requests, parsed back into the port's types

    private static EnvironmentSpec environmentSpec(JsonNode body) {
        JsonNode networking = body.path("config").path("networking");
        expect("cloud".equals(body.path("config").path("type").asString("")), "config.type must be cloud");
        List<String> hosts = null;
        if ("limited".equals(networking.path("type").asString(""))) {
            hosts = new ArrayList<>();
            for (JsonNode host : networking.path("allowed_hosts")) {
                hosts.add(host.asString());
            }
        } else {
            expect("unrestricted".equals(networking.path("type").asString("")), "networking.type must be limited or unrestricted");
        }
        return new EnvironmentSpec(body.path("name").asString(null), body.path("description").asString(null), hosts,
                networking.path("allow_mcp_servers").asBoolean(false),
                networking.path("allow_package_managers").asBoolean(false), strings(body.path("metadata")));
    }

    private static AgentSpec agentSpec(JsonNode body) {
        JsonNode model = body.path("model");
        String effort = model.path("effort").isString() ? model.path("effort").asString()
                : model.path("effort").path("type").asString(null);
        AgentSpec.Sandbox sandbox = null;
        Map<String, ToolPolicy> serverPolicies = new LinkedHashMap<>();
        List<AgentSpec.CustomTool> customTools = new ArrayList<>();
        for (JsonNode tool : body.path("tools")) {
            switch (tool.path("type").asString("")) {
                case "agent_toolset_20260401" -> {
                    boolean webSearch = true;
                    boolean webFetch = true;
                    for (JsonNode config : tool.path("configs")) {
                        if ("web_search".equals(config.path("name").asString(""))) {
                            webSearch = config.path("enabled").asBoolean(true);
                        } else if ("web_fetch".equals(config.path("name").asString(""))) {
                            webFetch = config.path("enabled").asBoolean(true);
                        }
                    }
                    sandbox = new AgentSpec.Sandbox(policy(tool, ToolPolicy.ALWAYS_ALLOW), webSearch, webFetch);
                }
                case "mcp_toolset" ->
                        serverPolicies.put(tool.path("mcp_server_name").asString(), policy(tool, ToolPolicy.ALWAYS_ASK));
                case "custom" -> customTools.add(new AgentSpec.CustomTool(tool.path("name").asString(),
                        tool.path("description").asString(null), tool.path("input_schema")));
                default -> throw CmaException.invalid("tools: unknown type " + tool.path("type"));
            }
        }
        List<AgentSpec.McpServer> servers = new ArrayList<>();
        for (JsonNode server : body.path("mcp_servers")) {
            expect("url".equals(server.path("type").asString("")), "mcp_servers.type must be url");
            String name = server.path("name").asString();
            expect(serverPolicies.containsKey(name), "MCP server " + name + " has no mcp_toolset");
            servers.add(new AgentSpec.McpServer(name, server.path("url").asString(), serverPolicies.get(name)));
        }
        expect(servers.size() == serverPolicies.size(), "an mcp_toolset names a server that is not declared");
        return new AgentSpec(body.path("name").asString(null), body.path("description").asString(null),
                model.isString() ? model.asString() : model.path("id").asString(null), effort,
                body.path("system").asString(null), sandbox, servers, customTools, strings(body.path("metadata")));
    }

    private static ToolPolicy policy(JsonNode toolset, ToolPolicy byDefault) {
        return switch (toolset.path("default_config").path("permission_policy").path("type").asString("")) {
            case "always_allow" -> ToolPolicy.ALWAYS_ALLOW;
            case "always_ask" -> ToolPolicy.ALWAYS_ASK;
            case "auto" -> ToolPolicy.AUTO;
            default -> byDefault;
        };
    }

    private static SessionSpec sessionSpec(JsonNode body) {
        JsonNode agent = body.path("agent");
        expect("agent".equals(agent.path("type").asString("")), "agent.type must be agent");
        List<String> vaults = new ArrayList<>();
        body.path("vault_ids").forEach(vault -> vaults.add(vault.asString()));
        JsonNode amount = body.path("budget").findValue("amount");
        return new SessionSpec(new AgentRef(agent.path("id").asString(), agent.path("version").asInt()),
                body.path("environment_id").asString(null), body.path("title").asString(null),
                strings(body.path("metadata")), vaults, amount == null ? null : new java.math.BigDecimal(amount.asString()));
    }

    private static List<Outgoing> outgoing(JsonNode body) {
        List<Outgoing> events = new ArrayList<>();
        for (JsonNode event : body.path("events")) {
            events.add(switch (event.path("type").asString("")) {
                case "user.message" -> new Outgoing.UserMessage(text(event.path("content")));
                case "user.interrupt" -> new Outgoing.Interrupt();
                case "user.tool_confirmation" -> new Outgoing.ToolConfirmation(event.path("tool_use_id").asString(),
                        "allow".equals(event.path("result").asString()), event.path("deny_message").asString(null));
                case "user.custom_tool_result" -> new Outgoing.CustomToolResult(
                        event.path("custom_tool_use_id").asString(), text(event.path("content")),
                        event.path("is_error").asBoolean(false));
                default -> throw CmaException.invalid("events: unknown type " + event.path("type"));
            });
        }
        return events;
    }

    private static String text(JsonNode content) {
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            expect("text".equals(block.path("type").asString("")), "content blocks must be of type text");
            text.append(block.path("text").asString());
        }
        return text.toString();
    }

    // ================================================================ responses, in the wire format

    private static ObjectNode environment(String id, EnvironmentSpec spec) {
        ObjectNode environment = JSON.createObjectNode();
        environment.put("type", "environment").put("id", id).put("name", spec.name());
        environment.put("description", spec.description() == null ? "" : spec.description());
        ObjectNode networking = environment.putObject("config").put("type", "cloud").putObject("networking");
        if (spec.unrestricted()) {
            networking.put("type", "unrestricted");
        } else {
            networking.put("type", "limited");
            ArrayNode hosts = networking.putArray("allowed_hosts");
            spec.allowedHosts().forEach(hosts::add);
            networking.put("allow_mcp_servers", spec.allowMcpServers());
            networking.put("allow_package_managers", spec.allowPackageManagers());
        }
        ObjectNode metadata = environment.putObject("metadata");
        spec.metadata().forEach(metadata::put);
        environment.put("created_at", CREATED.toString()).put("updated_at", CREATED.toString());
        environment.putNull("archived_at");
        return environment;
    }

    private static ObjectNode agent(AgentRef ref, AgentSpec spec) {
        ObjectNode agent = JSON.createObjectNode();
        agent.put("type", "agent").put("id", ref.id()).put("version", ref.version()).put("name", spec.name());
        ObjectNode model = agent.putObject("model").put("id", spec.model()).put("speed", "standard");
        if (spec.effort() != null) {
            model.putObject("effort").put("type", spec.effort());
        }
        agent.put("system", spec.system()).put("description", spec.description());
        agent.putArray("tools");
        agent.putArray("mcp_servers");
        agent.putArray("skills");
        ObjectNode metadata = agent.putObject("metadata");
        spec.metadata().forEach(metadata::put);
        agent.put("created_at", CREATED.toString()).put("updated_at", CREATED.toString());
        agent.putNull("archived_at");
        return agent;
    }

    private ObjectNode session(SessionInfo info) {
        SessionSpec spec = fake.sessionSpec(info.id());
        ObjectNode session = JSON.createObjectNode();
        session.put("type", "session").put("id", info.id());
        session.put("status", info.status().name().toLowerCase());
        session.put("environment_id", spec.environmentId()).put("title", spec.title());
        ObjectNode agent = agent(spec.agent(), fake.agentSpec(spec.agent()));
        agent.remove(List.of("created_at", "updated_at", "archived_at", "metadata"));
        session.set("agent", agent);
        ObjectNode metadata = session.putObject("metadata");
        spec.metadata().forEach(metadata::put);
        ArrayNode vaults = session.putArray("vault_ids");
        spec.vaultIds().forEach(vaults::add);
        session.putArray("resources");
        session.putArray("outcome_evaluations");
        session.putObject("usage");
        session.putObject("stats");
        session.put("created_at", CREATED.toString()).put("updated_at", CREATED.toString());
        session.putNull("archived_at");
        return session;
    }

    private static ObjectNode credentialJson(String id, String vaultId, String type, String url, boolean archived) {
        ObjectNode credential = JSON.createObjectNode();
        credential.put("id", id).put("type", "vault_credential").put("vault_id", vaultId)
                .put("created_at", "2026-11-20T10:00:00Z").put("updated_at", "2026-11-20T10:00:00Z");
        if (archived) {
            credential.put("archived_at", "2026-11-20T10:00:00Z");
        }
        credential.putObject("metadata");
        credential.putObject("auth").put("type", type).put("mcp_server_url", url);
        return credential;
    }

    private ObjectNode page(String sessionId, Map<String, String> query) {
        String since = query.get("created_at[gte]");
        List<CmaEvent> events = new ArrayList<>(fake.listEvents(sessionId, since == null ? null : Instant.parse(since)));
        if ("desc".equals(query.get("order"))) {
            java.util.Collections.reverse(events);
        }
        int from = query.containsKey("page") ? Integer.parseInt(query.get("page").substring("page_".length())) : 0;
        int size = Math.min(pageSize, Integer.parseInt(query.getOrDefault("limit", "1000")));
        int to = Math.min(events.size(), from + size);
        ObjectNode page = JSON.createObjectNode();
        ArrayNode data = page.putArray("data");
        events.subList(from, to).forEach(event -> data.add(event(event)));
        if (to < events.size()) {
            page.put("next_page", "page_" + to);
        } else {
            page.putNull("next_page");
        }
        return page;
    }

    /** One event as the API writes it. */
    static ObjectNode event(CmaEvent event) {
        ObjectNode json = JSON.createObjectNode();
        json.put("type", event.type()).put("id", event.id());
        if (event.processedAt() == null) {
            json.putNull("processed_at");
        } else {
            json.put("processed_at", event.processedAt().toString());
        }
        switch (event) {
            case CmaEvent.UserMessage message ->
                    json.putArray("content").addObject().put("type", "text").put("text", message.text());
            case CmaEvent.UserInterrupt interrupt -> {
            }
            case CmaEvent.UserToolConfirmation confirmation -> json.put("tool_use_id", confirmation.toolUseId())
                    .put("result", confirmation.allow() ? "allow" : "deny");
            case CmaEvent.UserCustomToolResult result -> json.put("custom_tool_use_id", result.customToolUseId());
            case CmaEvent.AgentMessage message ->
                    json.putArray("content").addObject().put("type", "text").put("text", message.text());
            case CmaEvent.ToolUse call -> {
                json.put("name", call.name());
                json.set("input", call.input());
                if (call.mcpServer() != null) {
                    json.put("mcp_server_name", call.mcpServer());
                }
                if (call.permission() != null) {
                    json.put("evaluated_permission", call.permission().name().toLowerCase());
                }
            }
            case CmaEvent.ToolResult result -> {
                json.put(result.mcp() ? "mcp_tool_use_id" : "tool_use_id", result.toolUseId());
                json.put("is_error", result.error());
                json.putArray("content").addObject().put("type", "text").put("text", result.error() ? "failed" : "ok");
            }
            case CmaEvent.CustomToolUse call -> {
                json.put("name", call.name());
                json.set("input", call.input());
            }
            case CmaEvent.Running running -> {
            }
            case CmaEvent.Idle idle -> {
                ObjectNode reason = json.putObject("stop_reason");
                reason.put("type", idle.reason().name().toLowerCase());
                if (idle.reason() == CmaEvent.StopReason.REQUIRES_ACTION) {
                    ArrayNode ids = reason.putArray("event_ids");
                    idle.awaiting().forEach(ids::add);
                }
            }
            case CmaEvent.Rescheduled rescheduled -> {
            }
            case CmaEvent.Terminated terminated -> {
            }
            case CmaEvent.Error error -> {
                ObjectNode detail = json.putObject("error");
                detail.put("type", error.errorType()).put("message", error.message());
                if (error.retry() != null) {
                    detail.putObject("retry_status").put("type", error.retry().name().toLowerCase());
                }
            }
            case CmaEvent.Other other -> {
            }
        }
        return json;
    }

    // ================================================================ the stream

    private void stream(HttpExchange exchange, String sessionId) throws IOException {
        // The fake's stream is opened before the response starts: once the client has the headers,
        // nothing that happens in the session can be missed.
        EventStream source = fake.openStream(sessionId);
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        Sse sse = new Sse(exchange.getResponseBody());
        streams.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>()).add(sse);
        try {
            int quiet = 0;
            while (!sse.broken) {
                Optional<CmaEvent> next = source.poll(Duration.ofMillis(50));
                if (next.isPresent()) {
                    sse.write(event(next.get()));
                    quiet = 0;
                } else if (++quiet % 10 == 0) {
                    sse.heartbeat();
                }
            }
        } catch (CmaException lost) {
            // the fake dropped the stream: the connection ends without a word, as a network failure would
        } finally {
            streams.get(sessionId).remove(sse);
            source.close();
        }
    }

    private static final class Sse {
        private final OutputStream out;
        private volatile boolean broken;

        Sse(OutputStream out) {
            this.out = out;
        }

        synchronized void write(JsonNode event) {
            send("event: " + event.path("type").asString("message") + "\ndata: " + event + "\n\n");
        }

        synchronized void heartbeat() {
            send("event: ping\ndata: {\"type\": \"ping\"}\n\n");
        }

        private void send(String frame) {
            try {
                out.write(frame.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                broken = true;
            }
        }
    }

    // ================================================================ helpers

    private static JsonNode body(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        return bytes.length == 0 ? JSON.createObjectNode() : JSON.readTree(bytes);
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> query = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    query.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return query;
    }

    private static Map<String, String> strings(JsonNode object) {
        Map<String, String> strings = new LinkedHashMap<>();
        object.properties().forEach(entry -> strings.put(entry.getKey(), entry.getValue().asString()));
        return strings;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw CmaException.invalid(message);
        }
    }

    private static void respond(HttpExchange exchange, int status, JsonNode body) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("request-id", "req_stub");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static void error(HttpExchange exchange, int status, String type, String message, Duration retryAfter)
            throws IOException {
        ObjectNode body = JSON.createObjectNode();
        body.put("type", "error");
        body.putObject("error").put("type", type).put("message", message);
        if (retryAfter != null) {
            exchange.getResponseHeaders().add("retry-after", Long.toString(retryAfter.toSeconds()));
        }
        respond(exchange, status, body);
    }
}
