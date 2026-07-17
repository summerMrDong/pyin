package com.pyin.plugin.spi;

import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;

public interface PluginMetadataSynchronizer {

    void sync(ResolvedPluginDescriptor descriptor);
}
