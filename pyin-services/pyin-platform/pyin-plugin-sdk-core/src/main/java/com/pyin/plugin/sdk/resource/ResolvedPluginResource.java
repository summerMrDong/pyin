package com.pyin.plugin.sdk.resource;

import org.springframework.core.io.Resource;

public record ResolvedPluginResource(
        String location,
        String relativePath,
        Resource resource
) {
}
