package com.pyin.plugin.statemachine;

import com.pyin.plugin.sdk.annotation.PluginComponent;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;

@PluginComponent
public class StateMachinePlugin implements PyinPlugin {

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder("state-machine")
                .pluginName("状态机设计")
                .pluginVersion("1.0.0")
                .basePath("/plugins/state-machine")
                .entryJs("/plugin-static/state-machine/assets/remoteEntry.js")
                .build();
    }
}
