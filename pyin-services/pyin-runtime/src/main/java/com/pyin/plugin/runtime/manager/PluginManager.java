package com.pyin.plugin.runtime.manager;

import com.pyin.plugin.runtime.loader.PluginRuntimeProperties;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.sdk.manifest.PluginDescriptorAssembler;
import com.pyin.plugin.spi.PluginMetadataSynchronizer;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(PluginRuntimeProperties.class)
public class PluginManager {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final PluginRuntimeProperties properties;
    private final PluginRegistry pluginRegistry;
    private final List<PyinPlugin> embeddedPlugins;
    private final PluginDescriptorAssembler pluginDescriptorAssembler;
    private final ApplicationContext applicationContext;
    private final ObjectProvider<PluginMetadataSynchronizer> pluginMetadataSynchronizerProvider;

    public PluginManager(
            PluginRuntimeProperties properties,
            PluginRegistry pluginRegistry,
            List<PyinPlugin> embeddedPlugins,
            PluginDescriptorAssembler pluginDescriptorAssembler,
            ApplicationContext applicationContext,
            ObjectProvider<PluginMetadataSynchronizer> pluginMetadataSynchronizerProvider
    ) {
        this.properties = properties;
        this.pluginRegistry = pluginRegistry;
        this.embeddedPlugins = embeddedPlugins;
        this.pluginDescriptorAssembler = pluginDescriptorAssembler;
        this.applicationContext = applicationContext;
        this.pluginMetadataSynchronizerProvider = pluginMetadataSynchronizerProvider;
    }

    @PostConstruct
    public void bootstrap() throws IOException {
        registerEmbeddedPlugins();
    }

    private void registerEmbeddedPlugins() {
        Path sourcePluginsDir = Path.of(properties.getSourcePluginsDir());
        embeddedPlugins.stream()
                .sorted(Comparator.comparing(PyinPlugin::pluginId))
                .forEach(plugin -> registerEmbeddedPlugin(plugin, sourcePluginsDir));
    }

    private void registerEmbeddedPlugin(PyinPlugin plugin, Path sourcePluginsDir) {
        try {
            ResolvedPluginDescriptor descriptor = pluginDescriptorAssembler.assemble(applicationContext, plugin);
            if (descriptor == null) {
                log.warn("Skip embedded plugin '{}' because manifest() returned null", plugin.pluginId());
                return;
            }
            pluginMetadataSynchronizerProvider.ifAvailable(synchronizer -> synchronizer.sync(descriptor));
            pluginRegistry.registerEmbedded(descriptor, plugin, resolveEmbeddedPluginHome(sourcePluginsDir, plugin.pluginId()));
        } catch (Exception exception) {
            log.warn("Failed to register embedded plugin '{}': {}", plugin.pluginId(), exception.getMessage());
        }
    }

    private java.nio.file.Path resolveEmbeddedPluginHome(java.nio.file.Path sourcePluginsDir, String pluginId) {
        java.nio.file.Path artifactPath = sourcePluginsDir.resolve("pyin-plugin-" + pluginId);
        if (java.nio.file.Files.exists(artifactPath)) {
            return artifactPath;
        }
        java.nio.file.Path plainIdPath = sourcePluginsDir.resolve(pluginId);
        if (java.nio.file.Files.exists(plainIdPath)) {
            return plainIdPath;
        }
        return java.nio.file.Path.of(properties.getSystemPluginsDir()).resolve(pluginId);
    }
}
