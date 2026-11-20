package dev.agentflow.inbox.client;

import dev.agentflow.inbox.api.ClaimRequest;
import dev.agentflow.inbox.api.ClaimedEvent;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs a {@link ScriptedWorker} from the command line, as a demo and as a process that tests can kill.
 *
 * <pre>
 * java -cp ... dev.agentflow.inbox.client.ScriptedWorkerMain \
 *     --base-url http://127.0.0.1:8080 --client-id runtime-1 --secret ... \
 *     --worker-id worker-1 --outbox-dir ./outbox [--behaviour reply|reply-then-hang] [--max-runs N]
 * </pre>
 *
 * <p>{@code reply} answers every run with one message that lists its events, then acks.
 * {@code reply-then-hang} sends the message, prints {@code SENT <run_id> <epoch>} and then stops making
 * progress without acking: the moment a real worker would be in if it crashed after its side effect.
 */
public final class ScriptedWorkerMain {

    private ScriptedWorkerMain() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        InboxClient client = new InboxClient(URI.create(required(options, "base-url")), required(options, "client-id"),
                required(options, "secret"));
        Outbox outbox = new FileOutbox(Path.of(required(options, "outbox-dir")));
        String workerId = options.getOrDefault("worker-id", "worker-" + ProcessHandle.current().pid());
        boolean hang = "reply-then-hang".equals(options.getOrDefault("behaviour", "reply"));
        int maxRuns = Integer.parseInt(options.getOrDefault("max-runs", hang ? "1" : "2147483647"));
        long waitMs = Long.parseLong(options.getOrDefault("wait-ms", "20000"));

        ScriptedWorker worker = new ScriptedWorker(client, outbox, workerId, context -> {
            context.send("im", summary(context));
            if (hang) {
                System.out.println("SENT " + context.run().runId() + " " + context.run().epoch());
                System.out.flush();
                Thread.sleep(Long.MAX_VALUE);
            }
            return ScriptedWorker.Decision.done();
        });

        System.out.println("READY " + workerId);
        System.out.flush();
        int handled = 0;
        while (handled < maxRuns) {
            if (worker.runOnce(ClaimRequest.any(workerId).withWait(waitMs))) {
                handled++;
            }
        }
    }

    /** The one reply a run produces: a line per event, in the order the run received them. */
    public static String summary(ScriptedWorker.RunContext context) {
        return "handled " + context.events().size() + " event(s): " + context.events().stream()
                .map(ScriptedWorkerMain::describe).collect(Collectors.joining("; "));
    }

    private static String describe(ClaimedEvent event) {
        return event.type() + " " + (event.payload().has("text") ? event.payload().get("text").stringValue()
                : event.sourceEventId());
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("expected an option, got " + args[i]);
            }
            options.put(args[i].substring(2), args[i + 1]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null) {
            throw new IllegalArgumentException("missing --" + name);
        }
        return value;
    }
}
