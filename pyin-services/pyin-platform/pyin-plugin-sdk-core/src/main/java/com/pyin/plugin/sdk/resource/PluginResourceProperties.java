package com.pyin.plugin.sdk.resource;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "pyin.plugin.resources")
@Getter
@Setter
public class PluginResourceProperties {

    private List<String> locations = new ArrayList<>(List.of("classpath:/plugin-static/{pluginId}/"));
    private String remoteEntryFile = "remoteEntry.js";
    private String assetsDir = "assets/";
    private Map<String, PluginResourceOverride> plugins = new LinkedHashMap<>();

    public List<String> getEffectiveLocations(String pluginId) {
        PluginResourceOverride override = plugins.get(pluginId);
        if (override != null && override.getLocations() != null && !override.getLocations().isEmpty()) {
            return override.getLocations();
        }
        return locations;
    }

    public String getEffectiveRemoteEntryFile(String pluginId) {
        return getEffectiveValue(pluginId, PluginResourceOverride::getRemoteEntryFile, remoteEntryFile);
    }

    public String getEffectiveAssetsDir(String pluginId) {
        return getEffectiveValue(pluginId, PluginResourceOverride::getAssetsDir, assetsDir);
    }

    private String getEffectiveValue(String pluginId, java.util.function.Function<PluginResourceOverride, String> extractor, String fallback) {
        PluginResourceOverride override = plugins.get(pluginId);
        if (override == null) {
            return fallback;
        }
        String value = extractor.apply(override);
        return StringUtils.hasText(value) ? value : fallback;
    }

    @Getter
    @Setter
    public static class PluginResourceOverride {
        private List<String> locations = new ArrayList<>();
        private String remoteEntryFile;
        private String assetsDir;
    }
}
