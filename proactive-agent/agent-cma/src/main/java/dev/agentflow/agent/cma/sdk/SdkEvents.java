package dev.agentflow.agent.cma.sdk;

import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentCustomToolUseEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentEvaluatedPermission;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentMcpToolResultEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentMcpToolUseEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentMessageEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentToolResultEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentToolUseEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionErrorEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionStatusIdleEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsStreamSessionEvents;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import dev.agentflow.agent.cma.CmaEvent;
import dev.agentflow.agent.cma.CmaEvent.Permission;
import dev.agentflow.agent.cma.CmaEvent.StopReason;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns the SDK's event types into the port's. The SDK has three unions for events (stored, streamed
 * and just sent) over the same event classes, so there is one converter per event class and one
 * dispatcher per union.
 */
final class SdkEvents {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private SdkEvents() {
    }

    // ---------------------------------------------------------------- the three unions

    /** An event from the stored list. */
    static CmaEvent of(BetaManagedAgentsSessionEvent event) {
        if (event.isUserMessage()) {
            return of(event.asUserMessage());
        } else if (event.isUserInterrupt()) {
            return of(event.asUserInterrupt());
        } else if (event.isUserToolConfirmation()) {
            return of(event.asUserToolConfirmation());
        } else if (event.isUserCustomToolResult()) {
            return of(event.asUserCustomToolResult());
        } else if (event.isAgentMessage()) {
            return of(event.asAgentMessage());
        } else if (event.isAgentToolUse()) {
            return of(event.asAgentToolUse());
        } else if (event.isAgentMcpToolUse()) {
            return of(event.asAgentMcpToolUse());
        } else if (event.isAgentToolResult()) {
            return of(event.asAgentToolResult());
        } else if (event.isAgentMcpToolResult()) {
            return of(event.asAgentMcpToolResult());
        } else if (event.isAgentCustomToolUse()) {
            return of(event.asAgentCustomToolUse());
        } else if (event.isSessionStatusIdle()) {
            return of(event.asSessionStatusIdle());
        } else if (event.isSessionStatusRunning()) {
            return new CmaEvent.Running(event.asSessionStatusRunning().id(), instant(event.asSessionStatusRunning().processedAt()));
        } else if (event.isSessionStatusRescheduled()) {
            return new CmaEvent.Rescheduled(event.asSessionStatusRescheduled().id(),
                    instant(event.asSessionStatusRescheduled().processedAt()));
        } else if (event.isSessionStatusTerminated()) {
            return new CmaEvent.Terminated(event.asSessionStatusTerminated().id(),
                    instant(event.asSessionStatusTerminated().processedAt()));
        } else if (event.isSessionError()) {
            return of(event.asSessionError());
        }
        return other(event._json());
    }

    /**
     * An event from the live stream.
     *
     * @return {@code null} for the preview events, which only announce an event that follows
     */
    static CmaEvent of(BetaManagedAgentsStreamSessionEvents event) {
        if (event.isEventStart() || event.isEventDelta()) {
            return null;
        } else if (event.isUserMessage()) {
            return of(event.asUserMessage());
        } else if (event.isUserInterrupt()) {
            return of(event.asUserInterrupt());
        } else if (event.isUserToolConfirmation()) {
            return of(event.asUserToolConfirmation());
        } else if (event.isUserCustomToolResult()) {
            return of(event.asUserCustomToolResult());
        } else if (event.isAgentMessage()) {
            return of(event.asAgentMessage());
        } else if (event.isAgentToolUse()) {
            return of(event.asAgentToolUse());
        } else if (event.isAgentMcpToolUse()) {
            return of(event.asAgentMcpToolUse());
        } else if (event.isAgentToolResult()) {
            return of(event.asAgentToolResult());
        } else if (event.isAgentMcpToolResult()) {
            return of(event.asAgentMcpToolResult());
        } else if (event.isAgentCustomToolUse()) {
            return of(event.asAgentCustomToolUse());
        } else if (event.isSessionStatusIdle()) {
            return of(event.asSessionStatusIdle());
        } else if (event.isSessionStatusRunning()) {
            return new CmaEvent.Running(event.asSessionStatusRunning().id(), instant(event.asSessionStatusRunning().processedAt()));
        } else if (event.isSessionStatusRescheduled()) {
            return new CmaEvent.Rescheduled(event.asSessionStatusRescheduled().id(),
                    instant(event.asSessionStatusRescheduled().processedAt()));
        } else if (event.isSessionStatusTerminated()) {
            return new CmaEvent.Terminated(event.asSessionStatusTerminated().id(),
                    instant(event.asSessionStatusTerminated().processedAt()));
        } else if (event.isSessionError()) {
            return of(event.asSessionError());
        }
        return other(event._json());
    }

