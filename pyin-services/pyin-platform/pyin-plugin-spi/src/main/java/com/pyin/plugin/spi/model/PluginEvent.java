package com.pyin.plugin.spi.model;

import java.util.Map;

public record PluginEvent(String pluginId, String eventType, Map<String, Object> payload) {
}
