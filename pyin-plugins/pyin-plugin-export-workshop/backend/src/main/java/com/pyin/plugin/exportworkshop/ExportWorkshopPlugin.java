package com.pyin.plugin.exportworkshop;

import com.pyin.plugin.sdk.annotation.PluginComponent;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;

@PluginComponent
public class ExportWorkshopPlugin implements PyinPlugin {

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder("export-workshop")
                .pluginName("导出工坊")
                .pluginVersion("1.0.0")
                .basePath("/plugins/export-workshop")
                .entryJs("/plugin-static/export-workshop/assets/remoteEntry.js")
                .build();
    }
}
