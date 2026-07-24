package com.pyin.plugin.system.permission.model;

public record PermissionSummary(
        String code,
        String name,
        String source,
        String pluginId,
        String pluginName,
        String resourceType
) {
}
