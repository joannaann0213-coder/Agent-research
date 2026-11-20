package dev.agentflow.inbox.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Deterministic text form of a JSON value: object keys sorted, no insignificant whitespace.
 *
 * <p>Two payloads that differ only in key order or spacing canonicalise to the same text, so a
 * producer that re-serialises a retry does not trip the "same key, different payload" check.
 * Numbers are rendered the way Jackson parses them: {@code 1.50} and {@code 1.5} are the same value,
 * while the integer {@code 1} and the decimal {@code 1.0} stay different.
 */
public final class CanonicalJson {

    private CanonicalJson() {
    }

    public static String of(JsonNode node) {
        StringBuilder out = new StringBuilder();
        write(node, out);
        return out.toString();
    }

    private static void write(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            out.append("null");
        } else if (node.isObject()) {
            List<String> names = new ArrayList<>(node.propertyNames());
            Collections.sort(names);
            out.append('{');
            boolean first = true;
            for (String name : names) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                quote(name, out);
                out.append(':');
                write(node.get(name), out);
            }
            out.append('}');
        } else if (node.isArray()) {
            out.append('[');
            boolean first = true;
            for (JsonNode item : node.values()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(item, out);
            }
            out.append(']');
        } else if (node.isString()) {
            quote(node.stringValue(), out);
        } else {
            // numbers and booleans: Jackson's own compact rendering
            out.append(node.toString());
        }
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
