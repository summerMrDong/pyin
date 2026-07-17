package com.pyin.plugin.system.role.resource;

import java.util.List;

public record ResourceNode(
        String resourceKey,
        String resourceCode,
        String resourceName,
        String resourceType,
        String resourceScope,
        String pluginId,
        String parentKey,
        String path,
        String icon,
        Integer sort,
        String permissionCode,
        Boolean visible,
        List<ResourceNode> children
) {
}
