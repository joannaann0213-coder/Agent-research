package dev.agentflow.agent.cma.fake;

import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the scripted model does in one step: text, tool calls, or both. A step without a tool call
 * ends the turn; after a step with tool calls the model is asked again once their results are in.
 */
public record ModelStep(List<Block> blocks) {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public ModelStep {
        blocks = List.copyOf(blocks);
    }

    public sealed interface Block {
    }

    /** Text the agent writes. */
    public record Text(String text) implements Block {
    }

    /** A call of a tool the caller runs. */
    public record Custom(String name, JsonNode input) implements Block {
    }

    /** A call of a tool of an MCP server. */
    public record Mcp(String server, String name, JsonNode input) implements Block {
    }

    /** A call of a built-in tool, such as {@code bash}. */
    public record Builtin(String name, JsonNode input) implements Block {
    }

    public static ModelStep of(Block... blocks) {
        return new ModelStep(List.of(blocks));
    }

    /** A step that only writes text, which ends the turn. */
    public static ModelStep say(String text) {
        return of(new Text(text));
    }

    public static Text text(String text) {
        return new Text(text);
    }

    public static Custom custom(String name, String inputJson) {
        return new Custom(name, JSON.readTree(inputJson));
    }

    public static Mcp mcp(String server, String name, String inputJson) {
        return new Mcp(server, name, JSON.readTree(inputJson));
    }

    public static Builtin builtin(String name, String inputJson) {
        return new Builtin(name, JSON.readTree(inputJson));
    }

    boolean callsTools() {
        return blocks.stream().anyMatch(block -> !(block instanceof Text));
    }
}
