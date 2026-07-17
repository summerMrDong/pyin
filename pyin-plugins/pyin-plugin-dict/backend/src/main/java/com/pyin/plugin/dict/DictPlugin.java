package com.pyin.plugin.dict;

import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.PluginMenu;
import com.pyin.plugin.spi.model.PluginMenuType;
import com.pyin.plugin.spi.model.PluginRuntimeMode;
import com.pyin.plugin.spi.model.PluginType;
import com.pyin.plugin.sdk.annotation.PluginComponent;
import java.util.List;

@PluginComponent
public class DictPlugin implements PyinPlugin {

    @Override
    public String pluginId() {
        return "dict";
    }

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder()
                .pluginId("dict")
                .pluginName("字典管理")
                .pluginType(PluginType.SYSTEM)
                .runtimeMode(PluginRuntimeMode.EMBEDDED)
                .pluginVersion("1.0.0")
                .basePath("/plugins/dict")
                .entryJs("/plugin-static/dict/assets/remoteEntry.js")
                .remoteName("dict")
                .exposedModule("./DictRemoteApp")
                .build();
    }

    @Override
    public List<PluginMenu> menus() {
        return List.of(new PluginMenu(
                "dict",
                "字典管理",
                PluginMenuType.ROUTE,
                "/plugins/dict",
                null,
                "BookKey",
                110,
                "dict:view",
                null,
                null,
                List.of()
        ));
    }
}
