package com.pyin.plugin.system.plugin;

import java.util.List;

public record PluginNavigationFrontend(
        String remoteName,
        String remoteEntry,
        List<String> exposedModules
) {
}
