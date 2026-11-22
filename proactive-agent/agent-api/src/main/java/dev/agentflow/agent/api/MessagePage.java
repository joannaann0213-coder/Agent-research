package dev.agentflow.agent.api;

import java.util.List;

/**
 * Messages in the order of their positions.
 *
 * @param nextAfter the value to pass as {@code after} to get what comes next: the last position on
 *     this page, or the {@code after} that was asked with when the page is empty
 */
public record MessagePage(List<MessageView> messages, long nextAfter) {
}
