package com.pyin.plugin.system.plugin;

import com.pyin.plugin.spi.model.PluginPermissionResourceType;

public record PluginNavigationPermission(
        String code,
        String name,
        PluginPermissionResourceType resourceType
) {
}
