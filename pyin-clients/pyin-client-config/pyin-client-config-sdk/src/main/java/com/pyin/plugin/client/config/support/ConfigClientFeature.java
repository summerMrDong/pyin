package com.pyin.plugin.client.config.support;

import com.pyin.plugin.client.api.PyinClientFeature;
import com.pyin.plugin.client.api.PyinClientFeatureContext;
import com.pyin.plugin.client.api.event.PyinCenterEvent;
import com.pyin.plugin.client.config.ConfigChangedListener;
import com.pyin.plugin.client.config.PyinConfigClient;
import com.pyin.plugin.client.config.event.ConfigChangedEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConfigClientFeature implements PyinClientFeature, PyinConfigClient {

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final List<ConfigChangedListener> listeners = new CopyOnWriteArrayList<>();
    private PyinClientFeatureContext context;

    @Override
    public String featureCode() {
        return "config";
    }

    @Override
    public String featureName() {
        return "Config Client";
    }

    @Override
    public void initialize(PyinClientFeatureContext context) {
        this.context = context;
    }

    @Override
    public List<String> supportedEventTypes() {
        return List.of("config.changed", "config.deleted", "config.refreshed");
    }

    @Override
    public void handleEvent(PyinCenterEvent event) {
        Object payload = event.getPayload().getOrDefault("values", Map.of());
        if (payload instanceof Map<?, ?> map) {
            cache.clear();
            map.forEach((key, value) -> cache.put(String.valueOf(key), String.valueOf(value)));
        }
        ConfigChangedEvent changedEvent = new ConfigChangedEvent(event.getNamespace(), event.getEnv(), Map.copyOf(cache));
        listeners.forEach(listener -> listener.onChanged(changedEvent));
        if (context != null) {
            context.publishLocalEvent(changedEvent);
        }
    }

    @Override
    public String getValue(String key) {
        return cache.get(key);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    @Override
    public String getValue(String namespace, String env, String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    @Override
    public Integer getInt(String key, Integer defaultValue) {
        String value = cache.get(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = cache.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    @Override
    public Map<String, String> getNamespace(String namespace, String env) {
        return Map.copyOf(cache);
    }

    @Override
    public void addListener(ConfigChangedListener listener) {
        listeners.add(listener);
    }
}
