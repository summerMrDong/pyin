package com.pyin.plugin.runtime.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pyin.plugin.runtime.loader.PluginRuntimeProperties;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.sdk.manifest.PluginDescriptorAssembler;
import com.pyin.plugin.spi.PluginMetadataSynchronizer;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.ApplicationContext;

class PluginManagerTest {

    @Test
    void shouldRenderSummaryWithAnOpenRightSide() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
        try {
            String summary = PluginManager.buildLoadedPluginsSummary(List.of(registeredPlugin("config", "配置管理")));
            List<String> lines = summary.lines().filter(line -> !line.isEmpty()).toList();

            assertThat(lines).hasSize(7);
            assertThat(lines).allSatisfy(line -> assertThat(line.charAt(0)).isIn('┌', '├', '│', '└'));
            assertThat(lines).noneSatisfy(line -> assertThat(line).endsWith("│"));
            assertThat(lines.get(0)).contains("Pyin 已加载插件 (1)");
        } finally {
            AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
        }
    }

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

    private RegisteredPlugin registeredPlugin(String pluginId, String pluginName) {
        ResolvedPluginDescriptor descriptor = descriptor(pluginId);
        descriptor.setPluginName(pluginName);
        descriptor.setBasePath("/plugins/" + pluginId);
        descriptor.setEntryJs("/plugin-static/" + pluginId + "/assets/remoteEntry.js");
        return new RegisteredPlugin(
                pluginId,
                "embedded:" + pluginId,
                descriptor,
                plugin(pluginId),
                PluginSourceType.EMBEDDED_SYSTEM,
                PluginRuntimeStatus.STARTED,
                Path.of(pluginId),
                null,
                null,
                Instant.now()
        );
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PluginMetadataSynchronizer> metadataSynchronizerProvider() {
        return mock(ObjectProvider.class);
    }
}
