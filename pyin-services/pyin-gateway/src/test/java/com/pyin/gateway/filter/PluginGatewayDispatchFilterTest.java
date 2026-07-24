package com.pyin.gateway.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pyin.center.auth.authentication.AdminRequestAuthenticator;
import com.pyin.center.auth.authentication.AuthorizationException;
import com.pyin.center.auth.authentication.ClientRequestAuthenticator;
import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.forward.StandalonePluginForwardService;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.route.CompiledPluginApiRegistry;
import com.pyin.plugin.runtime.route.PluginApiRouteService;
import com.pyin.plugin.runtime.route.PluginCompiledRegistryRefresher;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.PluginAccessMode;
import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class PluginGatewayDispatchFilterTest {

    private MockMvc mockMvc;
    private PluginRegistry pluginRegistry;
    private PluginCompiledRegistryRefresher refresher;
    private AdminRequestAuthenticator adminAuthService;
    private ClientRequestAuthenticator clientAuthService;
    private StandalonePluginForwardService forwardService;

    @BeforeEach
    void setUp() {
        pluginRegistry = new PluginRegistry();
        CompiledPluginApiRegistry apiRegistry = new CompiledPluginApiRegistry();
        refresher = new PluginCompiledRegistryRefresher(apiRegistry);
        adminAuthService = Mockito.mock(AdminRequestAuthenticator.class);
        clientAuthService = Mockito.mock(ClientRequestAuthenticator.class);
        forwardService = Mockito.mock(StandalonePluginForwardService.class);
        PluginGatewayExceptionResolver resolver = new PluginGatewayExceptionResolver();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestGatewayController())
                .addFilters(
                        new PluginGatewayDispatchFilter(
                                new PluginApiRouteService(pluginRegistry, apiRegistry),
                                adminAuthService,
                                clientAuthService,
                                forwardService,
                                resolver
                        ),
                        new PluginGatewayInternalGuardFilter(resolver)
                )
                .build();
    }

    @Test
    void shouldNotExposeLegacyEmbeddedControllerPath() throws Exception {
        register("dict", PluginSourceType.EMBEDDED_SYSTEM, PluginRuntimeStatus.STARTED);

        mockMvc.perform(get("/dict/admin/list"))
                .andExpect(status().isNotFound());

        verify(adminAuthService, never()).authenticate(any());
        verify(forwardService, never()).forward(any(), any(), any(), any(), any());
    }

    @Test
    void shouldAllowCanonicalPathToEmbeddedController() throws Exception {
        register("dict", PluginSourceType.EMBEDDED_SYSTEM, PluginRuntimeStatus.STARTED);
        when(adminAuthService.authenticate("dict:view")).thenReturn(AuthenticatedPrincipal.adminUser(1L, "admin"));

        mockMvc.perform(get("/plugins/dict/admin/list"))
                .andExpect(status().isOk())
                .andExpect(content().string("local:dict"));
    }

    @Test
    void shouldRejectUnpublishedApiBeforeAuth() throws Exception {
        register("dict", PluginSourceType.EMBEDDED_SYSTEM, PluginRuntimeStatus.STARTED);

        mockMvc.perform(get("/plugins/dict/admin/unknown"))
                .andExpect(status().isNotFound());

        verify(adminAuthService, never()).authenticate(any());
    }

    @Test
    void shouldMapPermissionDeniedToForbidden() throws Exception {
        register("dict", PluginSourceType.EMBEDDED_SYSTEM, PluginRuntimeStatus.STARTED);
        when(adminAuthService.authenticate("dict:view"))
                .thenThrow(new AuthorizationException("缺少插件接口权限：dict:view"));

        mockMvc.perform(get("/plugins/dict/admin/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForwardStandaloneAfterAuth() throws Exception {
        register("dict", PluginSourceType.STANDALONE_NODE, PluginRuntimeStatus.STARTED);
        when(adminAuthService.authenticate("dict:view")).thenReturn(AuthenticatedPrincipal.adminUser(1L, "admin"));
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(4);
            response.setStatus(200);
            response.getWriter().write("forwarded");
            return null;
        }).when(forwardService).forward(any(), any(), eq(AuthenticatedPrincipal.adminUser(1L, "admin")), any(), any());

        mockMvc.perform(get("/plugins/dict/admin/list"))
                .andExpect(status().isOk())
                .andExpect(content().string("forwarded"));

        verify(forwardService).forward(any(), any(), eq(AuthenticatedPrincipal.adminUser(1L, "admin")), any(), any());
    }

    private void register(
            String pluginId,
            PluginSourceType sourceType,
            PluginRuntimeStatus status
    ) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId(pluginId);
        descriptor.setApis(List.of(new PluginApiDefinition(
                "/list",
                "GET",
                "/plugins/" + pluginId + "/admin/list",
                PluginAccessMode.CENTER_ADMIN_ONLY,
                "dict:view",
                true
        )));
        pluginRegistry.register(new RegisteredPlugin(
                pluginId,
                sourceType.name().toLowerCase() + ":" + pluginId,
                descriptor,
                null,
                sourceType,
                status,
                null,
                "http://127.0.0.1:18080",
                null,
                Instant.now()
        ));
        refresher.refresh(descriptor);
    }

    @RestController
    static class TestGatewayController {

        @RequestMapping("/plugins/{pluginId}/admin/list")
        ResponseEntity<String> admin(@PathVariable String pluginId) {
            return ResponseEntity.ok("local:" + pluginId);
        }
    }
}
