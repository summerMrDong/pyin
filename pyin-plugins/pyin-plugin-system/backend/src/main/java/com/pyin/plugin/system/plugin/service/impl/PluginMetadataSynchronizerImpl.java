package com.pyin.plugin.system.plugin.service.impl;

import com.pyin.plugin.spi.PluginMetadataSynchronizer;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import com.pyin.plugin.runtime.route.PluginCompiledRegistryRefresher;
import com.pyin.plugin.system.plugin.support.PluginMetadataStore;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn("systemPluginIdMigrator")
public class PluginMetadataSynchronizerImpl implements PluginMetadataSynchronizer {

    private final PluginMetadataStore pluginMetadataStore;
    private final PluginCompiledRegistryRefresher pluginCompiledRegistryRefresher;

    public PluginMetadataSynchronizerImpl(
            PluginMetadataStore pluginMetadataStore,
            PluginCompiledRegistryRefresher pluginCompiledRegistryRefresher
    ) {
        this.pluginMetadataStore = pluginMetadataStore;
        this.pluginCompiledRegistryRefresher = pluginCompiledRegistryRefresher;
    }

    @Override
    @Transactional
    public void sync(ResolvedPluginDescriptor descriptor) {
        if (descriptor == null || descriptor.getPluginId() == null || descriptor.getPluginId().isBlank()) {
            return;
        }
        pluginMetadataStore.upsertPlugin(descriptor);
        pluginMetadataStore.replacePluginCapabilities(descriptor);
        pluginCompiledRegistryRefresher.refresh(descriptor);
    }
}
