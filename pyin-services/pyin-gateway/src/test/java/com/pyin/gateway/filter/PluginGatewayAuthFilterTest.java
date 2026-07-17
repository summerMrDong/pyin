package com.pyin.gateway.filter;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pyin.center.auth.gateway.GatewayAuthenticationException;
import com.pyin.center.auth.gateway.GatewayAdminAuthService;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class PluginGatewayAuthFilterTest {

    private MockMvc mockMvc;
    private GatewayAdminAuthService gatewayAdminAuthService;

    @BeforeEach
    void setUp() {
        gatewayAdminAuthService = Mockito.mock(GatewayAdminAuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestGatewayController())
                .addFilters(new PluginGatewayAuthFilter(gatewayAdminAuthService, new PluginGatewayExceptionResolver()))
                .build();
    }

    @Test
    void shouldRequireLoginForAdminRequest() throws Exception {
        doThrow(new GatewayAuthenticationException("未登录或登录已过期")).when(gatewayAdminAuthService).checkAdminRequest();

        mockMvc.perform(get("/plugins/dict/admin/list").contextPath("/plugins"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"success":false,"code":"PYIN-AUTH-401","message":"未登录或登录已过期"}
                        """));
    }

    @Test
    void shouldAllowLoggedInAdminRequest() throws Exception {
        mockMvc.perform(get("/plugins/dict/admin/list").contextPath("/plugins"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok:dict"));

        verify(gatewayAdminAuthService).checkAdminRequest();
    }

    @Test
    void shouldBypassLoginForOpenRequest() throws Exception {
        mockMvc.perform(get("/plugins/dict/open/query").contextPath("/plugins"))
                .andExpect(status().isOk())
                .andExpect(content().string("open:dict"));

        verifyNoInteractions(gatewayAdminAuthService);
    }

    @RestController
    static class TestGatewayController {

        @RequestMapping("/{pluginId}/admin/**")
        ResponseEntity<String> admin(@PathVariable String pluginId) {
            return ResponseEntity.ok("ok:" + pluginId);
        }

        @RequestMapping("/{pluginId}/open/**")
        ResponseEntity<String> open(@PathVariable String pluginId) {
            return ResponseEntity.ok("open:" + pluginId);
        }
    }
}
