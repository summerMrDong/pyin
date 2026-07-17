package com.pyin.gateway.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.forward.StandalonePluginForwardService;
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

class StandalonePluginForwardFilterTest {

    private MockMvc mockMvc;
    private PluginRegistry pluginRegistry;
    private StandalonePluginForwardService standalonePluginForwardService;

    @BeforeEach
    void setUp() {
        pluginRegistry = new PluginRegistry();
        standalonePluginForwardService = Mockito.mock(StandalonePluginForwardService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestGatewayController())
                .addFilters(new StandalonePluginForwardFilter(
                        pluginRegistry,
                        standalonePluginForwardService,
                        new PluginGatewayExceptionResolver()
                ))
                .build();
    }

    @Test
    void shouldForwardStandalonePluginRequest() throws Exception {
        pluginRegistry.register(plugin("dict", PluginSourceType.STANDALONE_NODE));
        Mockito.doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(2);
            response.setStatus(200);
            response.getWriter().write("forwarded:dict");
            return null;
        }).when(standalonePluginForwardService).forward(any(), any(), any());

        mockMvc.perform(get("/plugins/dict/admin/list").contextPath("/plugins"))
                .andExpect(status().isOk())
                .andExpect(content().string("forwarded:dict"));

        verify(standalonePluginForwardService).forward(any(), any(), any());
    }

    @Test
    void shouldLetEmbeddedPluginReachLocalController() throws Exception {
        pluginRegistry.register(plugin("dict", PluginSourceType.EMBEDDED_SYSTEM));

        mockMvc.perform(get("/plugins/dict/admin/list").contextPath("/plugins"))
                .andExpect(status().isOk())
                .andExpect(content().string("local:dict"));

        verify(standalonePluginForwardService, never()).forward(any(), any(), any());
    }

    private RegisteredPlugin plugin(String pluginId, PluginSourceType sourceType) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId(pluginId);
        return new RegisteredPlugin(
                pluginId,
                sourceType.name().toLowerCase() + ":" + pluginId,
                descriptor,
                null,
                sourceType,
                PluginRuntimeStatus.STARTED,
                null,
                "http://127.0.0.1:18080",
                null,
                Instant.now()
        );
    }

    @RestController
    static class TestGatewayController {

        @RequestMapping("/{pluginId}/admin/**")
        ResponseEntity<String> admin(@PathVariable String pluginId) {
            return ResponseEntity.ok("local:" + pluginId);
        }
    }
}
