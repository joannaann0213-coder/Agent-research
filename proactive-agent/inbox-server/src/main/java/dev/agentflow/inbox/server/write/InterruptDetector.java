package dev.agentflow.inbox.server.write;

import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.server.config.InboxProperties;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * The mechanical half of "interrupt": a conversation message whose whole text is a stop word. Whether
 * the run should really stop is the agent's decision; the inbox only raises the flag.
 */
@Component
public class InterruptDetector {

    private final Set<String> keywords = new HashSet<>();
    private final String textPointer;

    public InterruptDetector(InboxProperties properties) {
        for (String keyword : properties.getInterrupt().getKeywords()) {
            keywords.add(normalise(keyword));
        }
        this.textPointer = properties.getInterrupt().getTextPointer();
    }

    public boolean isInterrupt(EventSource source, JsonNode payload) {
        if (source != EventSource.CONVERSATION || payload == null) {
            return false;
        }
        JsonNode text = payload.at(textPointer);
        return text.isString() && keywords.contains(normalise(text.stringValue()));
    }

    private static String normalise(String text) {
        return text.strip().toLowerCase(Locale.ROOT);
    }
}
