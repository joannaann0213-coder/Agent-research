package dev.agentflow.testkit.openapi;

import java.util.Set;
import java.util.TreeSet;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Reads what a Spring MVC application maps, in the form {@link OpenApiTraffic#operations()} uses. */
public final class SpringMappings {

    private SpringMappings() {
    }

    /**
     * Every controller mapping whose path starts with {@code pathPrefix}, as {@code METHOD /path/{}}.
     * A mapping that is not limited to a method is reported as {@code ANY /path}; a specification
     * cannot describe that, so it shows up as a difference.
     */
    public static Set<String> under(String pathPrefix, RequestMappingHandlerMapping mappings) {
        Set<String> operations = new TreeSet<>();
        mappings.getHandlerMethods().forEach((info, handler) -> {
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith(pathPrefix)) {
                    continue;
                }
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    operations.add("ANY " + OpenApiTraffic.normalise(pattern));
                }
                for (RequestMethod method : methods) {
                    operations.add(method.name() + " " + OpenApiTraffic.normalise(pattern));
                }
            }
        });
        return operations;
    }
}
