package dev.agentflow.agent.server.delivery;

import dev.agentflow.agent.server.config.AgentProperties;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Where a webhook may point: a public address, over https. A webhook is a request this service makes
 * on an application's behalf, so it must not reach what is behind the service's own network: its
 * loopback, private networks, link-local addresses with the cloud's metadata service, and the like.
 *
 * <p>The address is checked when the webhook is set up, and again for every delivery, on the
 * address the delivery then connects to.
 */
@Component
public class AddressPolicy {

    /** A target that must not be reached, and why. */
    public static final class Refused extends Exception {
        Refused(String reason) {
            super(reason, null, false, false);
        }
    }

    private record Range(byte[] network, int prefix) {
    }

    private static final List<Range> BLOCKED = List.of(
            v4(0, 0, 0, 0, 8),          // this network
            v4(10, 0, 0, 0, 8),         // private
            v4(100, 64, 0, 0, 10),      // shared address space (carrier-grade NAT)
            v4(127, 0, 0, 0, 8),        // loopback
            v4(169, 254, 0, 0, 16),     // link-local, with cloud metadata services
            v4(172, 16, 0, 0, 12),      // private
            v4(192, 0, 0, 0, 24),       // protocol assignments
            v4(192, 0, 2, 0, 24),       // documentation
            v4(192, 88, 99, 0, 24),     // 6to4 relays
            v4(192, 168, 0, 0, 16),     // private
            v4(198, 18, 0, 0, 15),      // benchmarking
            v4(198, 51, 100, 0, 24),    // documentation
            v4(203, 0, 113, 0, 24),     // documentation
            v4(224, 0, 0, 0, 4),        // multicast
            v4(240, 0, 0, 0, 4));       // reserved, and broadcast

    private final AgentProperties.Webhooks config;

    public AddressPolicy(AgentProperties properties) {
        this.config = properties.getWebhooks();
    }

    /** What is wrong with a webhook URL, or {@code null} when nothing is that can be seen without sending to it. */
    public String problemWith(String url) {
        if (url == null || url.isBlank() || url.length() > 2000) {
            return "url is required and has at most 2000 characters";
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return "url is not a URL";
        }
        boolean https = "https".equals(uri.getScheme());
        if (!https && !(config.isAllowPlainHttp() && "http".equals(uri.getScheme()))) {
            return "url must be an https URL";
        }
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
            return "url needs a host, and no user information or fragment";
        }
        try {
            resolve(uri.getHost());
        } catch (Refused refused) {
            return "url cannot be used: " + refused.getMessage();
        }
        return null;
    }

    /**
     * The address to connect to for {@code host}. Every address the host has must be allowed: a host
     * that has one public and one private address would otherwise be a way around this check.
     */
    public InetAddress resolve(String host) throws Refused {
        String name = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(name);
        } catch (UnknownHostException e) {
            throw new Refused("the host " + name + " cannot be found");
        }
        for (InetAddress address : addresses) {
            String why = refusal(address);
            if (why != null) {
                throw new Refused("the host " + name + " has the address " + address.getHostAddress() + ", which is " + why);
            }
        }
        return addresses[0];
    }

    /** Why the address must not be reached, or {@code null} when it may. */
    String refusal(InetAddress address) {
        if (config.isAllowPrivateNetworks()) {
            return null;
        }
        if (address instanceof Inet6Address v6) {
            byte[] bytes = v6.getAddress();
            byte[] embedded = embeddedV4(bytes);
            if (embedded != null) {
                return refusalV4(embedded);
            }
            if (v6.isAnyLocalAddress() || v6.isLoopbackAddress()) {
                return "the host itself";
            }
            if ((bytes[0] & 0xfe) == 0xfc) {
                return "in a private network";
            }
            if (v6.isLinkLocalAddress() || v6.isSiteLocalAddress()) {
                return "link-local";
            }
            if (v6.isMulticastAddress()) {
                return "a multicast address";
            }
            if (bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] == 0x0d && (bytes[3] & 0xff) == 0xb8) {
                return "a documentation address";
            }
            return null;
        }
        return refusalV4(((Inet4Address) address).getAddress());
    }

    private static String refusalV4(byte[] bytes) {
        for (Range range : BLOCKED) {
            if (within(bytes, range)) {
                return bytes[0] == 127 || bytes[0] == 0 ? "the host itself"
                        : (bytes[0] & 0xff) == 169 && (bytes[1] & 0xff) == 254 ? "link-local, where cloud metadata services are"
                        : "not a public address";
            }
        }
        return null;
    }

    /** The IPv4 address inside an IPv4-mapped, IPv4-compatible or NAT64 IPv6 address; {@code null} for others. */
    private static byte[] embeddedV4(byte[] v6) {
        boolean mapped = allZero(v6, 0, 10) && (v6[10] & 0xff) == 0xff && (v6[11] & 0xff) == 0xff;
        boolean compatible = allZero(v6, 0, 12) && !(allZero(v6, 12, 15) && (v6[15] == 0 || v6[15] == 1));
        boolean nat64 = v6[0] == 0x00 && v6[1] == 0x64 && (v6[2] & 0xff) == 0xff && (v6[3] & 0xff) == 0x9b && allZero(v6, 4, 12);
        return mapped || compatible || nat64 ? Arrays.copyOfRange(v6, 12, 16) : null;
    }

    private static boolean allZero(byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean within(byte[] address, Range range) {
        int full = range.prefix() / 8;
        for (int i = 0; i < full; i++) {
            if (address[i] != range.network()[i]) {
                return false;
            }
        }
        int rest = range.prefix() % 8;
        if (rest == 0) {
            return true;
        }
        int mask = 0xff << (8 - rest) & 0xff;
        return (address[full] & mask) == (range.network()[full] & mask);
    }

    private static Range v4(int a, int b, int c, int d, int prefix) {
        return new Range(new byte[] {(byte) a, (byte) b, (byte) c, (byte) d}, prefix);
    }
}