    /** An event as the service stored it when it was sent. */
    static CmaEvent of(BetaManagedAgentsSendSessionEvents.Data event) {
        if (event.isUserMessage()) {
            return of(event.asUserMessage());
        } else if (event.isUserInterrupt()) {
            return of(event.asUserInterrupt());
        } else if (event.isUserToolConfirmation()) {
            return of(event.asUserToolConfirmation());
        } else if (event.isUserCustomToolResult()) {
            return of(event.asUserCustomToolResult());
        }
        return new CmaEvent.Other(event.id(), instant(event.processedAt()), event.type().toString());
    }

    // ---------------------------------------------------------------- one converter per event class

    private static CmaEvent of(BetaManagedAgentsUserMessageEvent event) {
        String text = event.content().stream()
                .filter(BetaManagedAgentsUserMessageEvent.Content::isText)
                .map(block -> block.asText().text())
                .collect(Collectors.joining("\n"));
        return new CmaEvent.UserMessage(event.id(), instant(event.processedAt()), text);
    }

    private static CmaEvent of(BetaManagedAgentsUserInterruptEvent event) {
        return new CmaEvent.UserInterrupt(event.id(), instant(event.processedAt()));
    }

    private static CmaEvent of(BetaManagedAgentsUserToolConfirmationEvent event) {
        boolean allow = BetaManagedAgentsUserToolConfirmationEvent.Result.ALLOW.equals(event.result());
        return new CmaEvent.UserToolConfirmation(event.id(), instant(event.processedAt()), event.toolUseId(), allow);
    }

    private static CmaEvent of(BetaManagedAgentsUserCustomToolResultEvent event) {
        return new CmaEvent.UserCustomToolResult(event.id(), instant(event.processedAt()), event.customToolUseId());
    }

    private static CmaEvent of(BetaManagedAgentsAgentMessageEvent event) {
        String text = event.content().stream()
                .filter(BetaManagedAgentsAgentMessageEvent.Content::isText)
                .map(block -> block.asText().text())
                .collect(Collectors.joining());
        return new CmaEvent.AgentMessage(event.id(), instant(event.processedAt()), text);
    }

    private static CmaEvent of(BetaManagedAgentsAgentToolUseEvent event) {
        return new CmaEvent.ToolUse(event.id(), instant(event.processedAt()), event.name(), null,
                json(event.input()._additionalProperties()), permission(event.evaluatedPermission()));
    }

    private static CmaEvent of(BetaManagedAgentsAgentMcpToolUseEvent event) {
        return new CmaEvent.ToolUse(event.id(), instant(event.processedAt()), event.name(), event.mcpServerName(),
                json(event.input()._additionalProperties()), permission(event.evaluatedPermission()));
    }

    private static CmaEvent of(BetaManagedAgentsAgentToolResultEvent event) {
        return new CmaEvent.ToolResult(event.id(), instant(event.processedAt()), event.toolUseId(),
                event.isError().orElse(false), false);
    }

    private static CmaEvent of(BetaManagedAgentsAgentMcpToolResultEvent event) {
        return new CmaEvent.ToolResult(event.id(), instant(event.processedAt()), event.mcpToolUseId(),
                event.isError().orElse(false), true);
    }

