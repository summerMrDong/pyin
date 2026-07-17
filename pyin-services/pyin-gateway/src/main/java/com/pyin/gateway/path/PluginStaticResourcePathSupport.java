package com.pyin.gateway.path;

import org.springframework.util.StringUtils;

public final class PluginStaticResourcePathSupport {

    private static final String STATIC_PREFIX = "/plugin-static/";
    private static final String ASSETS_SEGMENT = "/assets/";

    private PluginStaticResourcePathSupport() {
    }

    public static boolean isPluginStaticAssetRequest(String requestPath) {
        if (!StringUtils.hasText(requestPath) || !requestPath.startsWith(STATIC_PREFIX)) {
            return false;
        }
        int pluginEnd = requestPath.indexOf('/', STATIC_PREFIX.length());
        return pluginEnd > STATIC_PREFIX.length() && requestPath.startsWith(ASSETS_SEGMENT, pluginEnd);
    }

    public static String extractPluginId(String requestPath) {
        if (!isPluginStaticAssetRequest(requestPath)) {
            throw new IllegalArgumentException("Not a plugin static asset request: " + requestPath);
        }
        int start = STATIC_PREFIX.length();
        int end = requestPath.indexOf('/', start);
        return requestPath.substring(start, end);
    }

    public static String extractRelativeAssetPath(String requestPath) {
        if (!isPluginStaticAssetRequest(requestPath)) {
            throw new IllegalArgumentException("Not a plugin static asset request: " + requestPath);
        }
        return requestPath.substring(("/plugin-static/" + extractPluginId(requestPath) + "/").length());
    }
}
