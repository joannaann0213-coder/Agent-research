package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.StatsView;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.stats.Stats;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the agents did, and what it used: for one user, and for all users of the application. For
 * the application's back end, which sets how much its agents do unasked and wants to see what that
 * comes to.
 */
@RestController
public class StatsController {

    private final Stats stats;

    public StatsController(Stats stats) {
        this.stats = stats;
    }

    @GetMapping("/v1/stats")
    public StatsView ofApplication(Caller caller, @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        return stats.of(caller.requireApp(), null, since, until);
    }

    @GetMapping("/v1/users/{userId}/stats")
    public StatsView ofUser(Caller caller, @PathVariable String userId, @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        return stats.of(caller.requireApp(), userId, since, until);
    }
}
