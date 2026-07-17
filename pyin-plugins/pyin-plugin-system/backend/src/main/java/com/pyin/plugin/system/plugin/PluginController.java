package com.pyin.plugin.system.plugin;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.spi.model.PluginMenu;
import com.pyin.plugin.spi.model.PluginPermission;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/plugins")
public class PluginController {

    private final PluginRegistry pluginRegistry;

    public PluginController(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> plugins = pluginRegistry.all().stream()
                .sorted(Comparator.comparing(RegisteredPlugin::pluginId))
                .map(plugin -> Map.<String, Object>of(
                        "pluginId", plugin.pluginId(),
                        "pluginName", plugin.descriptor().getPluginName(),
                        "pluginType", plugin.descriptor().getPluginType().name(),
                        "status", plugin.status().name(),
                        "sourceType", plugin.sourceType().name()
                ))
                .toList();
        return Result.ok(plugins);
    }

    @GetMapping("/{pluginId}")
    public Result<Map<String, Object>> detail(@PathVariable String pluginId) {
        RegisteredPlugin plugin = pluginRegistry.getRegistration(pluginId);
        if (plugin == null) {
            return Result.fail("PYIN-PLUGIN-404", "Plugin not found: " + pluginId);
        }
        return Result.ok(Map.of(
                "pluginId", plugin.pluginId(),
                "pluginName", plugin.descriptor().getPluginName(),
                "pluginType", plugin.descriptor().getPluginType().name(),
                "status", plugin.status().name(),
                "sourceType", plugin.sourceType().name()
        ));
    }

    @PostMapping("/upload")
    public Result<Void> upload() {
        return Result.ok();
    }

    @RequestMapping(
            path = {"/{pluginId}/install", "/{pluginId}/start", "/{pluginId}/stop", "/{pluginId}/restart", "/{pluginId}/upgrade"},
            method = RequestMethod.POST
    )
    public Result<Void> lifecycle(@PathVariable String pluginId) {
        return Result.ok();
    }

    @DeleteMapping("/{pluginId}")
    public Result<Void> delete(@PathVariable String pluginId) {
        return Result.ok();
    }

    @GetMapping("/{pluginId}/logs")
    public Result<List<String>> logs(@PathVariable String pluginId) {
        return Result.ok(List.of("No logs yet for " + pluginId));
    }

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

    @GetMapping("/navigation")
    public Result<List<PluginNavigationItem>> navigation() {
        List<PluginNavigationItem> plugins = pluginRegistry.all().stream()
                .filter(plugin -> plugin.status() == PluginRuntimeStatus.STARTED)
                .sorted(Comparator.comparing(RegisteredPlugin::pluginId))
                .map(this::toNavigationItem)
                .filter(item -> !item.menus().isEmpty())
                .toList();
        return Result.ok(plugins);
    }

    private PluginNavigationItem toNavigationItem(RegisteredPlugin plugin) {
        ResolvedPluginDescriptor manifest = plugin.descriptor();
        PluginNavigationFrontend frontendInfo = new PluginNavigationFrontend(
                manifest.getRemoteName(),
                manifest.getEntryJs(),
                manifest.getExposedModules()
        );

        List<PluginNavigationMenu> menus = manifest.getMenus().stream()
                .map(this::toNavigationMenu)
                .toList();

        List<PluginNavigationPermission> permissions = manifest.getPermissions().stream()
                .map(this::toNavigationPermission)
                .toList();

        return new PluginNavigationItem(
                manifest.getPluginId(),
                manifest.getPluginName(),
                manifest.getPluginType().name(),
                plugin.status().name(),
                plugin.sourceType().name(),
                frontendInfo,
                menus,
                permissions
        );
    }

    private PluginNavigationMenu toNavigationMenu(PluginMenu menu) {
        return new PluginNavigationMenu(
                menu.code(),
                menu.name(),
                menu.type(),
                menu.path(),
                menu.url(),
                menu.icon(),
                menu.sort(),
                menu.permissionCode(),
                menu.openMode(),
                menu.page(),
                menu.children().stream().map(this::toNavigationMenu).toList()
        );
    }

    private PluginNavigationPermission toNavigationPermission(PluginPermission permission) {
        return new PluginNavigationPermission(
                permission.code(),
                permission.name(),
                permission.resourceType()
        );
    }
}
