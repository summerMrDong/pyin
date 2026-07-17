package com.pyin.plugin.client.config;

import java.util.Map;

public interface PyinConfigClient {

    String getValue(String key);

    String getValue(String key, String defaultValue);

    String getValue(String namespace, String env, String key, String defaultValue);

    Integer getInt(String key, Integer defaultValue);

    Boolean getBoolean(String key, Boolean defaultValue);

    Map<String, String> getNamespace(String namespace, String env);

    void addListener(ConfigChangedListener listener);
}
