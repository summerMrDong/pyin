package com.pyin.plugin.system.plugin;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.sdk.manifest.PluginDescriptorValidator;
import com.pyin.plugin.spi.PluginMetadataSynchronizer;
import com.pyin.plugin.spi.model.PluginNodeRegistration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
    @RequestMapping("/api/core/plugin-nodes")
public class PluginNodeController {

    private final PluginRegistry pluginRegistry;
    private final PluginMetadataSynchronizer pluginMetadataSynchronizer;

    public PluginNodeController(
            PluginRegistry pluginRegistry,
            PluginMetadataSynchronizer pluginMetadataSynchronizer
    ) {
        this.pluginRegistry = pluginRegistry;
        this.pluginMetadataSynchronizer = pluginMetadataSynchronizer;
    }

    @PostMapping("/register")
    public Result<RegisteredPlugin> register(@RequestBody PluginNodeRegistration registration) {
        if (registration == null || registration.getDescriptor() == null) {
            return Result.fail("PYIN-PLUGIN-400", "Plugin node registration descriptor must not be null");
        }
        PluginDescriptorValidator.validate(registration.getDescriptor());
        pluginMetadataSynchronizer.sync(registration.getDescriptor());
        return Result.ok(pluginRegistry.registerStandalone(registration));
    }

    @PostMapping("/{pluginId}/{nodeId}/heartbeat")
    public Result<RegisteredPlugin> heartbeat(@PathVariable String pluginId, @PathVariable String nodeId) {
        RegisteredPlugin plugin = pluginRegistry.heartbeat(pluginId, nodeId);
        if (plugin == null) {
            return Result.fail("PYIN-PLUGIN-404", "Plugin not found: " + pluginId);
        }
        return Result.ok(plugin);
    }

    @PostMapping("/{pluginId}/{nodeId}/offline")
    public Result<RegisteredPlugin> offline(@PathVariable String pluginId, @PathVariable String nodeId) {
        RegisteredPlugin plugin = pluginRegistry.markOffline(pluginId, nodeId);
        if (plugin == null) {
            return Result.fail("PYIN-PLUGIN-404", "Plugin not found: " + pluginId);
        }
        return Result.ok(plugin);
    }
}
