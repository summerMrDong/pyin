package com.pyin.plugin.sdk.manifest;

import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginPermission;
import java.util.List;

/**
 * 插件后端注解扫描结果。
 */
public record PluginScanResult(
        List<PluginApiDefinition> apis,
        List<PluginPermission> permissions
) {
}
