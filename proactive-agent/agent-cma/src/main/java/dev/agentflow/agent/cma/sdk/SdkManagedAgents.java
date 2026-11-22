package dev.agentflow.agent.cma.sdk;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.Timeout;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.beta.agents.AgentCreateParams;
import com.anthropic.models.beta.agents.AgentUpdateParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgent;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfigParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAllowPolicy;
import com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAskPolicy;
import com.anthropic.models.beta.agents.BetaManagedAgentsAutoPolicy;
import com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema;
import com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfigParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsModelConfigParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsUrlMcpServerParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsWebFetchToolConfigParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsWebSearchToolConfigParams;
import com.anthropic.models.beta.environments.BetaCloudConfigParams;
import com.anthropic.models.beta.environments.BetaLimitedNetworkParams;
import com.anthropic.models.beta.environments.BetaUnrestrictedNetwork;
import com.anthropic.models.beta.environments.EnvironmentCreateParams;
import com.anthropic.models.beta.environments.EnvironmentUpdateParams;
import com.anthropic.models.beta.sessions.BetaManagedAgentsAgentParams;
import com.anthropic.models.beta.sessions.BetaManagedAgentsSession;
import com.anthropic.models.beta.sessions.SessionCreateParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsStreamSessionEvents;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEventParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEventParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEventParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams;
import com.anthropic.models.beta.sessions.events.EventListParams;
import com.anthropic.models.beta.sessions.events.EventSendParams;
import dev.agentflow.agent.cma.AgentRef;
import dev.agentflow.agent.cma.AgentSpec;
import dev.agentflow.agent.cma.AgentSpec.ToolPolicy;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.CmaException.Kind;
import dev.agentflow.agent.cma.EnvironmentSpec;
import dev.agentflow.agent.cma.EventStream;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.Outgoing;
import dev.agentflow.agent.cma.SessionInfo;
import dev.agentflow.agent.cma.SessionSpec;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;

/**
 * The port to Claude Managed Agents over the official Java SDK.
 *
 * <p>Calls that read are retried by the SDK. Calls that send events are not: a send that timed out
 * may have been delivered, and sending it again would hand the agent the same message twice. The
 * caller decides what to do, see {@link CmaException#maybeDelivered()}.
 */
public final class SdkManagedAgents implements ManagedAgents, AutoCloseable {

    /** A stream is silent between events except for heartbeats; without any of them for this long it counts as lost. */
    private static final Duration STREAM_SILENCE = Duration.ofMinutes(2);
    private static final Duration STREAM_LIFETIME = Duration.ofHours(2);
    private static final int EVENTS_PER_PAGE = 1000;

    private final AnthropicClient client;
    private final AnthropicClient once;
    private final AnthropicClient streaming;

    /**
     * @param client a configured SDK client. It is closed by {@link #close()}
     */
    public SdkManagedAgents(AnthropicClient client) {
        this.client = client;
        this.once = client.withOptions(options -> options.maxRetries(0));
        this.streaming = client.withOptions(options -> options.maxRetries(0)
                .timeout(Timeout.builder().read(STREAM_SILENCE).request(STREAM_LIFETIME).build()));
    }

