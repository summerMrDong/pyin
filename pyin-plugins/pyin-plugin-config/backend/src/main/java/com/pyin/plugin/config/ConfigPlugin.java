package com.pyin.plugin.config;

import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.PluginMenu;
import com.pyin.plugin.spi.model.PluginMenuType;
import com.pyin.plugin.spi.model.PluginRuntimeMode;
import com.pyin.plugin.spi.model.PluginType;
import com.pyin.plugin.sdk.annotation.PluginComponent;

import java.util.List;

@PluginComponent
public class ConfigPlugin implements PyinPlugin {

    @Override
    public String pluginId() {
        return "config";
    }

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder()
                .pluginId("config")
                .pluginName("配置管理")
                .pluginType(PluginType.SYSTEM)
                .runtimeMode(PluginRuntimeMode.EMBEDDED)
                .pluginVersion("1.0.0")
                .basePath("/plugins/config")
                .entryJs("/plugin-static/config/assets/remoteEntry.js")
                .remoteName("config")
                .exposedModule("./ConfigRemoteApp")
                .build();
    }

    @Override
    public List<PluginMenu> menus() {
        return List.of(new PluginMenu(
                "config",
                "配置管理",
                PluginMenuType.ROUTE,
                "/plugins/config",
                null,
                "SlidersHorizontal",
                120,
                "config:view",
                null,
                null,
                List.of()
        ));
    }
}
