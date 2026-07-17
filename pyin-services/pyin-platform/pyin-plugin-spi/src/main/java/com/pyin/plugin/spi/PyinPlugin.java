package com.pyin.plugin.spi;

import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.PluginMenu;
import java.util.List;

public interface PyinPlugin {

    String pluginId();

    PluginManifest manifest();

    List<PluginMenu> menus();
}
