package com.pyin.gateway.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class PluginAvailabilityFilterTest {

    private MockMvc mockMvc;
    private PluginRegistry pluginRegistry;

    @BeforeEach
    void setUp() {
        pluginRegistry = new PluginRegistry();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestGatewayController())
                .addFilters(new PluginAvailabilityFilter(pluginRegistry, new PluginGatewayExceptionResolver()))
                .build();
    }

    @Test
    void shouldAllowStartedPluginAdminRequest() throws Exception {
        pluginRegistry.register(plugin("dict", PluginRuntimeStatus.STARTED));

        mockMvc.perform(get("/plugins/dict/admin/list").contextPath("/plugins"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok:dict"));
    }

    @Test
    void shouldReturnNotFoundWhenPluginMissing() throws Exception {
        mockMvc.perform(get("/plugins/dict/admin/list").contextPath("/plugins"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"success":false,"code":"PYIN-PLUGIN-404","message":"插件不存在：dict"}
                        """));
    }

    @Test
    void shouldReturnStoppedMessageWhenPluginStopped() throws Exception {
        pluginRegistry.register(plugin("dict", PluginRuntimeStatus.STOPPED));

        mockMvc.perform(get("/plugins/dict/open/query").contextPath("/plugins"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"success":false,"code":"PYIN-PLUGIN-503","message":"插件已停用，暂时无法访问：dict"}
                        """));
    }

    @Test
    void shouldReturnUnavailableMessageWhenPluginUnavailable() throws Exception {
        pluginRegistry.register(plugin("dict", PluginRuntimeStatus.UNAVAILABLE));

        mockMvc.perform(get("/plugins/dict/open/query").contextPath("/plugins"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"success":false,"code":"PYIN-PLUGIN-503","message":"插件当前不可用，请稍后重试：dict"}
                        """));
    }

    private RegisteredPlugin plugin(String pluginId, PluginRuntimeStatus status) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId(pluginId);
        return new RegisteredPlugin(
                pluginId,
                "embedded:" + pluginId,
                descriptor,
                null,
                PluginSourceType.EMBEDDED_SYSTEM,
                status,
                null,
                null,
                null,
                Instant.now()
        );
    }

    @RestController
    static class TestGatewayController {

        @RequestMapping("/{pluginId}/admin/**")
        ResponseEntity<String> admin(@PathVariable String pluginId) {
            return ResponseEntity.ok("ok:" + pluginId);
        }

        @RequestMapping("/{pluginId}/open/**")
        ResponseEntity<String> open(@PathVariable String pluginId) {
            return ResponseEntity.ok("ok:" + pluginId);
        }
    }
}
