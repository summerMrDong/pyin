package com.pyin.plugin.sdk.resource;

import java.util.List;
import org.springframework.core.io.Resource;

public interface PluginResourceResolver {

    ResolvedPluginResource resolve(String pluginId, String relativePath);

    ResolvedPluginResource resolve(String pluginId, String relativePath, List<String> locations);

    default Resource resolveRemoteEntry(String pluginId) {
        return resolve(pluginId, "remoteEntry.js").resource();
    }

    default Resource resolveAsset(String pluginId, String assetRelativePath) {
        return resolve(pluginId, assetRelativePath).resource();
    }
}
