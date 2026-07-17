package com.pyin.plugin.runtime.registry;

import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginNodeRegistration;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PluginRegistry {

    private final Map<String, RegisteredPlugin> plugins = new ConcurrentHashMap<>();

    public void register(RegisteredPlugin plugin) {
        plugins.put(plugin.pluginId(), plugin);
    }

    public boolean contains(String pluginId) {
        return plugins.containsKey(pluginId);
    }

    public RegisteredPlugin getRegistration(String pluginId) {
        return plugins.get(pluginId);
    }

    public ResolvedPluginDescriptor get(String pluginId) {
        RegisteredPlugin plugin = plugins.get(pluginId);
        return plugin == null ? null : plugin.descriptor();
    }

    public Collection<RegisteredPlugin> all() {
        return plugins.values();
    }

    public void registerEmbedded(ResolvedPluginDescriptor descriptor, PyinPlugin pluginInstance, Path pluginHome) {
        register(new RegisteredPlugin(
                descriptor.getPluginId(),
                "embedded:" + descriptor.getPluginId(),
                descriptor,
                pluginInstance,
                PluginSourceType.EMBEDDED_SYSTEM,
                PluginRuntimeStatus.STARTED,
                pluginHome,
                null,
                null,
                Instant.now()
        ));
    }

    public RegisteredPlugin registerStandalone(PluginNodeRegistration registration) {
        RegisteredPlugin plugin = new RegisteredPlugin(
                registration.getPluginId(),
                registration.getNodeId(),
                registration.getDescriptor(),
                null,
                PluginSourceType.STANDALONE_NODE,
                PluginRuntimeStatus.STARTED,
                null,
                registration.getBackendBaseUrl(),
                registration.getFrontendBaseUrl(),
                Instant.now()
        );
        register(plugin);
        return plugin;
    }

    public RegisteredPlugin heartbeat(String pluginId, String nodeId) {
        RegisteredPlugin existing = plugins.get(pluginId);
        if (existing == null || !existing.nodeId().equals(nodeId)) {
            return null;
        }
        RegisteredPlugin updated = new RegisteredPlugin(
                existing.pluginId(),
                existing.nodeId(),
                existing.descriptor(),
                existing.pluginInstance(),
                existing.sourceType(),
                PluginRuntimeStatus.STARTED,
                existing.pluginHome(),
                existing.backendBaseUrl(),
                existing.frontendBaseUrl(),
                Instant.now()
        );
        register(updated);
        return updated;
    }

    public RegisteredPlugin markOffline(String pluginId, String nodeId) {
        RegisteredPlugin existing = plugins.get(pluginId);
        if (existing == null || !existing.nodeId().equals(nodeId)) {
            return null;
        }
        RegisteredPlugin updated = new RegisteredPlugin(
                existing.pluginId(),
                existing.nodeId(),
                existing.descriptor(),
                existing.pluginInstance(),
                existing.sourceType(),
                PluginRuntimeStatus.UNAVAILABLE,
                existing.pluginHome(),
                existing.backendBaseUrl(),
                existing.frontendBaseUrl(),
                existing.lastHeartbeatAt()
        );
        register(updated);
        return updated;
    }
}
