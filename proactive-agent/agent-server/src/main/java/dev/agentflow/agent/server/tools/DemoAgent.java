package dev.agentflow.agent.server.tools;

import dev.agentflow.agent.cma.fake.FakeBrain;
import dev.agentflow.agent.cma.fake.ModelContext;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.fake.ToolOutcome;
import dev.agentflow.agent.server.core.Json;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The agent of demo mode: a script that walks the paths of the service so that they can be tried
 * without a model. It understands nothing.
 *
 * <ul>
 *   <li>In a conversation it repeats what the user said. If the service says that the user was sent
 *       notifications, it reads them first and says so.
 *   <li>In the background it reads the events and tells the user about each one, in fixed words,
 *       except those whose data has {@code "silent": true}.
 * </ul>
 */
public final class DemoAgent implements FakeBrain {

    @Override
    public ModelStep next(ModelContext context) {
        boolean background = context.sessionMetadata().getOrDefault("line", "").startsWith("task:");
        ToolOutcome read = context.resultOf(PlatformTools.READ_EVENTS);
        if (context.step() == 0 && (background || context.userMessage().contains("Call " + PlatformTools.READ_EVENTS))) {
            return ModelStep.of(ModelStep.custom(PlatformTools.READ_EVENTS, "{}"));
        }
        if (!background) {
            String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
            if (read == null) {
                return ModelStep.say("You said: " + said);
            }
            List<String> told = new ArrayList<>();
            Json.STORED.readTree(read.text()).path("messages").forEach(one -> told.add(one.path("text").asString()));
            return ModelStep.say("You said: " + said + " (Since we last spoke you were told: " + String.join(" / ", told) + ")");
        }
        if (read == null) {
            ToolOutcome sent = context.resultOf(PlatformTools.NOTIFY_USER);
            return ModelStep.say(sent == null ? "Nothing to do." : "Notification: " + sent.text());
        }
        List<String> worthTelling = new ArrayList<>();
        for (JsonNode event : Json.STORED.readTree(read.text()).path("events")) {
            if (!event.path("data").path("silent").asBoolean(false)) {
                worthTelling.add(event.path("type").asString() + " (" + event.path("subject").asString() + ")");
            }
        }
        if (worthTelling.isEmpty()) {
            return ModelStep.say("Nothing the user needs to hear about.");
        }
        ObjectNode input = Json.STORED.createObjectNode();
        input.put("text", "[demo] Something happened that you should know about: " + String.join(", ", worthTelling) + ".");
        return ModelStep.of(ModelStep.custom(PlatformTools.NOTIFY_USER, input.toString()));
    }
}
