package com.pyin.plugin.spi.model;

public record PluginPermission(
        String code,
        String name,
        PluginPermissionResourceType resourceType
) {
}
