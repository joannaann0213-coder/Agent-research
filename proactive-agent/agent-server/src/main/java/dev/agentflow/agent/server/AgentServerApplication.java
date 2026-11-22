package dev.agentflow.agent.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentServerApplication {

    /** The name of the service's configuration file, without the extension. See {@code agent-server.yml}. */
    public static final String CONFIG_NAME = "agent-server";

    public static void main(String[] args) {
        new SpringApplicationBuilder(AgentServerApplication.class)
                .properties("spring.config.name=" + CONFIG_NAME)
                .run(args);
    }
}
