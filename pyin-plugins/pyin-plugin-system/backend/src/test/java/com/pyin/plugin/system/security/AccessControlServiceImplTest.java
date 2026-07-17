package com.pyin.plugin.system.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pyin.plugin.common.security.PluginAdminAccessDecision;
import com.pyin.plugin.system.plugin.CompiledPluginApiRegistry;
import com.pyin.plugin.spi.model.PluginAccessMode;
import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessControlServiceImplTest {

    private CompiledPluginApiRegistry compiledPluginApiRegistry;
    private AccessControlServiceImpl accessControlService;

    @BeforeEach
    void setUp() {
        compiledPluginApiRegistry = new CompiledPluginApiRegistry();
        accessControlService = new AccessControlServiceImpl(compiledPluginApiRegistry);
    }

    @Test
    void shouldReturnPermissionCodeWhenRegisteredApiMatches() {
        registerApis(
                new PluginApiDefinition("/admin/items/{id}", "GET", "/internal/items/{id}", PluginAccessMode.CENTER_ADMIN_ONLY, "config:view", true)
        );

        PluginAdminAccessDecision decision = accessControlService.checkAccess("config", "GET", "/admin/items/123");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.permissionCode()).isEqualTo("config:view");
    }

    @Test
    void shouldAllowWithoutPermissionCodeForLoginOnlyApi() {
        registerApis(
                new PluginApiDefinition("/admin/items/{id}", "GET", "/internal/items/{id}", PluginAccessMode.CENTER_ADMIN_ONLY, "", true)
        );

        PluginAdminAccessDecision decision = accessControlService.checkAccess("config", "GET", "/admin/items/123");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.permissionCode()).isBlank();
    }

    @Test
    void shouldDenyWhenApiIsNotRegistered() {
        registerApis(
                new PluginApiDefinition("/admin/items", "GET", "/internal/items", PluginAccessMode.CENTER_ADMIN_ONLY, "config:view", true)
        );

        PluginAdminAccessDecision decision = accessControlService.checkAccess("config", "GET", "/admin/unknown");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("PYIN-PLUGIN-403");
    }

    @Test
    void shouldRespectHttpMethodWhenMatchingApi() {
        registerApis(
                new PluginApiDefinition("/admin/items/{id}", "GET", "/internal/items/{id}", PluginAccessMode.CENTER_ADMIN_ONLY, "config:view", true)
        );

        PluginAdminAccessDecision decision = accessControlService.checkAccess("config", "POST", "/admin/items/123");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("PYIN-PLUGIN-403");
    }

    @Test
    void shouldPreferMoreSpecificPattern() {
        registerApis(
                new PluginApiDefinition("/admin/items/{id}", "GET", "/internal/items/{id}", PluginAccessMode.CENTER_ADMIN_ONLY, "config:view", true),
                new PluginApiDefinition("/admin/items/special", "GET", "/internal/items/special", PluginAccessMode.CENTER_ADMIN_ONLY, "config:special", true)
        );

        PluginAdminAccessDecision decision = accessControlService.checkAccess("config", "GET", "/admin/items/special");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.permissionCode()).isEqualTo("config:special");
    }

    private void registerApis(PluginApiDefinition... apis) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId("config");
        descriptor.setPluginName("配置插件");
        descriptor.setPluginVersion("1.0.0");
        descriptor.setApis(List.of(apis));
        compiledPluginApiRegistry.replace(compiledPluginApiRegistry.compile(descriptor));
    }
}
