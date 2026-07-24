package com.pyin.plugin.runtime.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pyin.plugin.runtime.loader.PluginRuntimeProperties;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.sdk.manifest.PluginDescriptorAssembler;
import com.pyin.plugin.spi.PluginMetadataSynchronizer;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

class PluginManagerTest {

    @Test
    void shouldRejectClasspathPluginOutsideEmbeddedAllowlist() throws Exception {
        PluginRuntimeProperties properties = new PluginRuntimeProperties();
        properties.setEmbeddedPluginIds(List.of("system"));
        PluginRegistry registry = new PluginRegistry();
        PyinPlugin unapprovedPlugin = plugin("external");
        PluginDescriptorAssembler assembler = mock(PluginDescriptorAssembler.class);
        when(assembler.assemble(any(ApplicationContext.class), same(unapprovedPlugin)))
                .thenReturn(descriptor("external"));

        PluginManager manager = new PluginManager(
                properties,
                registry,
                List.of(unapprovedPlugin),
                assembler,
                mock(ApplicationContext.class),
                metadataSynchronizerProvider()
        );

        manager.bootstrap();

        assertThat(registry.getRegistration("external")).isNull();
    }

    private PyinPlugin plugin(String pluginId) {
        return () -> PluginManifest.builder(pluginId)
                .pluginName(pluginId)
                .pluginVersion("1.0.0")
                .build();
    }

    private ResolvedPluginDescriptor descriptor(String pluginId) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId(pluginId);
        descriptor.setPluginName(pluginId);
        descriptor.setPluginVersion("1.0.0");
        return descriptor;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PluginMetadataSynchronizer> metadataSynchronizerProvider() {
        return mock(ObjectProvider.class);
    }
}
