package dev.agentflow.agent.server.delivery;

import dev.agentflow.agent.server.config.AgentProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.springframework.stereotype.Component;

/**
 * Sends one webhook delivery. It connects to the address that {@link AddressPolicy} allowed, not to
 * whatever the host name resolves to a moment later, so that a name that changes its address
 * between the check and the connection gets nowhere. The certificate is still checked against the
 * host name. Redirects are not followed: only the status of the answer counts.
 */
@Component
public class WebhookSender {

    /** @param status the receiver's status; 0 when there was none */
    public record Result(int status, String error) {

        public boolean ok() {
            return error == null && status >= 200 && status < 300;
        }

        public String describe() {
            return error != null ? error : "the receiver answered " + status;
        }
    }

    private static final int MAX_STATUS_LINE = 1024;

    private final AddressPolicy policy;
    private final AgentProperties.Webhooks config;

    public WebhookSender(AddressPolicy policy, AgentProperties properties) {
        this.policy = policy;
        this.config = properties.getWebhooks();
    }

    public Result post(URI url, Map<String, String> headers, byte[] body) {
        boolean tls = "https".equals(url.getScheme());
        String host = url.getHost();
        int port = url.getPort() != -1 ? url.getPort() : tls ? 443 : 80;
        InetAddress address;
        try {
            address = policy.resolve(host);
        } catch (AddressPolicy.Refused refused) {
            return new Result(0, "not sent: " + refused.getMessage());
        }
        try (Socket plain = new Socket()) {
            plain.connect(new InetSocketAddress(address, port), (int) config.getConnectTimeout().toMillis());
            plain.setSoTimeout((int) config.getReadTimeout().toMillis());
            Socket socket = plain;
            if (tls) {
                SSLSocket secure = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(plain, host, port, true);
                SSLParameters parameters = secure.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                if (!host.startsWith("[") && !Character.isDigit(host.charAt(host.length() - 1))) {
                    parameters.setServerNames(List.of(new SNIHostName(host)));
                }
                secure.setSSLParameters(parameters);
                secure.startHandshake();
                socket = secure;
            }
            OutputStream out = socket.getOutputStream();
            out.write(request(url, port, tls, headers, body.length).getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
            return new Result(status(socket.getInputStream()), null);
        } catch (IOException | RuntimeException e) {
            return new Result(0, e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        }
    }

    private static String request(URI url, int port, boolean tls, Map<String, String> headers, int length) {
        String path = url.getRawPath() == null || url.getRawPath().isEmpty() ? "/" : url.getRawPath();
        if (url.getRawQuery() != null) {
            path += "?" + url.getRawQuery();
        }
        boolean defaultPort = port == (tls ? 443 : 80);
        StringBuilder request = new StringBuilder("POST ").append(path).append(" HTTP/1.1\r\n")
                .append("Host: ").append(url.getHost()).append(defaultPort ? "" : ":" + port).append("\r\n")
                .append("Content-Length: ").append(length).append("\r\n")
                .append("Connection: close\r\n");
        headers.forEach((name, value) -> request.append(name).append(": ").append(value).append("\r\n"));
        return request.append("\r\n").toString();
    }

    /** The status of the answer, read from its first line. */
    private static int status(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int previous = -1;
        for (int next = in.read(); next != -1 && line.size() < MAX_STATUS_LINE; next = in.read()) {
            if (previous == '\r' && next == '\n') {
                break;
            }
            line.write(next);
            previous = next;
        }
        String text = line.toString(StandardCharsets.ISO_8859_1).trim();
        String[] parts = text.split(" ");
        if (parts.length < 2 || !parts[0].startsWith("HTTP/")) {
            throw new IOException("the receiver did not answer in HTTP");
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("the receiver's status line is not valid: " + text);
        }
    }
}
