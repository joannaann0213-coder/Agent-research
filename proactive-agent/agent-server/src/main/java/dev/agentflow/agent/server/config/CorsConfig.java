package dev.agentflow.agent.server.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Lets web pages of the configured origins call the API from the browser. The browser asks first,
 * with a preflight that carries no credential; it is answered here, before the authentication filter,
 * which would refuse it. Answers, refusals included, then carry the headers that let the page read
 * them. Without configured origins nothing of this is in the way, and a browser reaches the API only
 * from its own origin.
 */
@Configuration
public class CorsConfig {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(AgentProperties properties) {
        List<String> origins = properties.getCors().getAllowedOrigins().stream().filter(origin -> !origin.isBlank()).toList();
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Last-Event-ID"));
        cors.setExposedHeaders(List.of("Retry-After"));
        cors.setMaxAge(Duration.ofMinutes(10));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/v1/**", cors);
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.addUrlPatterns("/v1/*");
        // Before the authentication filter (HIGHEST_PRECEDENCE + 50).
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 40);
        registration.setEnabled(!origins.isEmpty());
        return registration;
    }
}
