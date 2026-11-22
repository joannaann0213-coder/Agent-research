package dev.agentflow.agent.server.api;

import dev.agentflow.agent.api.Whoami;
import dev.agentflow.agent.server.auth.Caller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tells a caller who the service takes it for: the quickest check that a key or a token works. */
@RestController
public class WhoamiController {

    @GetMapping("/v1/whoami")
    public Whoami whoami(Caller caller) {
        return new Whoami(caller.kind().name().toLowerCase(), caller.appId(), caller.userId());
    }
}
