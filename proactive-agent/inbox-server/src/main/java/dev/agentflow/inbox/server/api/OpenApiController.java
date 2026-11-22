package dev.agentflow.inbox.server.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the API specification of this service. It is not under {@code /v1}, so it needs no
 * signature: the specification says nothing that the documentation does not.
 */
@RestController
public class OpenApiController {

    /** Where the specification lives on the classpath. The tests check the service's traffic against it. */
    public static final String SPECIFICATION = "openapi/inbox.yaml";

    private final Resource specification = new ClassPathResource(SPECIFICATION);

    @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
    public Resource specification() {
        return specification;
    }
}
