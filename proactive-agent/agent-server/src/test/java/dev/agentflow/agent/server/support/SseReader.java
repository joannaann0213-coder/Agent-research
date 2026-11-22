package dev.agentflow.agent.server.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Reads a server-sent event stream the way a browser does, one event at a time. */
public final class SseReader implements AutoCloseable {

    /** One event: its id, its name, its data; and the comments that came before it. */
    public record Event(String id, String name, String data) {
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Event END = new Event(null, null, null);

    private final HttpResponse<InputStream> response;
    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    private final Thread reader;

    private SseReader(HttpResponse<InputStream> response) {
        this.response = response;
        this.reader = Thread.ofVirtual().start(this::read);
    }

    /** Opens the stream. Headers are name, value, name, value. */
    public static SseReader open(URI uri, String... headers) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).header("Accept", "text/event-stream").GET();
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        try {
            return new SseReader(HTTP.send(request.build(), HttpResponse.BodyHandlers.ofInputStream()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public int status() {
        return response.statusCode();
    }

    public Map<String, List<String>> headers() {
        return response.headers().map();
    }

    /** The next event, waiting up to five seconds for it. */
    public Event next() {
        try {
            Event event = events.poll(5, TimeUnit.SECONDS);
            if (event == null || event == END) {
                throw new AssertionError(event == null ? "no event came within 5 seconds" : "the stream ended");
            }
            return event;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** The next {@code count} events. */
    public List<Event> next(int count) {
        List<Event> taken = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            taken.add(next());
        }
        return taken;
    }

    /** Whether no event comes within {@code time}. */
    public boolean quietFor(Duration time) {
        try {
            Event event = events.poll(time.toMillis(), TimeUnit.MILLISECONDS);
            return event == null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void read() {
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String id = null;
            String name = null;
            StringBuilder data = new StringBuilder();
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                if (line.isEmpty()) {
                    if (!data.isEmpty() || name != null) {
                        events.add(new Event(id, name, data.toString()));
                    }
                    id = null;
                    name = null;
                    data.setLength(0);
                } else if (line.startsWith("id:")) {
                    id = line.substring(3).trim();
                } else if (line.startsWith("event:")) {
                    name = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    data.append(data.isEmpty() ? "" : "\n").append(line.substring(5).trim());
                }
            }
        } catch (IOException e) {
            // closed
        } finally {
            events.add(END);
        }
    }

    @Override
    public void close() {
        try {
            response.body().close();
        } catch (IOException e) {
            // closed already
        }
        reader.interrupt();
    }
}
