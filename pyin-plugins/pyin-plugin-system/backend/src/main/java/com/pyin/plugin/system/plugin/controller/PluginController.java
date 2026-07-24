package com.pyin.plugin.system.plugin.controller;


import com.pyin.plugin.system.plugin.model.PluginWorkspaceFrontend;
import com.pyin.plugin.system.plugin.model.PluginWorkspaceItem;
import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;

@AdminMapping("/plugins")
public class PluginController {

    private final PluginRegistry pluginRegistry;

    public PluginController(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> plugins = pluginRegistry.all().stream()
                .sorted(Comparator.comparing(RegisteredPlugin::pluginId))
                .map(plugin -> Map.<String, Object>of(
                        "pluginId", plugin.pluginId(),
                        "pluginName", plugin.descriptor().getPluginName(),
                        "status", plugin.status().name(),
                        "sourceType", plugin.sourceType().name()
                ))
                .toList();
        return Result.ok(plugins);
    }

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping("/{pluginId}")
    public Result<Map<String, Object>> detail(@PathVariable String pluginId) {
        RegisteredPlugin plugin = pluginRegistry.getRegistration(pluginId);
        if (plugin == null) {
            return Result.fail("PYIN-PLUGIN-404", "Plugin not found: " + pluginId);
        }
        return Result.ok(Map.of(
                "pluginId", plugin.pluginId(),
                "pluginName", plugin.descriptor().getPluginName(),
                "status", plugin.status().name(),
                "sourceType", plugin.sourceType().name()
        ));
    }

    @Permission(code = "system:view", name = "系统查看")
    @PostMapping("/upload")
    public Result<Void> upload() {
        return Result.ok();
    }

    @RequestMapping(
            path = {"/{pluginId}/install", "/{pluginId}/start", "/{pluginId}/stop", "/{pluginId}/restart", "/{pluginId}/upgrade"},
            method = RequestMethod.POST
    )
    @Permission(code = "system:view", name = "系统查看")
    public Result<Void> lifecycle(@PathVariable String pluginId) {
        return Result.ok();
    }

    @Permission(code = "system:view", name = "系统查看")
    @DeleteMapping("/{pluginId}")
    public Result<Void> delete(@PathVariable String pluginId) {
        return Result.ok();
    }

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping("/{pluginId}/logs")
    public Result<List<String>> logs(@PathVariable String pluginId) {
        return Result.ok(List.of("No logs yet for " + pluginId));
    }

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping("/manifest")
    public Result<List<Map<String, Object>>> manifest() {
        return Result.ok(pluginRegistry.all().stream()
                .map(plugin -> Map.<String, Object>of(
                        "pluginId", plugin.pluginId(),
                        "pluginName", plugin.descriptor().getPluginName(),
                        "sourceType", plugin.sourceType().name()
                ))
                .toList());
    }

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping("/workspaces")
    public Result<List<PluginWorkspaceItem>> workspaces() {
        List<PluginWorkspaceItem> plugins = pluginRegistry.all().stream()
                .filter(plugin -> plugin.status() == PluginRuntimeStatus.STARTED)
                .sorted(Comparator
                        .comparing((RegisteredPlugin plugin) -> plugin.pluginId().equals("system") ? 0 : 1)
                        .thenComparing(plugin -> plugin.descriptor().getPluginName(), Comparator.nullsLast(String::compareTo)))
                .map(this::toWorkspaceItem)
                .toList();
        return Result.ok(plugins);
    }

    private PluginWorkspaceItem toWorkspaceItem(RegisteredPlugin plugin) {
        ResolvedPluginDescriptor manifest = plugin.descriptor();
        PluginWorkspaceFrontend frontendInfo = new PluginWorkspaceFrontend(
                manifest.getEntryJs()
        );

        return new PluginWorkspaceItem(
                manifest.getPluginId(),
                manifest.getPluginName(),
                plugin.status().name(),
                plugin.sourceType().name(),
                frontendInfo
        );
    }
}
