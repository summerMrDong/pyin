package com.pyin.plugin.file;

import com.pyin.plugin.sdk.annotation.PluginComponent;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;

@PluginComponent
public class FilePlugin implements PyinPlugin {

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder("file")
                .pluginName("文件管理")
                .pluginVersion("1.0.0")
                .basePath("/plugins/file")
                .entryJs("/plugin-static/file/assets/remoteEntry.js")
                .build();
    }
}
