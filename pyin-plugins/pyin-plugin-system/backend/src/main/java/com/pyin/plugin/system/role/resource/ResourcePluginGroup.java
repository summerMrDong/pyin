package com.pyin.plugin.system.role.resource;

import java.util.List;

public record ResourcePluginGroup(
        String pluginId,
        String pluginName,
        List<ResourceNode> resources
) {
}
