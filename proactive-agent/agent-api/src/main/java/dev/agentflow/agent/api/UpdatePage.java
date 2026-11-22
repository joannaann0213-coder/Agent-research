package dev.agentflow.agent.api;

import java.util.List;

/**
 * @param nextAfter what to pass as {@code after} to get what comes next: the last {@code seq} of this
 *     page, or the {@code after} asked with when the page is empty
 */
public record UpdatePage(List<Update> updates, long nextAfter) {
}
