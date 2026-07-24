package com.pyin.plugin.system.resource.model;

import java.util.List;

public record ResourcePluginGroup(
        String pluginId,
        String pluginName,
        List<ResourceNode> resources
) {
}
