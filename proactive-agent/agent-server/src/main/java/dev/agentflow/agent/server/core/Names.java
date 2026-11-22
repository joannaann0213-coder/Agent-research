package dev.agentflow.agent.server.core;

import dev.agentflow.agent.server.api.AgentException;
import java.util.regex.Pattern;

/**
 * The shapes of the names that callers choose. They end up in keys, in paths and in the Event Inbox,
 * so they are checked once, at the edge.
 */
public final class Names {

    /** An application id. It is also a tenant id in the Event Inbox. */
    public static final Pattern APP_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{1,39}");

    /** A user id. It is also an inbox id in the Event Inbox, whose pattern this is. */
    public static final Pattern USER_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:@-]{0,127}");

    private Names() {
    }

    public static String appId(String value) {
        return check(value, APP_ID, "app_id", "2 to 40 characters of a-z, 0-9, _ and -, starting with a letter or a digit");
    }

    public static String userId(String value) {
        return check(value, USER_ID, "the user id", "1 to 128 characters of letters, digits and _ . : @ -, starting with a letter or a digit");
    }

    private static String check(String value, Pattern pattern, String what, String rule) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw AgentException.invalid(what + " must be " + rule);
        }
        return value;
    }
}
