package com.pyin.plugin.system.role.resource;

public record SystemResourceDefinition(
        String resourceCode,
        String resourceName,
        String resourceType,
        String parentCode,
        String path,
        String icon,
        Integer sort,
        String permissionCode,
        Boolean visible
) {
}
