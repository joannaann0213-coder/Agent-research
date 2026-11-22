package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.CreateScheduleRequest;
import dev.agentflow.agent.api.SchedulePage;
import dev.agentflow.agent.api.ScheduleView;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.schedules.Schedules;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the agent is to do for a user at a given time. Only the application's back end sets a
 * schedule up, because the agent takes its instructions as the application's; the user's own client
 * may see the user's schedules and cancel them.
 */
@RestController
@RequestMapping("/v1/users/{userId}/schedules")
public class ScheduleController {

    private final Schedules schedules;

    public ScheduleController(Schedules schedules) {
        this.schedules = schedules;
    }

    @PostMapping
    public ResponseEntity<ScheduleView> create(Caller caller, @PathVariable String userId, @RequestBody CreateScheduleRequest request) {
        Schedules.Created created = schedules.create(caller.requireApp(), userId, request);
        return ResponseEntity.status(created.created() ? HttpStatus.CREATED : HttpStatus.OK).body(created.schedule());
    }

    @GetMapping
    public SchedulePage list(Caller caller, @PathVariable String userId) {
        return new SchedulePage(schedules.active(caller.requireAppOrUser(userId), userId));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> cancel(Caller caller, @PathVariable String userId, @PathVariable String scheduleId) {
        schedules.cancel(caller.requireAppOrUser(userId), userId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
