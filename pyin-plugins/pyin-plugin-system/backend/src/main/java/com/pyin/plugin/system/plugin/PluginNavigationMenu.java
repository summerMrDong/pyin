package com.pyin.plugin.system.plugin;

import com.pyin.plugin.spi.model.PluginMenuOpenMode;
import com.pyin.plugin.spi.model.PluginMenuType;
import java.util.List;

public record PluginNavigationMenu(
        String code,
        String name,
        PluginMenuType type,
        String path,
        String url,
        String icon,
        Integer sort,
        String permissionCode,
        PluginMenuOpenMode openMode,
        String page,
        List<PluginNavigationMenu> children
) {
}
