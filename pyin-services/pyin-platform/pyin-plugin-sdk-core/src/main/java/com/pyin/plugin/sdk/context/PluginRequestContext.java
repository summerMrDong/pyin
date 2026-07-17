package com.pyin.plugin.sdk.context;

import java.util.Optional;

public final class PluginRequestContext {

    private static final ThreadLocal<String> REQUEST_SOURCE = new ThreadLocal<>();

    private PluginRequestContext() {
    }

    public static void setRequestSource(String source) {
        REQUEST_SOURCE.set(source);
    }

    public static Optional<String> getRequestSource() {
        return Optional.ofNullable(REQUEST_SOURCE.get());
    }

    public static void clear() {
        REQUEST_SOURCE.remove();
    }
}
