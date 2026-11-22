package dev.agentflow.agent.server.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** A webhook receiver on the loopback, which answers with the statuses it is told to and keeps what it got. */
public final class HookReceiver implements AutoCloseable {

    /** What a delivery brought. Header names are lower case. */
    public record Delivery(String path, Map<String, String> headers, byte[] body) {
    }

    private final HttpServer server;
    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>();
    private final Deque<Integer> statuses = new ArrayDeque<>();
    private int otherwise = 204;

    public HookReceiver() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            Map<String, String> headers = new java.util.TreeMap<>();
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), values.getFirst()));
            deliveries.add(new Delivery(exchange.getRequestURI().toString(), headers, exchange.getRequestBody().readAllBytes()));
            int status;
            synchronized (statuses) {
                status = statuses.isEmpty() ? otherwise : statuses.poll();
            }
            if (status == 302) {
                exchange.getResponseHeaders().add("Location", "http://127.0.0.1:1/elsewhere");
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    public String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    /** The next answers, in order; after them, {@link #otherwise}. */
    public HookReceiver answer(Integer... next) {
        synchronized (statuses) {
            statuses.addAll(List.of(next));
        }
        return this;
    }

    public HookReceiver otherwise(int status) {
        this.otherwise = status;
        return this;
    }

    public List<Delivery> deliveries() {
        return List.copyOf(deliveries);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
