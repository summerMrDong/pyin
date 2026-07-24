package com.pyin.gateway.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.gateway.path.PluginGatewayPathSupport;
import com.pyin.gateway.signature.GatewaySignatureService;
import com.pyin.plugin.runtime.route.PluginApiRoute;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.PluginAccessMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class StandalonePluginForwardServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private StandalonePluginForwardService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new StandalonePluginForwardService(new GatewaySignatureService(), restTemplate);
    }

    @Test
    void shouldForwardUnifiedRouteWithPrincipalPermissionAndTargetHeaders() throws Exception {
        server.expect(once(), requestTo("http://127.0.0.1:18080/plugins/dict/admin/list?page=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Pyin-Plugin-Id", "dict"))
                .andExpect(header("X-Pyin-Request-Source", "ADMIN_GATEWAY"))
                .andExpect(header("X-Pyin-Principal-Type", "ADMIN_USER"))
                .andExpect(header("X-Pyin-Principal-Id", "1"))
                .andExpect(header("X-Pyin-Permission-Code", "dict:view"))
                .andExpect(header("X-Pyin-Forward-Method", "GET"))
                .andExpect(header("X-Pyin-Forward-Path", "/plugins/dict/admin/list"))
                .andRespond(withStatus(HttpStatus.OK).body("ok"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/plugins/dict/admin/list");
        request.setQueryString("page=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.forward(
                route("dict"),
                PluginGatewayPathSupport.parse("/plugins/dict/admin/list"),
                AuthenticatedPrincipal.adminUser(1L, "admin"),
                request,
                response
        );

        assertEquals(200, response.getStatus());
        assertEquals("ok", response.getContentAsString());
        server.verify();
    }

    private PluginApiRoute route(String pluginId) {
        return new PluginApiRoute(
                pluginId,
                PluginRuntimeStatus.STARTED,
                PluginSourceType.STANDALONE_NODE,
                PluginAccessMode.CENTER_ADMIN_ONLY,
                "/plugins/" + pluginId + "/admin/list",
                "dict:view",
                true,
                "http://127.0.0.1:18080",
                "/list"
        );
    }
}
