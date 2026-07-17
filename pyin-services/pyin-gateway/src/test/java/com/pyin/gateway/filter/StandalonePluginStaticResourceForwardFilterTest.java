package com.pyin.gateway.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.forward.StandalonePluginStaticResourceForwardService;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class StandalonePluginStaticResourceForwardFilterTest {

    private MockMvc mockMvc;
    private PluginRegistry pluginRegistry;
    private StandalonePluginStaticResourceForwardService forwardService;

    @BeforeEach
    void setUp() {
        pluginRegistry = new PluginRegistry();
        forwardService = Mockito.mock(StandalonePluginStaticResourceForwardService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestStaticController())
                .addFilters(new StandalonePluginStaticResourceForwardFilter(
                        pluginRegistry,
                        forwardService,
                        new PluginGatewayExceptionResolver()
                ))
                .build();
    }

    @Test
    void shouldForwardStandalonePluginStaticRequest() throws Exception {
        pluginRegistry.register(plugin("dict", PluginSourceType.STANDALONE_NODE, PluginRuntimeStatus.STARTED));
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(2);
            response.setStatus(200);
            response.getWriter().write("forwarded-static:dict");
            return null;
        }).when(forwardService).forward(any(), any(), any());

        mockMvc.perform(get("/plugin-static/dict/assets/remoteEntry.js"))
                .andExpect(status().isOk())
                .andExpect(content().string("forwarded-static:dict"));

        verify(forwardService).forward(any(), any(), any());
    }

    @Test
    void shouldLetEmbeddedPluginReachLocalHandler() throws Exception {
        pluginRegistry.register(plugin("dict", PluginSourceType.EMBEDDED_SYSTEM, PluginRuntimeStatus.STARTED));

        mockMvc.perform(get("/plugin-static/dict/assets/remoteEntry.js"))
                .andExpect(status().isOk())
                .andExpect(content().string("local-static:dict"));

        verify(forwardService, never()).forward(any(), any(), any());
    }

    @Test
    void shouldReturnNotFoundWhenPluginMissing() throws Exception {
        mockMvc.perform(get("/plugin-static/dict/assets/remoteEntry.js"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"success":false,"code":"PYIN-PLUGIN-404","message":"插件不存在：dict"}
                        """));
    }

    @Test
    void shouldReturnUnavailableWhenPluginStopped() throws Exception {
        pluginRegistry.register(plugin("dict", PluginSourceType.STANDALONE_NODE, PluginRuntimeStatus.STOPPED));

        mockMvc.perform(get("/plugin-static/dict/assets/remoteEntry.js"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"success":false,"code":"PYIN-PLUGIN-503","message":"插件已停用，暂时无法访问：dict"}
                        """));
    }

    private RegisteredPlugin plugin(String pluginId, PluginSourceType sourceType, PluginRuntimeStatus status) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId(pluginId);
        return new RegisteredPlugin(
                pluginId,
                sourceType.name().toLowerCase() + ":" + pluginId,
                descriptor,
                null,
                sourceType,
                status,
                null,
                "http://127.0.0.1:18080/plugin-static/" + pluginId,
                null,
                Instant.now()
        );
    }

    @RestController
    static class TestStaticController {

        @RequestMapping("/plugin-static/{pluginId}/assets/**")
        ResponseEntity<String> asset(@PathVariable String pluginId) {
            return ResponseEntity.ok("local-static:" + pluginId);
        }
    }
}
