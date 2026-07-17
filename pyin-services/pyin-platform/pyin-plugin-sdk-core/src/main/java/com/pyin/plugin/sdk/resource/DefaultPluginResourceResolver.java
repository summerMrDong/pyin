package com.pyin.plugin.sdk.resource;

import java.util.List;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

public class DefaultPluginResourceResolver implements PluginResourceResolver {

    private final PluginResourceProperties properties;
    private final ResourceLoader resourceLoader;

    public DefaultPluginResourceResolver(PluginResourceProperties properties) {
        this(properties, new DefaultResourceLoader());
    }

    public DefaultPluginResourceResolver(PluginResourceProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public ResolvedPluginResource resolve(String pluginId, String relativePath) {
        return resolve(pluginId, relativePath, properties.getEffectiveLocations(pluginId));
    }

    @Override
    public ResolvedPluginResource resolve(String pluginId, String relativePath, List<String> locations) {
        if (!StringUtils.hasText(pluginId)) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        String normalizedRelativePath = normalizeRelativePath(relativePath);
        for (String rawLocation : locations) {
            String candidate = buildLocation(rawLocation, pluginId, normalizedRelativePath);
            Resource resource = resourceLoader.getResource(candidate);
            if (resource.exists()) {
                return new ResolvedPluginResource(candidate, normalizedRelativePath, resource);
            }
        }
        throw new IllegalStateException("Missing plugin resource: pluginId=" + pluginId + ", relativePath=" + normalizedRelativePath);
    }

    private String buildLocation(String rawLocation, String pluginId, String relativePath) {
        String location = (rawLocation == null ? "" : rawLocation).replace("{pluginId}", pluginId);
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        return location + relativePath;
    }

    private String normalizeRelativePath(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return "";
        }
        return relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
    }
}
