package com.pyin.plugin.config;

import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.sdk.annotation.PluginComponent;


@PluginComponent
public class ConfigPlugin implements PyinPlugin {

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder("config")
                .pluginName("配置管理")
                .pluginVersion("1.0.0")
                .basePath("/plugins/config")
                .entryJs("/plugin-static/config/assets/remoteEntry.js")
                .build();
    }
}
