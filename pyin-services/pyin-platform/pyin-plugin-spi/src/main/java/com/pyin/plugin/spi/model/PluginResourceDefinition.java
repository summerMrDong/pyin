package com.pyin.plugin.spi.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record PluginResourceDefinition(
        String resourceCode,
        String resourceName,
        PluginResourceType resourceType,
        String parentCode,
        String path,
        String icon,
        Integer sort,
        String permissionCode,
        boolean visible,
        Map<String, Object> metadata
) {

    public PluginResourceDefinition {
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
    }
}
