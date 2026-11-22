package dev.agentflow.testkit.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks real HTTP traffic against an OpenAPI specification, and remembers which operations and
 * statuses it has seen.
 *
 * <p>Rules for one exchange:
 *
 * <ul>
 *   <li>The response is always checked against what the operation defines for its status. A field
 *       the schema does not list is an error.
 *   <li>The request is checked when the server accepted it (a status below 400). A refused request
 *       may be wrong; that is usually the point of the test.
 *   <li>The status must be listed for the operation. {@code default} does not count, except for 406
 *       and 415, which content negotiation produces for any operation.
 *   <li>A request the specification has no operation for must be refused (a status of 400 or more).
 * </ul>
 *
 * <p>There is one instance per specification and JVM, so coverage adds up across test classes and
 * application contexts.
 */
public final class OpenApiTraffic {

    /** Statuses that {@code default} may cover: they come from content negotiation, not from the operation. */
    private static final Set<Integer> COVERED_BY_DEFAULT = Set.of(406, 415);

    private static final Pattern VARIABLE = Pattern.compile("\\{[^/}]+}");
    private static final Pattern REFERENCE = Pattern.compile("#/components/([A-Za-z]+)/([A-Za-z0-9._-]+)");
    private static final int BODY_EXCERPT = 800;

    private static final Map<String, OpenApiTraffic> BY_SPEC = new ConcurrentHashMap<>();

    private final String name;
    private final OpenApiInteractionValidator validator;
    private final List<SpecOperation> operations;
    private final Map<String, Set<Integer>> observed = new ConcurrentHashMap<>();
    private final Queue<String> violations = new ConcurrentLinkedQueue<>();
    private final AtomicLong exchanges = new AtomicLong();

    private OpenApiTraffic(String name, String specification) {
        this.name = name;
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parsed = new OpenAPIV3Parser().readContents(specification, null, options);
        if (parsed.getOpenAPI() == null || (parsed.getMessages() != null && !parsed.getMessages().isEmpty())) {
            throw new IllegalStateException("the specification " + name + " does not parse cleanly: " + parsed.getMessages());
        }
        List<String> dangling = danglingReferences(specification, parsed.getOpenAPI());
        if (!dangling.isEmpty()) {
            throw new IllegalStateException("the specification " + name + " refers to components it does not define: " + dangling);
        }
        this.operations = operations(parsed.getOpenAPI());
        this.validator = OpenApiInteractionValidator.createForInlineApiSpecification(specification).build();
    }

    /** The recorder for a specification on the classpath, for example {@code openapi/inbox.yaml}. */
    public static OpenApiTraffic forClasspathSpec(String resource) {
        return BY_SPEC.computeIfAbsent(resource, r -> new OpenApiTraffic(r, readResource(r)));
    }

    /** A recorder of its own for a specification given as text; nothing is shared. */
    public static OpenApiTraffic forSpec(String name, String specification) {
        return new OpenApiTraffic(name, specification);
    }

    // ---------------------------------------------------------------- recording

    /** Checks one exchange and counts it. Never throws: what is wrong is kept for {@link #assertNoViolations()}. */
    public void record(Exchange exchange) {
        exchanges.incrementAndGet();
        try {
            check(exchange);
        } catch (RuntimeException e) {
            violations.add(exchange.summary() + "\n  the check itself failed: " + e);
        }
    }

    /** Notes a request that ended with an exception instead of a response. */
    public void failed(String method, String path, Throwable error) {
        exchanges.incrementAndGet();
        violations.add(method + " " + path + "\n  ended with an exception instead of a response: " + error);
    }

    /** Whether the operation behind this request streams its response, which must then not be buffered. */
    public boolean streams(String method, String path) {
        SpecOperation operation = match(method, path);
        return operation != null && operation.streaming();
    }

    private void check(Exchange exchange) {
        SpecOperation operation = match(exchange.method(), exchange.path());
        if (operation == null) {
            if (exchange.status() < 400) {
                violations.add(describe(exchange, null, List.of(
                        "the specification has no operation for this request, yet the server accepted it")));
            }
            return;
        }
        observed.computeIfAbsent(operation.key(), k -> ConcurrentHashMap.newKeySet()).add(exchange.status());

        List<String> problems = new ArrayList<>();
        if (!operation.statuses().contains(exchange.status()) && !COVERED_BY_DEFAULT.contains(exchange.status())) {
            problems.add("status " + exchange.status() + " is not listed for this operation; listed: "
                    + new TreeSet<>(operation.statuses()));
        }
        Request.Method method = Request.Method.valueOf(exchange.method());
        if (!operation.streaming()) {
            collect(problems, "response", validator.validateResponse(exchange.path(), method, response(exchange)));
        }
        if (exchange.status() < 400) {
            collect(problems, "request", validator.validateRequest(request(method, exchange)));
        }
        if (!problems.isEmpty()) {
            violations.add(describe(exchange, operation, problems));
        }
    }

