package dev.agentflow.testkit.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks an OpenAPI specification for what a reader, or a code generator, would trip over. The
 * parser accepts all of these:
 *
 * <ul>
 *   <li>a reference to a component that is not defined, and a component nothing refers to;
 *   <li>an operation without an {@code operationId}, a summary or a declared tag, and an
 *       {@code operationId} used twice;
 *   <li>a path variable without a required path parameter, and a path parameter without a variable;
 *   <li>an operation without a success response;
 *   <li>a security requirement that names no defined scheme, and a scheme nothing requires;
 *   <li>an example that does not match the schema it illustrates.
 * </ul>
 */
public final class OpenApiLint {

    private static final Pattern REFERENCE = Pattern.compile("#/components/([A-Za-z]+)/([A-Za-z0-9._-]+)");
    private static final Pattern VARIABLE = Pattern.compile("\\{([^/}]+)}");

    private OpenApiLint() {
    }

    /** Fails with every problem found. */
    public static void assertClean(String name, String specification) {
        List<String> problems = problems(specification);
        if (!problems.isEmpty()) {
            throw new AssertionError("the specification " + name + " has " + problems.size() + " problem(s):\n  "
                    + String.join("\n  ", problems));
        }
    }

    /** Everything that is wrong with the specification; empty when it is clean. */
    public static List<String> problems(String specification) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parsed = new OpenAPIV3Parser().readContents(specification, null, options);
        List<String> problems = new ArrayList<>();
        if (parsed.getMessages() != null) {
            problems.addAll(parsed.getMessages());
        }
        OpenAPI api = parsed.getOpenAPI();
        if (api == null || api.getPaths() == null) {
            problems.add("the specification cannot be read or has no paths");
            return problems;
        }
        references(specification, api, problems);
        operations(api, problems);
        security(api, problems);
        if (problems.isEmpty()) {
            // Examples can only be checked against a specification that is otherwise sound.
            examples(specification, api, problems);
        }
        return problems;
    }

    // ---------------------------------------------------------------- references

    private static void references(String specification, OpenAPI api, List<String> problems) {
        Set<String> used = new HashSet<>();
        Matcher reference = REFERENCE.matcher(specification);
        while (reference.find()) {
            used.add(reference.group(1) + "/" + reference.group(2));
            Map<String, ?> section = section(api.getComponents(), reference.group(1));
            if (section == null || !section.containsKey(reference.group(2))) {
                problems.add("a reference points at a component that is not defined: " + reference.group(0));
            }
        }
        for (String name : List.of("schemas", "responses", "parameters", "requestBodies", "headers", "examples")) {
            Map<String, ?> section = section(api.getComponents(), name);
            if (section == null) {
                continue;
            }
            for (String component : new TreeSet<>(section.keySet())) {
                if (!used.contains(name + "/" + component)) {
                    problems.add("nothing refers to the component #/components/" + name + "/" + component);
                }
            }
        }
    }

    static Map<String, ?> section(Components components, String name) {
        if (components == null) {
            return null;
        }
        return switch (name) {
            case "schemas" -> components.getSchemas();
            case "responses" -> components.getResponses();
            case "parameters" -> components.getParameters();
            case "examples" -> components.getExamples();
            case "requestBodies" -> components.getRequestBodies();
            case "headers" -> components.getHeaders();
            case "securitySchemes" -> components.getSecuritySchemes();
            case "links" -> components.getLinks();
            case "callbacks" -> components.getCallbacks();
            default -> null;
        };
    }

    // ---------------------------------------------------------------- operations

    private static void operations(OpenAPI api, List<String> problems) {
        Set<String> declaredTags = new HashSet<>();
        if (api.getTags() != null) {
            api.getTags().stream().map(Tag::getName).forEach(declaredTags::add);
        }
        Set<String> ids = new HashSet<>();
        api.getPaths().forEach((template, item) -> item.readOperationsMap().forEach((method, operation) -> {
            String where = method + " " + template;
            if (operation.getOperationId() == null || operation.getOperationId().isBlank()) {
                problems.add(where + " has no operationId");
            } else if (!ids.add(operation.getOperationId())) {
                problems.add("the operationId " + operation.getOperationId() + " is used more than once");
            }
            if (operation.getSummary() == null || operation.getSummary().isBlank()) {
                problems.add(where + " has no summary");
            }
            if (operation.getTags() == null || operation.getTags().isEmpty()) {
                problems.add(where + " has no tag");
            } else {
                for (String tag : operation.getTags()) {
                    if (!declaredTags.contains(tag)) {
                        problems.add(where + " uses the tag " + tag + ", which is not declared");
                    }
                }
            }
            if (operation.getResponses() == null
                    || operation.getResponses().keySet().stream().noneMatch(status -> status.startsWith("2"))) {
                problems.add(where + " has no success response");
            }
            pathParameters(api, where, template, item, operation, problems);
        }));
    }

    private static void pathParameters(OpenAPI api, String where, String template, PathItem item, Operation operation,
            List<String> problems) {
        Set<String> variables = new TreeSet<>();
        Matcher variable = VARIABLE.matcher(template);
        while (variable.find()) {
            variables.add(variable.group(1));
        }
        Set<String> declared = new TreeSet<>();
        List<Parameter> parameters = new ArrayList<>();
        if (item.getParameters() != null) {
            parameters.addAll(item.getParameters());
        }
        if (operation.getParameters() != null) {
            parameters.addAll(operation.getParameters());
        }
        for (Parameter parameter : parameters) {
            Parameter resolved = resolve(api, parameter);
            if (resolved == null || !"path".equals(resolved.getIn())) {
                continue;
            }
            declared.add(resolved.getName());
            if (!Boolean.TRUE.equals(resolved.getRequired())) {
                problems.add(where + ": the path parameter " + resolved.getName() + " is not marked as required");
            }
        }
        if (!declared.equals(variables)) {
            problems.add(where + ": the path has the variables " + variables + " but declares the path parameters " + declared);
        }
    }

    private static Parameter resolve(OpenAPI api, Parameter parameter) {
        if (parameter.get$ref() == null) {
            return parameter;
        }
        Map<String, Parameter> defined = api.getComponents() == null ? null : api.getComponents().getParameters();
        return defined == null ? null : defined.get(lastSegment(parameter.get$ref()));
    }

    private static ApiResponse resolve(OpenAPI api, ApiResponse response) {
        if (response.get$ref() == null) {
            return response;
        }
        Map<String, ApiResponse> defined = api.getComponents() == null ? null : api.getComponents().getResponses();
        return defined == null ? null : defined.get(lastSegment(response.get$ref()));
    }

    private static String lastSegment(String reference) {
        return reference.substring(reference.lastIndexOf('/') + 1);
    }

    // ---------------------------------------------------------------- security

    private static void security(OpenAPI api, List<String> problems) {
        Set<String> defined = api.getComponents() == null || api.getComponents().getSecuritySchemes() == null
                ? Set.of() : api.getComponents().getSecuritySchemes().keySet();
        Set<String> required = new TreeSet<>();
        List<SecurityRequirement> requirements = new ArrayList<>();
        if (api.getSecurity() != null) {
            requirements.addAll(api.getSecurity());
        }
        api.getPaths().values().forEach(item -> item.readOperations().forEach(operation -> {
            if (operation.getSecurity() != null) {
                requirements.addAll(operation.getSecurity());
            }
        }));
        requirements.forEach(requirement -> required.addAll(requirement.keySet()));
        for (String scheme : required) {
            if (!defined.contains(scheme)) {
                problems.add("a security requirement names the scheme " + scheme + ", which is not defined");
            }
        }
        for (String scheme : new TreeSet<>(defined)) {
            if (!required.contains(scheme)) {
                problems.add("nothing requires the security scheme " + scheme);
            }
        }
    }

    // ---------------------------------------------------------------- examples

    private static void examples(String specification, OpenAPI api, List<String> problems) {
        OpenApiInteractionValidator validator =
                OpenApiInteractionValidator.createForInlineApiSpecification(specification).build();
        api.getPaths().forEach((template, item) -> item.readOperationsMap().forEach((httpMethod, operation) -> {
            String where = httpMethod + " " + template;
            String path = VARIABLE.matcher(template).replaceAll("x");
            Request.Method method = Request.Method.valueOf(httpMethod.name());

            if (operation.getRequestBody() != null) {
                examplesOf(operation.getRequestBody().getContent()).forEach((label, example) -> {
                    ValidationReport report = validator.validateRequest(new SimpleRequest.Builder(method, path)
                            .withContentType(example.mediaType()).withBody(example.json()).build());
                    report(problems, where + ": the request example" + label, "validation.request.body", report);
                });
            }
            operation.getResponses().forEach((status, response) -> {
                ApiResponse resolved = resolve(api, response);
                if (resolved == null || "default".equals(status)) {
                    return;
                }
                examplesOf(resolved.getContent()).forEach((label, example) -> {
                    ValidationReport report = validator.validateResponse(path, method,
                            SimpleResponse.Builder.status(Integer.parseInt(status))
                                    .withContentType(example.mediaType()).withBody(example.json()).build());
                    report(problems, where + ": the example of response " + status + label, "validation.response.body", report);
                });
            });
        }));
    }

    private record ExampleBody(String mediaType, String json) {
    }

    /** The examples of a body, keyed by a label for messages: {@code ""} or {@code " 'name'"}. */
    private static Map<String, ExampleBody> examplesOf(Content content) {
        Map<String, ExampleBody> found = new LinkedHashMap<>();
        if (content == null) {
            return found;
        }
        for (Map.Entry<String, MediaType> entry : content.entrySet()) {
            if (!entry.getKey().contains("json")) {
                continue;
            }
            MediaType media = entry.getValue();
            if (media.getExample() != null) {
                found.put("", new ExampleBody(entry.getKey(), json(media.getExample())));
            }
            if (media.getExamples() != null) {
                for (Map.Entry<String, Example> named : media.getExamples().entrySet()) {
                    if (named.getValue().getValue() != null) {
                        found.put(" '" + named.getKey() + "'", new ExampleBody(entry.getKey(), json(named.getValue().getValue())));
                    }
                }
            }
        }
        return found;
    }

    private static String json(Object example) {
        try {
            return example instanceof String text ? text : Json.mapper().writeValueAsString(example);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void report(List<String> problems, String what, String keyPrefix, ValidationReport report) {
        for (ValidationReport.Message message : report.getMessages()) {
            if (message.getLevel() == ValidationReport.Level.ERROR && message.getKey().startsWith(keyPrefix)) {
                StringBuilder text = new StringBuilder(what).append(" does not match its schema: ").append(message.getMessage());
                for (String detail : message.getAdditionalInfo()) {
                    text.append("\n      ").append(detail.replace("\n", "\n      "));
                }
                problems.add(text.toString());
            }
        }
    }
}
