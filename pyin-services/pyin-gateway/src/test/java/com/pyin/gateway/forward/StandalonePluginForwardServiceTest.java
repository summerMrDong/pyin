package com.pyin.gateway.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.pyin.gateway.signature.GatewaySignatureService;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    void shouldForwardRequestWithHeadersBodyAndQueryString() throws Exception {
        server.expect(once(), requestTo("http://127.0.0.1:18080/dict/open/batch?page=1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("page", "1"))
                .andExpect(content().string("{\"key\":\"value\"}"))
                .andExpect(header("X-Pyin-Plugin-Id", "dict"))
                .andExpect(header("X-Pyin-Request-Source", "CLIENT_SDK_GATEWAY"))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":true}"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/plugins/dict/open/batch");
        request.setContextPath("/plugins");
        request.setQueryString("page=1");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        service.forward(plugin("dict"), request, response);

        assertEquals(202, response.getStatus());
        assertEquals("{\"ok\":true}", response.getContentAsString());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Content-Type"));
        server.verify();
    }

    private RegisteredPlugin plugin(String pluginId) {
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        descriptor.setPluginId(pluginId);
        return new RegisteredPlugin(
                pluginId,
                "standalone:" + pluginId,
                descriptor,
                null,
                PluginSourceType.STANDALONE_NODE,
                PluginRuntimeStatus.STARTED,
                null,
                "http://127.0.0.1:18080",
                null,
                Instant.now()
        );
    }
}