    private static SimpleRequest request(Request.Method method, Exchange exchange) {
        SimpleRequest.Builder request = new SimpleRequest.Builder(method, exchange.path());
        exchange.requestHeaders().forEach(request::withHeader);
        queryParameters(exchange.query()).forEach(request::withQueryParam);
        if (exchange.requestBody() != null && exchange.requestBody().length > 0) {
            request.withBody(exchange.requestBody());
        }
        return request.build();
    }

    private static SimpleResponse response(Exchange exchange) {
        SimpleResponse.Builder response = SimpleResponse.Builder.status(exchange.status());
        exchange.responseHeaders().forEach(response::withHeader);
        if (exchange.responseBody() != null && exchange.responseBody().length > 0) {
            response.withBody(exchange.responseBody());
        }
        return response.build();
    }

    private static Map<String, List<String>> queryParameters(String query) {
        Map<String, List<String>> parameters = new java.util.LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return parameters;
        }
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String key = URLDecoder.decode(equals < 0 ? pair : pair.substring(0, equals), StandardCharsets.UTF_8);
            String value = equals < 0 ? "" : URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
            parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return parameters;
    }

    private static void collect(List<String> problems, String side, ValidationReport report) {
        for (ValidationReport.Message message : report.getMessages()) {
            if (message.getLevel() != ValidationReport.Level.ERROR) {
                continue;
            }
            StringBuilder text = new StringBuilder(side).append(": ").append(message.getKey()).append(": ")
                    .append(message.getMessage());
            for (String detail : message.getAdditionalInfo()) {
                text.append("\n      ").append(detail.replace("\n", "\n      "));
            }
            problems.add(text.toString());
        }
    }

    private static String describe(Exchange exchange, SpecOperation operation, List<String> problems) {
        StringBuilder text = new StringBuilder(exchange.summary());
        if (operation != null) {
            text.append("  [").append(operation.id()).append("]");
        }
        for (String problem : problems) {
            text.append("\n  - ").append(problem);
        }
        text.append("\n  request body:  ").append(excerpt(exchange.requestBody()));
        text.append("\n  response body: ").append(excerpt(exchange.responseBody()));
        return text.toString();
    }

    private static String excerpt(byte[] body) {
        if (body == null || body.length == 0) {
            return "(none)";
        }
        String text = new String(body, StandardCharsets.UTF_8);
        return text.length() <= BODY_EXCERPT ? text : text.substring(0, BODY_EXCERPT) + "… (" + body.length + " bytes)";
    }

    // ---------------------------------------------------------------- assertions

    /** Fails with everything recorded since the last call, and forgets it. */
    public void assertNoViolations() {
        List<String> found = drainViolations();
        if (!found.isEmpty()) {
            throw new AssertionError(found.size() + " exchange(s) do not match the specification " + name + ":\n\n"
                    + String.join("\n\n", found));
        }
    }

    /** Returns everything recorded since the last call, and forgets it. */
    public List<String> drainViolations() {
        List<String> found = new ArrayList<>();
        for (String violation = violations.poll(); violation != null; violation = violations.poll()) {
            found.add(violation);
        }
        return found;
    }

    /** How many exchanges were recorded in this JVM. */
    public long exchanges() {
        return exchanges.get();
    }

    /** Every operation of the specification as {@code METHOD /path/{}}: variable names are left out. */
    public Set<String> operations() {
        return operations.stream().map(o -> o.method() + " " + normalise(o.template()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Fails unless the specification and the server describe the same operations.
     *
     * @param serverOperations what the server maps, in the form of {@link #operations()}
     */
    public void assertSameOperationsAs(Collection<String> serverOperations) {
        Set<String> documented = operations();
        Set<String> undocumented = new TreeSet<>(serverOperations);
        undocumented.removeAll(documented);
        Set<String> unimplemented = new TreeSet<>(documented);
        unimplemented.removeAll(serverOperations);
        if (!undocumented.isEmpty() || !unimplemented.isEmpty()) {
            throw new AssertionError("the specification " + name + " and the server disagree.\n"
                    + "  mapped by the server but not in the specification: " + undocumented + "\n"
                    + "  in the specification but not mapped by the server: " + unimplemented);
        }
    }

    /**
     * What the recorded traffic has not shown yet: for every operation, each listed status that was
     * never observed.
     *
     * @param exempt statuses that need not be observed for every operation, typically the ones a
     *     shared layer produces (400, 401, 403)
     */
    public List<String> coverageGaps(Set<Integer> exempt) {
        List<String> gaps = new ArrayList<>();
        for (SpecOperation operation : operations) {
            Set<Integer> seen = observed.getOrDefault(operation.key(), Set.of());
            Set<Integer> missing = new TreeSet<>(operation.statuses());
            missing.removeAll(seen);
            missing.removeIf(status -> status >= 400 && exempt.contains(status));
            if (!missing.isEmpty()) {
                gaps.add(operation.key() + " [" + operation.id() + "]: never observed " + missing);
            }
        }
        return gaps;
    }

    /** Fails when {@link #coverageGaps(Set)} finds anything. */
    public void assertCovered(Set<Integer> exempt) {
        List<String> gaps = coverageGaps(exempt);
        if (!gaps.isEmpty()) {
            throw new AssertionError("the tests do not exercise all of the specification " + name + ":\n  "
                    + String.join("\n  ", gaps) + "\n\n" + coverageReport());
        }
    }

    /** One line per operation: the statuses it lists and the statuses that were observed. */
    public String coverageReport() {
        StringBuilder report = new StringBuilder("coverage of " + name + " (" + exchanges.get() + " exchanges)\n");
        for (SpecOperation operation : operations) {
            report.append(String.format("  %-52s listed %-36s observed %s%n", operation.key(),
                    new TreeSet<>(operation.statuses()),
                    new TreeSet<>(observed.getOrDefault(operation.key(), Set.of()))));
        }
        return report.toString();
    }

    // ---------------------------------------------------------------- the specification

    /** {@code /v1/things/{thing_id}} becomes {@code /v1/things/{}}. */
    public static String normalise(String template) {
        return VARIABLE.matcher(template).replaceAll("{}");
    }

    /** The most specific operation with this method whose path template fits, or {@code null}. */
    private SpecOperation match(String method, String path) {
        return operations.stream()
                .filter(o -> o.method().equals(method) && o.pattern().matcher(path).matches())
                .min(Comparator.comparingInt(SpecOperation::variables))
                .orElse(null);
    }

    private static List<SpecOperation> operations(OpenAPI api) {
        List<SpecOperation> found = new ArrayList<>();
        api.getPaths().forEach((template, item) -> item.readOperationsMap().forEach((method, operation) -> {
            Set<Integer> statuses = new TreeSet<>();
            boolean streaming = false;
            for (Map.Entry<String, ApiResponse> response : operation.getResponses().entrySet()) {
                if (!"default".equals(response.getKey())) {
                    statuses.add(Integer.valueOf(response.getKey()));
                }
                streaming |= streams(api, response.getValue());
            }
            found.add(new SpecOperation(id(operation, method.name(), template), method.name(), template,
                    pattern(template), variables(template), statuses, streaming));
        }));
        found.sort(Comparator.comparing(SpecOperation::template).thenComparing(SpecOperation::method));
        return List.copyOf(found);
    }

    private static String id(Operation operation, String method, String template) {
        return operation.getOperationId() != null ? operation.getOperationId() : method + " " + template;
    }

    private static boolean streams(OpenAPI api, ApiResponse response) {
        ApiResponse resolved = response;
        if (response.get$ref() != null && api.getComponents() != null && api.getComponents().getResponses() != null) {
            String refName = response.get$ref().substring(response.get$ref().lastIndexOf('/') + 1);
            resolved = api.getComponents().getResponses().getOrDefault(refName, response);
        }
        return resolved.getContent() != null
                && resolved.getContent().keySet().stream().anyMatch(type -> type.startsWith("text/event-stream"));
    }

    /** The parser accepts a reference to a component that does not exist; a reader of the specification cannot. */
    private static List<String> danglingReferences(String specification, OpenAPI api) {
        List<String> dangling = new ArrayList<>();
        Matcher reference = REFERENCE.matcher(specification);
        while (reference.find()) {
            Map<String, ?> section = OpenApiLint.section(api.getComponents(), reference.group(1));
            if (section == null || !section.containsKey(reference.group(2))) {
                dangling.add(reference.group(0));
            }
        }
        return dangling;
    }

    private static Pattern pattern(String template) {
        StringBuilder regex = new StringBuilder();
        Matcher variable = VARIABLE.matcher(template);
        int at = 0;
        while (variable.find()) {
            regex.append(Pattern.quote(template.substring(at, variable.start()))).append("[^/]+");
            at = variable.end();
        }
        regex.append(Pattern.quote(template.substring(at)));
        return Pattern.compile(regex.toString());
    }

    private static int variables(String template) {
        Matcher variable = VARIABLE.matcher(template);
        int count = 0;
        while (variable.find()) {
            count++;
        }
        return count;
    }

    private static String readResource(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = OpenApiTraffic.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("no such classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record SpecOperation(String id, String method, String template, Pattern pattern, int variables,
            Set<Integer> statuses, boolean streaming) {

        String key() {
            return method + " " + template;
        }
    }
}
