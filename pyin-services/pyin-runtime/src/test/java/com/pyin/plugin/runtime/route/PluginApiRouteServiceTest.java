package com.pyin.plugin.runtime.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.PluginAccessMode;
import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginApiRouteServiceTest {

    private PluginRegistry pluginRegistry;
    private PluginCompiledRegistryRefresher refresher;
    private PluginApiRouteService routeService;

    @BeforeEach
    void setUp() {
        pluginRegistry = new PluginRegistry();
        CompiledPluginApiRegistry compiledPluginApiRegistry = new CompiledPluginApiRegistry();
        refresher = new PluginCompiledRegistryRefresher(compiledPluginApiRegistry);
        routeService = new PluginApiRouteService(pluginRegistry, compiledPluginApiRegistry);
    }

    @Test
    void shouldResolveAdminRouteWithPermissionAndRuntimeFacts() {
        ResolvedPluginDescriptor descriptor = descriptor("dict", List.of(
                new PluginApiDefinition("/items/{id}", "GET", "/plugins/dict/admin/items/{id}",
                        PluginAccessMode.CENTER_ADMIN_ONLY, "dict:view", true)
        ));
        pluginRegistry.register(plugin(descriptor, PluginSourceType.EMBEDDED_SYSTEM, PluginRuntimeStatus.STARTED, null));
        refresher.refresh(descriptor);

        PluginApiRouteService.PluginRouteResolution resolution =
                routeService.resolve("dict", PluginApiChannel.ADMIN, "GET", "/items/100");

        assertThat(resolution.plugin()).isNotNull();
        assertThat(resolution.route()).hasValueSatisfying(route -> {
            assertThat(route.pluginId()).isEqualTo("dict");
            assertThat(route.status()).isEqualTo(PluginRuntimeStatus.STARTED);
            assertThat(route.sourceType()).isEqualTo(PluginSourceType.EMBEDDED_SYSTEM);
            assertThat(route.permissionCode()).isEqualTo("dict:view");
            assertThat(route.auditEnabled()).isTrue();
        });
    }

    @Test
    void shouldFilterByAccessMode() {
        ResolvedPluginDescriptor descriptor = descriptor("dict", List.of(
                new PluginApiDefinition("/items", "GET", "/plugins/dict/admin/items",
                        PluginAccessMode.CENTER_ADMIN_ONLY, "dict:view", true)
        ));
        pluginRegistry.register(plugin(descriptor, PluginSourceType.EMBEDDED_SYSTEM, PluginRuntimeStatus.STARTED, null));
        refresher.refresh(descriptor);

        PluginApiRouteService.PluginRouteResolution resolution =
                routeService.resolve("dict", PluginApiChannel.CLIENT, "GET", "/items");

        assertThat(resolution.plugin()).isNotNull();
        assertThat(resolution.route()).isEmpty();
    }

    @Test
    void shouldReturnPluginStatusEvenWhenUnavailable() {
        ResolvedPluginDescriptor descriptor = descriptor("dict", List.of(
                new PluginApiDefinition("/items", "GET", "/plugins/dict/admin/items",
                        PluginAccessMode.CENTER_ADMIN_ONLY, "dict:view", true)
        ));
        pluginRegistry.register(plugin(descriptor, PluginSourceType.STANDALONE_NODE, PluginRuntimeStatus.UNAVAILABLE, "http://127.0.0.1:18080"));
        refresher.refresh(descriptor);

        PluginApiRoute route = routeService.resolve("dict", PluginApiChannel.ADMIN, "GET", "/items")
                .route()
                .orElseThrow();

        assertThat(route.status()).isEqualTo(PluginRuntimeStatus.UNAVAILABLE);
        assertThat(route.backendBaseUrl()).isEqualTo("http://127.0.0.1:18080");
    }

    @Test
    void shouldDetectConflictingPathVariables() {
        ResolvedPluginDescriptor descriptor = descriptor("dict", List.of(
                new PluginApiDefinition("/items/{id}", "GET", "/plugins/dict/admin/items/{id}",
                        PluginAccessMode.CENTER_ADMIN_ONLY, "dict:view", true),
                new PluginApiDefinition("/items/{itemId}", "GET", "/plugins/dict/admin/items/{itemId}",
                        PluginAccessMode.CENTER_ADMIN_ONLY, "dict:view", true)
        ));

        assertThatThrownBy(() -> refresher.refresh(descriptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicting plugin api path pattern");
    }

    private ResolvedPluginDescriptor descriptor(String pluginId, List<PluginApiDefinition> apis) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId(pluginId);
        descriptor.setApis(apis);
        return descriptor;
    }

    private RegisteredPlugin plugin(
            ResolvedPluginDescriptor descriptor,
            PluginSourceType sourceType,
            PluginRuntimeStatus status,
            String backendBaseUrl
    ) {
        return new RegisteredPlugin(
                descriptor.getPluginId(),
                sourceType.name().toLowerCase() + ":" + descriptor.getPluginId(),
                descriptor,
                null,
                sourceType,
                status,
                null,
                backendBaseUrl,
                null,
                Instant.now()
        );
    }
}
