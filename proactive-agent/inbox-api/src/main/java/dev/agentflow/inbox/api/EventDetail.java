package dev.agentflow.inbox.api;

import java.util.List;

/** An event together with its full transition history, oldest first. */
public record EventDetail(EventView event, List<TransitionView> transitions) {
}
