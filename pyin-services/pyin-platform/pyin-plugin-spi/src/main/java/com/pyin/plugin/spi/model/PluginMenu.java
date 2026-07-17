package com.pyin.plugin.spi.model;

import java.util.ArrayList;
import java.util.List;

public record PluginMenu(
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
        List<PluginMenu> children
) {

    public PluginMenu {
        children = children == null ? List.of() : List.copyOf(new ArrayList<>(children));
    }
}
