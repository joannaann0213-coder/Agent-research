package dev.agentflow.agent.api;

import java.util.List;

/** The user's active schedules, oldest first. */
public record SchedulePage(List<ScheduleView> schedules) {
}
