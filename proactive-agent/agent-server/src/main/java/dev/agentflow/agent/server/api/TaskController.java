package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.TaskPage;
import dev.agentflow.agent.api.TaskView;
import dev.agentflow.agent.server.auth.Caller;
import dev.agentflow.agent.server.tasks.Tasks;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The work a user's main agent has handed to workers. People do not start a task, the main agent
 * does; they can see what is under way and stop it. The application's back end also sees what the
 * worker reported and what the two agents told each other; a user's own client does not: the user
 * hears of a worker only what the main agent tells them, and the worker may have read what only the
 * application is to see.
 */
@RestController
@RequestMapping("/v1/users/{userId}/tasks")
public class TaskController {

    private final Tasks tasks;

    public TaskController(Tasks tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public TaskPage list(Caller caller, @PathVariable String userId,
            @RequestParam(name = "conversation_id", required = false) String conversationId,
            @RequestParam(name = "status", defaultValue = "open") String status) {
        String appId = caller.requireAppOrUser(userId);
        if (!status.equals("open") && !status.equals("all")) {
            throw AgentException.invalid("status is open, for the tasks that are not closed, or all");
        }
        return tasks.list(appId, userId, conversationId, status.equals("open"), 200, caller.kind() == Caller.Kind.APP);
    }

    @GetMapping("/{taskId}")
    public TaskView get(Caller caller, @PathVariable String userId, @PathVariable String taskId) {
        return tasks.get(caller.requireAppOrUser(userId), userId, taskId, caller.kind() == Caller.Kind.APP);
    }

    /** Closing a task that is closed already changes nothing, and answers with the task as it is. */
    @PostMapping("/{taskId}/close")
    public TaskView close(Caller caller, @PathVariable String userId, @PathVariable String taskId) {
        String appId = caller.requireAppOrUser(userId);
        return tasks.close(appId, userId, taskId, caller.kind() == Caller.Kind.USER ? Tasks.BY_USER : Tasks.BY_APP, null);
    }
}
