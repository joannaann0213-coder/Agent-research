package dev.agentflow.agent.cma.fake;

import dev.agentflow.agent.cma.CmaEvent.StopReason;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the scripted model does in one step: text, tool calls, or both. A step without a tool call
 * ends the turn; after a step with tool calls the model is asked again once their results are in.
 * A step can also end the turn the way the service ends one that went wrong.
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

    /**
     * The turn ends here, not because the agent is done: the service gave up after errors, the
     * session's budget is used up, or the model refused.
     */
    public record Stop(StopReason reason) implements Block {
    }

    public static ModelStep of(Block... blocks) {
        return new ModelStep(List.of(blocks));
    }

    /** A step that only writes text, which ends the turn. */
    public static ModelStep say(String text) {
        return of(new Text(text));
    }

    /** A step that ends the turn for {@code reason} instead of the usual end. */
    public static ModelStep stop(StopReason reason) {
        return of(new Stop(reason));
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
        return blocks.stream().anyMatch(block -> !(block instanceof Text) && !(block instanceof Stop));
    }

    Optional<StopReason> stop() {
        return blocks.stream().filter(Stop.class::isInstance).map(block -> ((Stop) block).reason()).findFirst();
    }
}
