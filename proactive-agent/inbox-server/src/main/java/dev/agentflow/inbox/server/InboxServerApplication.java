package dev.agentflow.inbox.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InboxServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InboxServerApplication.class, args);
    }
}