    private static CmaEvent of(BetaManagedAgentsAgentCustomToolUseEvent event) {
        return new CmaEvent.CustomToolUse(event.id(), instant(event.processedAt()), event.name(),
                json(event.input()._additionalProperties()));
    }

    private static CmaEvent of(BetaManagedAgentsSessionStatusIdleEvent event) {
        BetaManagedAgentsSessionStatusIdleEvent.StopReason reason = event.stopReason();
        if (reason.isRequiresAction()) {
            return new CmaEvent.Idle(event.id(), instant(event.processedAt()), StopReason.REQUIRES_ACTION,
                    List.copyOf(reason.asRequiresAction().eventIds()));
        }
        StopReason mapped = reason.isEndTurn() ? StopReason.END_TURN
                : reason.isRetriesExhausted() ? StopReason.RETRIES_EXHAUSTED
                : reason.isBudgetReached() ? StopReason.BUDGET_REACHED
                : reason.isRefusal() ? StopReason.REFUSAL
                : StopReason.UNKNOWN;
        return new CmaEvent.Idle(event.id(), instant(event.processedAt()), mapped, List.of());
    }

    private static CmaEvent of(BetaManagedAgentsSessionErrorEvent event) {
        // The error is a union of some fifteen types. Its type, message and retry status are read from
        // the raw value, so that an error type added later is still reported.
        JsonNode error = event.error()._json().map(SdkEvents::json).orElse(JSON.createObjectNode());
        String retry = error.path("retry_status").path("type").asString("");
        return new CmaEvent.Error(event.id(), instant(event.processedAt()), error.path("type").asString("unknown_error"),
                error.path("message").asString(""), switch (retry) {
                    case "retrying" -> CmaEvent.Error.Retry.RETRYING;
                    case "exhausted" -> CmaEvent.Error.Retry.EXHAUSTED;
                    case "terminal" -> CmaEvent.Error.Retry.TERMINAL;
                    default -> null;
                });
    }

    /** An event this service does not act on. Its id, time and type are read from the raw value. */
    private static CmaEvent other(Optional<JsonValue> raw) {
        JsonNode event = raw.map(SdkEvents::json).orElse(JSON.createObjectNode());
        String processedAt = event.path("processed_at").asString("");
        return new CmaEvent.Other(event.path("id").asString(""),
                processedAt.isEmpty() ? null : OffsetDateTime.parse(processedAt).toInstant(),
                event.path("type").asString("unknown"));
    }

    // ---------------------------------------------------------------- helpers

    private static Permission permission(Optional<BetaManagedAgentsAgentEvaluatedPermission> evaluated) {
        return evaluated.map(value -> switch (value.asString()) {
            case "allow" -> Permission.ALLOW;
            case "ask" -> Permission.ASK;
            case "deny" -> Permission.DENY;
            default -> null;
        }).orElse(null);
    }

    private static Instant instant(OffsetDateTime time) {
        return time == null ? null : time.toInstant();
    }

    private static Instant instant(Optional<OffsetDateTime> time) {
        return time.map(OffsetDateTime::toInstant).orElse(null);
    }

    /** The SDK reads JSON with Jackson 2; the rest of this code base uses Jackson 3. The text form is the bridge. */
    static JsonNode json(Object sdkValue) {
        try {
            return JSON.readTree(ObjectMappers.jsonMapper().writeValueAsString(sdkValue));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("a value of the SDK cannot be written as JSON", e);
        }
    }

    /** The other direction: a JSON value of this code base as the SDK's. */
    static JsonValue sdkValue(JsonNode node) {
        try {
            return JsonValue.fromJsonNode(ObjectMappers.jsonMapper().readTree(JSON.writeValueAsString(node)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("a JSON value cannot be read by the SDK", e);
        }
    }

    /** A free-form map of the SDK as plain strings; a value that is not a string is given as its JSON text. */
    static Map<String, String> strings(Map<String, JsonValue> values) {
        Map<String, String> strings = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            JsonNode node = json(value);
            strings.put(key, node.isString() ? node.asString() : node.toString());
        });
        return Map.copyOf(strings);
    }
}
