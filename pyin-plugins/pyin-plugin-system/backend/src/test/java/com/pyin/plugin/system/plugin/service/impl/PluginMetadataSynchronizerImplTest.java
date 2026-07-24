package com.pyin.plugin.system.plugin.service.impl;

import com.pyin.plugin.system.plugin.entity.PluginApiEntity;
import com.pyin.plugin.system.plugin.entity.PluginEntity;
import com.pyin.plugin.system.plugin.entity.PluginResourceEntity;
import com.pyin.plugin.system.plugin.repository.PluginApiRepository;
import com.pyin.plugin.system.plugin.repository.PluginRepository;
import com.pyin.plugin.system.plugin.repository.PluginResourceRepository;
import com.pyin.plugin.runtime.route.CompiledPluginApiRegistry;
import com.pyin.plugin.runtime.route.PluginCompiledRegistryRefresher;
import com.pyin.plugin.system.plugin.support.PluginMetadataStore;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pyin.plugin.system.permission.entity.PluginPermissionEntity;
import com.pyin.plugin.system.permission.repository.PluginPermissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.spi.model.PluginAccessMode;
import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginPermission;
import com.pyin.plugin.spi.model.PluginPermissionResourceType;
import com.pyin.plugin.spi.model.PluginResourceDefinition;
import com.pyin.plugin.spi.model.PluginResourceType;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PluginMetadataSynchronizerImplTest {

    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private PluginPermissionRepository pluginPermissionRepository;

    @Mock
    private PluginApiRepository pluginApiRepository;

    @Mock
    private PluginResourceRepository pluginResourceRepository;

    private CompiledPluginApiRegistry compiledPluginApiRegistry;
    private PluginMetadataSynchronizerImpl synchronizer;

    @BeforeEach
    void setUp() {
        compiledPluginApiRegistry = new CompiledPluginApiRegistry();
        PluginMetadataStore pluginMetadataStore = new PluginMetadataStore(
                pluginRepository,
                pluginPermissionRepository,
                pluginApiRepository,
                pluginResourceRepository,
                new ObjectMapper()
        );
        PluginCompiledRegistryRefresher pluginCompiledRegistryRefresher =
                new PluginCompiledRegistryRefresher(compiledPluginApiRegistry);
        synchronizer = new PluginMetadataSynchronizerImpl(
                pluginMetadataStore,
                pluginCompiledRegistryRefresher
        );
    }

    @Test
    void shouldReplacePluginPermissionsAndApisOnSync() {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId("config");
        descriptor.setPluginName("配置插件");
        descriptor.setPluginVersion("1.0.0");
        descriptor.setPermissions(List.of(
                        new PluginPermission("config:view", "配置查看", PluginPermissionResourceType.MENU),
                        new PluginPermission("config:update", "配置修改", PluginPermissionResourceType.API)
                ));
        descriptor.setApis(List.of(
                new PluginApiDefinition("/items", "GET", "/plugins/config/admin/items", PluginAccessMode.CENTER_ADMIN_ONLY, "config:view", true),
                new PluginApiDefinition("/config/value", "GET", "/plugins/config/open/config/value", PluginAccessMode.CLIENT_SDK_GATEWAY, "", false)
        ));
        descriptor.setResources(List.of(
                new PluginResourceDefinition("config:page", "配置管理", PluginResourceType.PAGE, null, "/plugins/config", "Settings", 100, "config:view", true, Map.of()),
                new PluginResourceDefinition("config:api", "配置接口", PluginResourceType.API, null, "/admin/items", null, null, "config:view", false, Map.of())
        ));

        synchronizer.sync(descriptor);

        verify(pluginRepository).selectById("config");
        verify(pluginRepository).insert(org.mockito.ArgumentMatchers.<PluginEntity>any());
        verify(pluginPermissionRepository).delete(any());
        verify(pluginApiRepository).delete(any());
        verify(pluginResourceRepository).delete(any());
        verify(pluginPermissionRepository, times(2)).insert(org.mockito.ArgumentMatchers.<PluginPermissionEntity>any());
        verify(pluginApiRepository, times(2)).insert(org.mockito.ArgumentMatchers.<PluginApiEntity>any());
        verify(pluginResourceRepository, times(2)).insert(org.mockito.ArgumentMatchers.<PluginResourceEntity>any());
    }

    @Test
    void shouldRejectEquivalentPathPatternsWithinSameMethod() {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId("config");
        descriptor.setPluginName("配置插件");
        descriptor.setPluginVersion("1.0.0");
        descriptor.setApis(List.of(
                new PluginApiDefinition("/items/{id}", "GET", "/items/{id}", PluginAccessMode.CENTER_ADMIN_ONLY, "config:view", true),
                new PluginApiDefinition("/items/{itemId}", "GET", "/items/{itemId}", PluginAccessMode.CENTER_ADMIN_ONLY, "config:view", true)
        ));

        assertThatThrownBy(() -> synchronizer.sync(descriptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicting plugin api path pattern");
    }
}
