package com.pyin.gateway.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
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

class StandalonePluginStaticResourceForwardServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private StandalonePluginStaticResourceForwardService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new StandalonePluginStaticResourceForwardService(restTemplate);
    }

    @Test
    void shouldForwardStaticAssetRequest() throws Exception {
        server.expect(once(), requestTo("http://127.0.0.1:18080/plugin-static/dict/assets/remoteEntry.js?v=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("v", "1"))
                .andExpect(header("Accept", "*/*"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.valueOf("application/javascript"))
                        .body("export default {}"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/plugin-static/dict/assets/remoteEntry.js");
        request.setQueryString("v=1");
        request.addHeader("Accept", "*/*");

        MockHttpServletResponse response = new MockHttpServletResponse();
        service.forward(plugin("dict"), request, response);

        assertEquals(200, response.getStatus());
        assertEquals("export default {}", response.getContentAsString());
        assertEquals("application/javascript", response.getContentType());
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
                null,
                "http://127.0.0.1:18080/plugin-static/" + pluginId,
                Instant.now()
        );
    }
}
