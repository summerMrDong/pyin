package com.pyin.plugin.system.plugin;

import org.springframework.web.util.pattern.PathPattern;

public record CompiledApiRule(
        String pluginId,
        String httpMethod,
        String rawPathPattern,
        String canonicalPathPattern,
        PathPattern compiledPattern,
        String internalPath,
        String permissionCode,
        String accessMode,
        boolean auditEnabled
) {
}
