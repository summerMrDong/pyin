package com.pyin.plugin.system.plugin;

import java.util.List;

public record PluginNavigationItem(
        String pluginId,
        String pluginName,
        String pluginType,
        String status,
        String sourceType,
        PluginNavigationFrontend frontend,
        List<PluginNavigationMenu> menus,
        List<PluginNavigationPermission> permissions
) {
}
