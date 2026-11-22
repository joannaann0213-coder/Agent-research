package dev.agentflow.agent.cma;

import java.net.URI;

/** How the platform tells whether a credential is for an MCP server: by URL, without regard to case, a default port or a trailing slash. */
public final class McpUrls {

    private McpUrls() {
    }

    public static String normalised(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        int port = uri.getPort();
        boolean defaultPort = port == -1 || ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return scheme + "://" + (uri.getHost() == null ? "" : uri.getHost().toLowerCase()) + (defaultPort ? "" : ":" + port) + path
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    public static boolean same(String one, String other) {
        return normalised(one).equals(normalised(other));
    }
}
