package com.pyin.plugin.spi.model;

public record PluginApiDefinition(
        String path,
        String method,
        String internalPath,
        PluginAccessMode accessMode,
        String permissionCode,
        boolean auditEnabled
) {
}
