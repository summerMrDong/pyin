package com.pyin.plugin.runtime.registry;

import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.nio.file.Path;
import java.time.Instant;

public record RegisteredPlugin(
        String pluginId,
        String nodeId,
        ResolvedPluginDescriptor descriptor,
        PyinPlugin pluginInstance,
        PluginSourceType sourceType,
        PluginRuntimeStatus status,
        Path pluginHome,
        String backendBaseUrl,
        String frontendBaseUrl,
        Instant lastHeartbeatAt
) {
}
