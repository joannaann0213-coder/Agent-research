package dev.agentflow.agent.api;

import java.util.List;

/** The tasks of a user, newest first. */
public record TaskPage(List<TaskView> tasks) {
}
