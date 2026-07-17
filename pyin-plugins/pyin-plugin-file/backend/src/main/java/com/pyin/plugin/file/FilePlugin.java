package com.pyin.plugin.file;

import com.pyin.plugin.sdk.annotation.PluginComponent;
import com.pyin.plugin.sdk.standalone.StandalonePluginProperties;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.PluginMenu;
import com.pyin.plugin.spi.model.PluginMenuType;
import com.pyin.plugin.spi.model.PluginRuntimeMode;
import com.pyin.plugin.spi.model.PluginType;
import java.util.List;

@PluginComponent
public class FilePlugin implements PyinPlugin {

    private final StandalonePluginProperties standalonePluginProperties;

    public FilePlugin(StandalonePluginProperties standalonePluginProperties) {
        this.standalonePluginProperties = standalonePluginProperties;
    }

    @Override
    public String pluginId() {
        return "file";
    }

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder()
                .pluginId("file")
                .pluginName("文件管理")
                .pluginType(PluginType.EXTERNAL)
                .runtimeMode(PluginRuntimeMode.STANDALONE)
                .pluginVersion("1.0.0")
                .basePath("/plugins/file")
                .entryJs("/plugin-static/file/assets/remoteEntry.js")
                .remoteName("file")
                .exposedModule("./FileRemoteApp")
                .backendBaseUrl(standalonePluginProperties.getResolvedBackendBaseUrl())
                .frontendBaseUrl(standalonePluginProperties.getResolvedFrontendBaseUrl())
                .healthUrl(standalonePluginProperties.getResolvedHealthUrl())
                .build();
    }

    @Override
    public List<PluginMenu> menus() {
        return List.of(new PluginMenu(
                "file",
                "文件管理",
                PluginMenuType.ROUTE,
                "/plugins/file",
                null,
                "FolderOpen",
                160,
                "file:view",
                null,
                null,
                List.of()
        ));
    }
}
