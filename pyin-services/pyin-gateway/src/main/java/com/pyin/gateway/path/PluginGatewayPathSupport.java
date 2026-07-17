package com.pyin.gateway.path;

public final class PluginGatewayPathSupport {

    private PluginGatewayPathSupport() {
    }

    public static boolean isPluginGatewayRequest(String requestUri) {
        String[] segments = requestUri == null ? new String[0] : requestUri.split("/");
        return segments.length >= 3
                && !segments[1].isBlank()
                && ("admin".equals(segments[2]) || "open".equals(segments[2]));
    }

    public static String extractPluginId(String requestUri) {
        String[] segments = requestUri == null ? new String[0] : requestUri.split("/");
        if (segments.length < 2 || segments[1].isBlank()) {
            return "";
        }
        return segments[1];
    }

    public static boolean isAdminRequest(String requestUri) {
        String[] segments = requestUri == null ? new String[0] : requestUri.split("/");
        return segments.length >= 3 && "admin".equals(segments[2]);
    }

    public static String requestPath(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    public static String extractPluginRelativePath(String requestUri, String pluginId) {
        String pluginPrefix = "/" + pluginId;
        String remainingPath;
        if (requestUri.startsWith(pluginPrefix)) {
            remainingPath = requestUri.substring(pluginPrefix.length());
        } else {
            return "/";
        }
        if (remainingPath.startsWith("/admin")) {
            remainingPath = remainingPath.substring("/admin".length());
        } else if (remainingPath.startsWith("/open")) {
            remainingPath = remainingPath.substring("/open".length());
        }
        return normalize(remainingPath);
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
