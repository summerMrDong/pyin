package com.pyin.plugin.system.role.resource;

import java.util.List;

public record ResourceTreeResponse(
        List<ResourceNode> systemResources,
        List<ResourcePluginGroup> pluginGroups
) {
}
