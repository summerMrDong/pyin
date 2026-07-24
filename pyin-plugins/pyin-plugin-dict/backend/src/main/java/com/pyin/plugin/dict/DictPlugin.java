package com.pyin.plugin.dict;

import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.sdk.annotation.PluginComponent;

@PluginComponent
public class DictPlugin implements PyinPlugin {

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder("dict")
                .pluginName("字典管理")
                .pluginVersion("1.0.0")
                .basePath("/plugins/dict")
                .entryJs("/plugin-static/dict/assets/remoteEntry.js")
                .build();
    }
}
