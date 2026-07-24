package com.pyin.gateway.path;

import com.pyin.plugin.runtime.route.PluginApiChannel;

public final class PluginGatewayPathSupport {

    private static final String PLUGIN_GATEWAY_PREFIX = "plugins";

    private PluginGatewayPathSupport() {
    }

    public static boolean isPluginGatewayRequest(String requestUri) {
        return parse(requestUri) != null;
    }

    public static String extractPluginId(String requestUri) {
        PluginGatewayPath path = parse(requestUri);
        if (path != null) {
            return path.pluginId();
        }
        return "";
    }

    public static boolean isAdminRequest(String requestUri) {
        PluginGatewayPath path = parse(requestUri);
        return path != null && path.channel() == PluginApiChannel.ADMIN;
    }

    public static String requestPath(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    public static String extractPluginRelativePath(String requestUri, String pluginId) {
        PluginGatewayPath path = parse(requestUri);
        return path != null && path.pluginId().equals(pluginId) ? path.relativePath() : "/";
    }

    public static PluginGatewayPath parse(String requestUri) {
        String normalized = normalize(requestUri);
        String[] segments = normalized.split("/");
        if (segments.length >= 4
                && PLUGIN_GATEWAY_PREFIX.equals(segments[1])
                && !segments[2].isBlank()
                && ("admin".equals(segments[3]) || "open".equals(segments[3]))) {
            return new PluginGatewayPath(
                    segments[2],
                    "admin".equals(segments[3]) ? PluginApiChannel.ADMIN : PluginApiChannel.CLIENT,
                    relativeAfterSegments(segments, 4)
            );
        }
        return null;
    }

    public static String toPluginControllerPath(PluginGatewayPath path) {
        String fixedSegment = path.channel() == PluginApiChannel.ADMIN ? "/admin" : "/open";
        return normalize("/plugins/" + path.pluginId() + fixedSegment + normalize(path.relativePath()));
    }

    private static String relativeAfterSegments(String[] segments, int startIndex) {
        if (segments.length <= startIndex) {
            return "/";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < segments.length; i++) {
            if (segments[i].isBlank()) {
                continue;
            }
            builder.append('/').append(segments[i]);
        }
        return normalize(builder.toString());
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    public record PluginGatewayPath(
            String pluginId,
            PluginApiChannel channel,
            String relativePath
    ) {
    }
}
