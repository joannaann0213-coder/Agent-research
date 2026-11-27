package dev.agentflow.agent.cma;

import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * An event of a session, as the agent service needs to see it. Everything the service does not act
 * on arrives as {@link Other}.
 *
 * <p>An event that the caller sent shows up twice on a stream: first with {@code processedAt} null,
 * while it waits behind earlier work, then again with the same id once it has been processed.
 */
public sealed interface CmaEvent {

    /** The event's id. Empty for some interrupts, which the service stores without one. */
    String id();

    /** When the service finished processing the event; {@code null} while a sent event is still queued. */
    Instant processedAt();

    /** The wire name of the event type, for logs. */
    String type();

    // ---------------------------------------------------------------- what the caller sent

    record UserMessage(String id, Instant processedAt, String text) implements CmaEvent {
        @Override
        public String type() {
            return "user.message";
        }
    }

    record UserInterrupt(String id, Instant processedAt) implements CmaEvent {
        @Override
        public String type() {
            return "user.interrupt";
        }
    }

    /** The answer to a tool call that waited for permission. */
    record UserToolConfirmation(String id, Instant processedAt, String toolUseId, boolean allow) implements CmaEvent {
        @Override
        public String type() {
            return "user.tool_confirmation";
        }
    }

    /** The result of a custom tool call. */
    record UserCustomToolResult(String id, Instant processedAt, String customToolUseId) implements CmaEvent {
        @Override
        public String type() {
            return "user.custom_tool_result";
        }
    }

    // ---------------------------------------------------------------- what the agent did

    /** Text the agent wrote. */
    record AgentMessage(String id, Instant processedAt, String text) implements CmaEvent {
        @Override
        public String type() {
            return "agent.message";
        }
    }

    /**
     * A call of a tool that the platform runs: a built-in tool, or a tool of an MCP server.
     *
     * @param mcpServer the MCP server's name; {@code null} for a built-in tool
     * @param permission how the call was judged. {@link Permission#ASK} means the session now waits
     *     for a {@link Outgoing.ToolConfirmation} that names this event's id
     */
    record ToolUse(String id, Instant processedAt, String name, String mcpServer, JsonNode input, Permission permission)
            implements CmaEvent {
        @Override
        public String type() {
            return mcpServer == null ? "agent.tool_use" : "agent.mcp_tool_use";
        }
    }

    /**
     * The result of a call the platform ran.
     *
     * @param mcp whether the call went to an MCP server
     */
    record ToolResult(String id, Instant processedAt, String toolUseId, boolean error, boolean mcp) implements CmaEvent {
        @Override
        public String type() {
            return mcp ? "agent.mcp_tool_result" : "agent.tool_result";
        }
    }

    /**
     * A call of a tool that the caller runs. The session waits for a {@link Outgoing.CustomToolResult}
     * that names this event's id.
     */
    record CustomToolUse(String id, Instant processedAt, String name, JsonNode input) implements CmaEvent {
        @Override
        public String type() {
            return "agent.custom_tool_use";
        }
    }

    // ---------------------------------------------------------------- the session's state

    record Running(String id, Instant processedAt) implements CmaEvent {
        @Override
        public String type() {
            return "session.status_running";
        }
    }

    /**
     * The session stopped working.
     *
     * @param awaiting for {@link StopReason#REQUIRES_ACTION}, the ids of the tool calls that wait for
     *     an answer; empty otherwise
     */
    record Idle(String id, Instant processedAt, StopReason reason, List<String> awaiting) implements CmaEvent {
        @Override
        public String type() {
            return "session.status_idle";
        }
    }

    /** The session is being rescheduled after an error the platform retries. */
    record Rescheduled(String id, Instant processedAt) implements CmaEvent {
        @Override
        public String type() {
            return "session.status_rescheduled";
        }
    }

    /** The session has ended for good, by completion or by an error. */
    record Terminated(String id, Instant processedAt) implements CmaEvent {
        @Override
        public String type() {
            return "session.status_terminated";
        }
    }

    /**
     * Something went wrong inside the session. It is a report, not a state: what the session does
     * next shows in the events that follow.
     *
     * @param errorType the platform's name for the error, for example {@code model_overloaded_error}
     * @param retry what the platform does about it; {@code null} when it does not say
     */
    record Error(String id, Instant processedAt, String errorType, String message, Retry retry) implements CmaEvent {
        @Override
        public String type() {
            return "session.error";
        }

        public enum Retry {
            /** The platform is retrying on its own. */
            RETRYING,
            /** The platform gave up retrying. */
            EXHAUSTED,
            /** Not retried at all. */
            TERMINAL
        }
    }

    /**
     * A request to the model has ended, and this is what it used. A turn makes one for every step of
     * the agent: one before each batch of tool calls, and one for its last words.
     *
     * @param inputTokens input that was neither read from the cache nor written to it
     * @param cacheReadTokens input that was read from the cache
     * @param cacheCreationTokens input that was written to the cache
     */
    record ModelUsage(String id, Instant processedAt, int inputTokens, int outputTokens, int cacheReadTokens,
            int cacheCreationTokens) implements CmaEvent {
        @Override
        public String type() {
            return "span.model_request_end";
        }
    }

    /** Any event the agent service does not act on: thinking, the start of a span, and types added later. */
    record Other(String id, Instant processedAt, String type) implements CmaEvent {
    }

    // ---------------------------------------------------------------- vocabulary

    /** Why a session went idle. */
    enum StopReason {
        /** The agent finished its turn, or the turn was interrupted. */
        END_TURN,
        /** One or more tool calls wait for an answer from the caller. */
        REQUIRES_ACTION,
        /** The platform gave up after repeated errors. */
        RETRIES_EXHAUSTED,
        /** The session reached its spending cap. Only a change of the cap resumes it. */
        BUDGET_REACHED,
        /** The model declined to continue. */
        REFUSAL,
        /** A reason added after this code was written. */
        UNKNOWN
    }

    /** How the platform judged a tool call. */
    enum Permission {
        ALLOW,
        ASK,
        DENY
    }
}
