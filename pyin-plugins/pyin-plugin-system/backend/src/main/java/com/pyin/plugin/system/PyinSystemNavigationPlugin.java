package com.pyin.plugin.system;

import com.pyin.plugin.sdk.annotation.PluginComponent;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;

@PluginComponent
public class PyinSystemNavigationPlugin implements PyinPlugin {

    public static final String PLUGIN_ID = "system";

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder(PLUGIN_ID)
                .pluginName("系统模块")
                .pluginVersion("1.0.0")
                .basePath("/plugins/system")
                .entryJs("/plugin-static/system/assets/remoteEntry.js")
                .build();
    }
}