    /**
     * Connects with an API key. Nothing is read from the process environment: the key and the
     * address are exactly the ones given here.
     *
     * @param baseUrl the address of the API; {@code null} for {@code https://api.anthropic.com}
     * @param requestTimeout how long one ordinary call may take
     */
    public static SdkManagedAgents connect(String apiKey, String baseUrl, Duration requestTimeout) {
        return new SdkManagedAgents(AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl == null ? "https://api.anthropic.com" : baseUrl)
                .timeout(requestTimeout)
                .build());
    }

    @Override
    public void close() {
        client.close();
    }

    // ================================================================ set-up

    @Override
    public String createEnvironment(EnvironmentSpec spec) {
        return call(() -> {
            EnvironmentCreateParams.Builder params = EnvironmentCreateParams.builder()
                    .name(spec.name())
                    .config(cloud(spec));
            if (spec.description() != null) {
                params.description(spec.description());
            }
            if (!spec.metadata().isEmpty()) {
                EnvironmentCreateParams.Metadata.Builder metadata = EnvironmentCreateParams.Metadata.builder();
                spec.metadata().forEach((key, value) -> metadata.putAdditionalProperty(key, JsonValue.from(value)));
                params.metadata(metadata.build());
            }
            return client.beta().environments().create(params.build()).id();
        });
    }

    @Override
    public void updateEnvironment(String environmentId, EnvironmentSpec spec) {
        call(() -> {
            EnvironmentUpdateParams.Builder params = EnvironmentUpdateParams.builder()
                    .name(spec.name())
                    .config(cloud(spec));
            if (spec.description() != null) {
                params.description(spec.description());
            }
            return client.beta().environments().update(environmentId, params.build());
        });
    }

    private static BetaCloudConfigParams cloud(EnvironmentSpec spec) {
        BetaCloudConfigParams.Builder config = BetaCloudConfigParams.builder();
        if (spec.unrestricted()) {
            config.networking(BetaUnrestrictedNetwork.builder().build());
        } else {
            config.networking(BetaLimitedNetworkParams.builder()
                    .allowedHosts(spec.allowedHosts())
                    .allowMcpServers(spec.allowMcpServers())
                    .allowPackageManagers(spec.allowPackageManagers())
                    .build());
        }
        return config.build();
    }

    @Override
    public AgentRef createAgent(AgentSpec spec) {
        return call(() -> {
            AgentCreateParams.Builder params = AgentCreateParams.builder()
                    .name(spec.name())
                    .model(model(spec));
            if (spec.description() != null) {
                params.description(spec.description());
            }
            if (spec.system() != null) {
                params.system(spec.system());
            }
            if (spec.sandbox() != null) {
                params.addTool(sandbox(spec.sandbox()));
            }
            for (AgentSpec.McpServer server : spec.mcpServers()) {
                params.addMcpServer(mcpServer(server));
                params.addTool(mcpToolset(server));
            }
            for (AgentSpec.CustomTool tool : spec.customTools()) {
                params.addTool(customTool(tool));
            }
            if (!spec.metadata().isEmpty()) {
                AgentCreateParams.Metadata.Builder metadata = AgentCreateParams.Metadata.builder();
                spec.metadata().forEach((key, value) -> metadata.putAdditionalProperty(key, JsonValue.from(value)));
                params.metadata(metadata.build());
            }
            BetaManagedAgentsAgent agent = client.beta().agents().create(params.build());
            return new AgentRef(agent.id(), agent.version());
        });
    }

    @Override
    public AgentRef updateAgent(AgentRef current, AgentSpec spec) {
        return call(() -> {
            // An update replaces the lists as a whole, so every tool and server is sent again.
            List<AgentUpdateParams.Tool> tools = new ArrayList<>();
            if (spec.sandbox() != null) {
                tools.add(AgentUpdateParams.Tool.ofAgentToolset20260401(sandbox(spec.sandbox())));
            }
            for (AgentSpec.McpServer server : spec.mcpServers()) {
                tools.add(AgentUpdateParams.Tool.ofMcpToolset(mcpToolset(server)));
            }
            for (AgentSpec.CustomTool tool : spec.customTools()) {
                tools.add(AgentUpdateParams.Tool.ofCustom(customTool(tool)));
            }
            AgentUpdateParams.Builder params = AgentUpdateParams.builder()
                    .version(current.version())
                    .name(spec.name())
                    .model(model(spec))
                    .description(Optional.ofNullable(spec.description()))
                    .system(Optional.ofNullable(spec.system()))
                    .tools(tools)
                    .mcpServers(spec.mcpServers().stream().map(SdkManagedAgents::mcpServer).toList());
            AgentUpdateParams.Metadata.Builder metadata = AgentUpdateParams.Metadata.builder();
            spec.metadata().forEach((key, value) -> metadata.putAdditionalProperty(key, JsonValue.from(value)));
            params.metadata(metadata.build());
            BetaManagedAgentsAgent agent = client.beta().agents().update(current.id(), params.build());
            return new AgentRef(agent.id(), agent.version());
        });
    }

    private static BetaManagedAgentsModelConfigParams model(AgentSpec spec) {
        BetaManagedAgentsModelConfigParams.Builder model = BetaManagedAgentsModelConfigParams.builder().id(spec.model());
        if (spec.effort() != null) {
            model.effort(BetaManagedAgentsModelConfigParams.Effort.BetaManagedAgentsEffortLevel.of(spec.effort()));
        }
        return model.build();
    }

    private static BetaManagedAgentsAgentToolset20260401Params sandbox(AgentSpec.Sandbox sandbox) {
        BetaManagedAgentsAgentToolsetDefaultConfigParams.Builder defaults =
                BetaManagedAgentsAgentToolsetDefaultConfigParams.builder();
        switch (sandbox.policy()) {
            case ALWAYS_ALLOW -> defaults.permissionPolicy(BetaManagedAgentsAlwaysAllowPolicy.builder().build());
            case ALWAYS_ASK -> defaults.permissionPolicy(BetaManagedAgentsAlwaysAskPolicy.builder().build());
            case AUTO -> defaults.permissionPolicy(BetaManagedAgentsAutoPolicy.builder().build());
        }
        // The two web tools are always listed, so that leaving one out can never switch it on.
        return BetaManagedAgentsAgentToolset20260401Params.builder()
                .defaultConfig(defaults.build())
                .addConfig(BetaManagedAgentsWebFetchToolConfigParams.builder().enabled(sandbox.webFetch()).build())
                .addConfig(BetaManagedAgentsWebSearchToolConfigParams.builder().enabled(sandbox.webSearch()).build())
                .build();
    }

    private static BetaManagedAgentsUrlMcpServerParams mcpServer(AgentSpec.McpServer server) {
        return BetaManagedAgentsUrlMcpServerParams.builder().name(server.name()).url(server.url()).build();
    }

    private static BetaManagedAgentsMcpToolsetParams mcpToolset(AgentSpec.McpServer server) {
        BetaManagedAgentsMcpToolsetDefaultConfigParams.Builder defaults =
                BetaManagedAgentsMcpToolsetDefaultConfigParams.builder();
        ToolPolicy policy = server.policy() == null ? ToolPolicy.ALWAYS_ASK : server.policy();
        switch (policy) {
            case ALWAYS_ALLOW -> defaults.permissionPolicy(BetaManagedAgentsAlwaysAllowPolicy.builder().build());
            case ALWAYS_ASK -> defaults.permissionPolicy(BetaManagedAgentsAlwaysAskPolicy.builder().build());
            case AUTO -> defaults.permissionPolicy(BetaManagedAgentsAutoPolicy.builder().build());
        }
        return BetaManagedAgentsMcpToolsetParams.builder()
                .mcpServerName(server.name())
                .defaultConfig(defaults.build())
                .build();
    }

    private static BetaManagedAgentsCustomToolParams customTool(AgentSpec.CustomTool tool) {
        JsonNode schema = tool.inputSchema();
        BetaManagedAgentsCustomToolInputSchema.Builder input = BetaManagedAgentsCustomToolInputSchema.builder();
        BetaManagedAgentsCustomToolInputSchema.Properties.Builder properties =
                BetaManagedAgentsCustomToolInputSchema.Properties.builder();
        schema.path("properties").properties()
                .forEach(property -> properties.putAdditionalProperty(property.getKey(), SdkEvents.sdkValue(property.getValue())));
        input.properties(properties.build());
        List<String> required = new ArrayList<>();
        schema.path("required").forEach(name -> required.add(name.asString()));
        input.required(required);
        // Anything else the schema says, such as additionalProperties, is passed through as it is.
        schema.properties().forEach(entry -> {
            if (!List.of("type", "properties", "required").contains(entry.getKey())) {
                input.putAdditionalProperty(entry.getKey(), SdkEvents.sdkValue(entry.getValue()));
            }
        });
        return BetaManagedAgentsCustomToolParams.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(input.build())
                .build();
    }

    // ================================================================ sessions

    @Override
    public String createSession(SessionSpec spec) {
        return call(() -> {
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .agent(BetaManagedAgentsAgentParams.builder()
                            .id(spec.agent().id())
                            .version(spec.agent().version())
                            .build())
                    .environmentId(spec.environmentId());
            if (spec.title() != null) {
                params.title(spec.title());
            }
            if (!spec.metadata().isEmpty()) {
                SessionCreateParams.Metadata.Builder metadata = SessionCreateParams.Metadata.builder();
                spec.metadata().forEach((key, value) -> metadata.putAdditionalProperty(key, JsonValue.from(value)));
                params.metadata(metadata.build());
            }
            if (!spec.vaultIds().isEmpty()) {
                params.vaultIds(spec.vaultIds());
            }
            return client.beta().sessions().create(params.build()).id();
        });
    }

    @Override
    public SessionInfo getSession(String sessionId) {
        return call(() -> {
            BetaManagedAgentsSession session = client.beta().sessions().retrieve(sessionId);
            SessionInfo.Status status = switch (session.status().asString()) {
                case "idle" -> SessionInfo.Status.IDLE;
                case "running" -> SessionInfo.Status.RUNNING;
                case "rescheduling" -> SessionInfo.Status.RESCHEDULING;
                case "terminated" -> SessionInfo.Status.TERMINATED;
                default -> SessionInfo.Status.UNKNOWN;
            };
            return new SessionInfo(session.id(), status, session.archivedAt().isPresent(),
                    new AgentRef(session.agent().id(), session.agent().version()),
                    SdkEvents.strings(session.metadata()._additionalProperties()));
        });
    }

    @Override
    public List<CmaEvent> listEvents(String sessionId, Instant createdSince) {
        return call(() -> {
            EventListParams.Builder params = EventListParams.builder()
                    .order(EventListParams.Order.ASC)
                    .limit(EVENTS_PER_PAGE);
            if (createdSince != null) {
                params.createdAtGte(OffsetDateTime.ofInstant(createdSince, ZoneOffset.UTC));
            }
            List<CmaEvent> events = new ArrayList<>();
            for (BetaManagedAgentsSessionEvent event
                    : client.beta().sessions().events().list(sessionId, params.build()).autoPager()) {
                events.add(SdkEvents.of(event));
            }
            return events;
        });
    }

    @Override
    public EventStream openStream(String sessionId) {
        return call(() -> new SdkStream(sessionId, streaming.beta().sessions().events().streamStreaming(sessionId)));
    }

    @Override
    public List<CmaEvent> send(String sessionId, List<Outgoing> events) {
        return call(() -> {
            EventSendParams.Builder params = EventSendParams.builder();
            for (Outgoing event : events) {
                switch (event) {
                    case Outgoing.UserMessage message -> params.addEvent(
                            BetaManagedAgentsUserMessageEventParams.builder().addTextContent(message.text()).build());
                    case Outgoing.Interrupt interrupt -> params.addEvent(
                            BetaManagedAgentsUserInterruptEventParams.builder().build());
                    case Outgoing.ToolConfirmation confirmation -> {
                        BetaManagedAgentsUserToolConfirmationEventParams.Builder answer =
                                BetaManagedAgentsUserToolConfirmationEventParams.builder()
                                        .toolUseId(confirmation.toolUseId())
                                        .result(confirmation.allow()
                                                ? BetaManagedAgentsUserToolConfirmationEventParams.Result.ALLOW
                                                : BetaManagedAgentsUserToolConfirmationEventParams.Result.DENY);
                        if (!confirmation.allow() && confirmation.denyMessage() != null) {
                            answer.denyMessage(confirmation.denyMessage());
                        }
                        params.addEvent(answer.build());
                    }
                    case Outgoing.CustomToolResult result -> params.addEvent(
                            BetaManagedAgentsUserCustomToolResultEventParams.builder()
                                    .customToolUseId(result.customToolUseId())
                                    .addTextContent(result.text())
                                    .isError(result.error())
                                    .build());
                }
            }
            BetaManagedAgentsSendSessionEvents sent = once.beta().sessions().events().send(sessionId, params.build());
            return sent.data().orElse(List.of()).stream().map(SdkEvents::of).toList();
        });
    }

    // ================================================================ errors

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (AnthropicException e) {
            throw translate(e);
        }
    }

    static CmaException translate(AnthropicException error) {
        if (error instanceof AnthropicServiceException response) {
            int status = response.statusCode();
            boolean overloaded = status == 529
                    || response.errorType().map(type -> "overloaded_error".equals(type.asString())).orElse(false);
            Kind kind = overloaded ? Kind.OVERLOADED : switch (status) {
                case 401, 403 -> Kind.UNAUTHORIZED;
                case 404 -> Kind.NOT_FOUND;
                case 409 -> Kind.CONFLICT;
                case 429 -> Kind.RATE_LIMITED;
                default -> status >= 500 ? Kind.SERVER : Kind.INVALID_REQUEST;
            };
            return new CmaException(kind, status, response.apiMessage().orElse(String.valueOf(response.getMessage())),
                    retryAfter(response), error);
        }
        // No response at all: a refused or broken connection, a timeout, or an answer that could not be read.
        return CmaException.connection(String.valueOf(error.getMessage()), error);
    }

    private static Duration retryAfter(AnthropicServiceException response) {
        for (String value : response.headers().values("retry-after")) {
            try {
                return Duration.ofMillis(Math.round(Double.parseDouble(value.trim()) * 1000));
            } catch (NumberFormatException e) {
                // an HTTP date is allowed here too; the caller then falls back to its own delay
            }
        }
        return null;
    }

    // ================================================================ the stream

    /**
     * The SDK's stream is a blocking iterator. A thread of its own drains it into a queue, so that
     * the reader can wait with a time limit and do other things in between.
     */
    private static final class SdkStream implements EventStream {

        private record End(Throwable cause) {
        }

        private final String sessionId;
        private final StreamResponse<BetaManagedAgentsStreamSessionEvents> response;
        private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        private volatile boolean closed;

        SdkStream(String sessionId, StreamResponse<BetaManagedAgentsStreamSessionEvents> response) {
            this.sessionId = sessionId;
            this.response = response;
            Thread.ofVirtual().name("cma-stream-" + sessionId).start(this::pump);
        }

        private void pump() {
            try {
                for (BetaManagedAgentsStreamSessionEvents event : response) {
                    CmaEvent converted = SdkEvents.of(event);
                    if (converted != null) {
                        queue.add(converted);
                    }
                }
                queue.add(new End(null));
            } catch (RuntimeException e) {
                queue.add(new End(e));
            }
        }

        @Override
        public Optional<CmaEvent> poll(Duration timeout) {
            if (closed) {
                throw CmaException.connection("the stream is closed", null);
            }
            Object next;
            try {
                next = queue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw CmaException.connection("interrupted while waiting for an event", e);
            }
            if (next instanceof End end) {
                closed = true;
                response.close();
                throw CmaException.connection("the stream of session " + sessionId
                        + (end.cause() == null ? " ended" : " was lost: " + end.cause().getMessage()), end.cause());
            }
            return Optional.ofNullable((CmaEvent) next);
        }

        @Override
        public void close() {
            closed = true;
            response.close();
        }
    }
}
