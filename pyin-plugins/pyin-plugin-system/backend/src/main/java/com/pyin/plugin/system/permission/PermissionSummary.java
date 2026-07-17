package com.pyin.plugin.system.permission;

public record PermissionSummary(
        String code,
        String name,
        String source,
        String pluginId,
        String pluginName,
        String resourceType
) {
}
