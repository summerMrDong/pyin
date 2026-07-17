package com.pyin.plugin.client.config.event;

import java.util.Map;

public record ConfigChangedEvent(String namespace, String env, Map<String, String> values) {
}
