package dev.agentflow.agent.server.tools;

import dev.agentflow.agent.cma.AgentSpec;
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
 *   <li>As a main agent it hands on whatever the user writes after {@code task:} to the first of its
 *       workers, and tells the user what the worker reports. An event whose data has a text
 *       {@code "task"} makes it hand that on by itself, in the background: the task then waits for
 *       the user's confirmation, unless the role may start unasked. When the application hands an
 *       event to one of its workers itself, it takes note and says nothing.
 *   <li>As a worker it reads the task, or the event that the application handed it, and reports
 *       that it is done.
 * </ul>
 */
public final class DemoAgent implements FakeBrain {

    /** How the service's notice begins when a turn is not the user's. */
    private static final String UNATTENDED = "[From the service that runs you, not from the user. The user is not here";

    /** What the user writes to have a main agent hand the rest of the line to a worker. */
    private static final String HAND_ON = "task:";

    @Override
    public ModelStep next(ModelContext context) {
        String line = context.sessionMetadata().getOrDefault("line", "");
        if (line.startsWith("work:")) {
            return worker(context);
        }
        // A subject's line is always in the background; a conversation's is when the service says so.
        boolean background = line.startsWith("task:") || context.userMessage().contains(UNATTENDED);
        ToolOutcome read = context.resultOf(PlatformTools.READ_EVENTS);
        if (context.step() == 0 && (background || context.userMessage().contains("Call " + PlatformTools.READ_EVENTS))) {
            return ModelStep.of(ModelStep.custom(PlatformTools.READ_EVENTS, "{}"));
        }
        if (!background) {
            String said = context.userMessage().lines().reduce((first, second) -> second).orElse("");
            ToolOutcome handedOn = context.resultOf(PlatformTools.DELEGATE_TASK);
            if (handedOn != null) {
                return ModelStep.say(handedOn.error() ? "I could not hand that on. " + handedOn.text()
                        : "I have handed that to a worker (" + Json.STORED.readTree(handedOn.text()).path("task_id").asString()
                                + "). I will tell you what comes of it.");
            }
            String role = firstRole(context.agent());
            if (read == null && context.step() == 0 && role != null && said.regionMatches(true, 0, HAND_ON, 0, HAND_ON.length())) {
                String what = said.substring(HAND_ON.length()).strip();
                ObjectNode input = Json.STORED.createObjectNode();
                input.put("role", role).put("title", what.length() <= 60 ? what : what.substring(0, 60)).put("instructions", what);
                return ModelStep.of(ModelStep.custom(PlatformTools.DELEGATE_TASK, input.toString()));
            }
            if (read == null) {
                return ModelStep.say("You said: " + said);
            }
            List<String> told = new ArrayList<>();
            Json.STORED.readTree(read.text()).path("messages").forEach(one -> told.add(one.path("text").asString()));
            return ModelStep.say("You said: " + said + " (Since we last spoke you were told: " + String.join(" / ", told) + ")");
        }
        if (read == null) {
            ToolOutcome sent = context.resultOf(PlatformTools.NOTIFY_USER);
            ToolOutcome begun = context.resultOf(PlatformTools.DELEGATE_TASK);
            if (sent == null && begun != null) {
                return ModelStep.say(begun.error() ? "Could not hand that on. " + begun.text()
                        : "Handed on by myself: " + Json.STORED.readTree(begun.text()).path("status").asString() + ".");
            }
            return ModelStep.say(sent == null ? "Nothing to do." : "Notification: " + sent.text());
        }
        JsonNode waiting = Json.STORED.readTree(read.text());
        String ownRole = firstRole(context.agent());
        List<String> worthTelling = new ArrayList<>();
        String toDo = null;
        for (JsonNode event : waiting.path("events")) {
            String task = event.path("data").path("task").asString(null);
            if (ownRole != null && task != null && !task.isBlank()) {
                // Work that the main agent takes up by itself: the first such event of the turn.
                toDo = toDo == null ? task.strip() : toDo;
            } else if (!event.path("data").path("silent").asBoolean(false)) {
                worthTelling.add(event.path("type").asString() + " (" + event.path("subject").asString() + ")");
            }
        }
        List<String> tell = new ArrayList<>();
        if (!worthTelling.isEmpty()) {
            tell.add("[demo] Something happened that you should know about: " + String.join(", ", worthTelling) + ".");
        }
        for (JsonNode report : waiting.path("reports")) {
            if ("assigned".equals(report.path("kind").asString())) {
                // The application handed a worker an event itself. The main agent knows now; the worker's report will follow.
                continue;
            }
            tell.add("[demo] Your task \"" + report.path("title").asString() + "\" is back from the "
                    + report.path("role").asString() + ": " + report.path("text").asString());
        }
        List<ModelStep.Block> calls = new ArrayList<>();
        if (!tell.isEmpty()) {
            ObjectNode input = Json.STORED.createObjectNode();
            input.put("text", String.join(" ", tell));
            calls.add(ModelStep.custom(PlatformTools.NOTIFY_USER, input.toString()));
        }
        if (toDo != null) {
            ObjectNode input = Json.STORED.createObjectNode();
            input.put("role", ownRole).put("title", toDo.length() <= 60 ? toDo : toDo.substring(0, 60)).put("instructions", toDo)
                    .put("summary", "[demo] I can do this for you: " + (toDo.length() <= 400 ? toDo : toDo.substring(0, 400)));
            calls.add(ModelStep.custom(PlatformTools.DELEGATE_TASK, input.toString()));
        }
        if (calls.isEmpty()) {
            return ModelStep.say("Nothing the user needs to hear about.");
        }
        return ModelStep.of(calls.toArray(ModelStep.Block[]::new));
    }

    /** A worker's turn: read what the main agent wants, and report that it is done. */
    private static ModelStep worker(ModelContext context) {
        if (context.step() == 0) {
            return ModelStep.of(ModelStep.custom(PlatformTools.READ_EVENTS, "{}"));
        }
        ToolOutcome read = context.resultOf(PlatformTools.READ_EVENTS);
        if (read == null) {
            return ModelStep.say("[demo] Nothing more to report.");
        }
        String asked = "";
        String handled = null;
        for (JsonNode message : Json.STORED.readTree(read.text()).path("messages")) {
            if (message.has("event")) {
                // An event that the application handed to this worker itself.
                handled = message.path("event").path("type").asString() + " (" + message.path("event").path("subject").asString() + ")";
            } else {
                asked = message.path("text").asString();
                handled = null;
            }
        }
        return ModelStep.say(handled != null ? "[demo] Done. I handled the event " + handled + "."
                : "[demo] Done. You asked for: " + asked);
    }

    /** The first role a main agent can delegate to, as its delegation tool names them; {@code null} for an agent without workers. */
    private static String firstRole(AgentSpec agent) {
        return agent.customTools().stream().filter(tool -> tool.name().equals(PlatformTools.DELEGATE_TASK)).findFirst()
                .map(tool -> tool.inputSchema().path("properties").path("role").path("enum").path(0).asString(null))
                .orElse(null);
    }
}
