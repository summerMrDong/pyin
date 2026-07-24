package com.pyin.plugin.system.resource.model;

import java.util.List;

public record ResourceTreeResponse(
        List<ResourceNode> systemResources,
        List<ResourcePluginGroup> pluginGroups
) {
}
