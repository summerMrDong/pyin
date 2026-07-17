package com.pyin.plugin.sdk.standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class StandalonePluginPropertiesTest {

    @Test
    void shouldResolveUrlsFromAdvertiseBaseUrl() {
        StandalonePluginProperties properties = new StandalonePluginProperties();
        properties.setPluginId("file");
        properties.setAdvertiseBaseUrl("http://127.0.0.1:19110/");

        assertEquals("http://127.0.0.1:19110", properties.getResolvedBackendBaseUrl());
        assertEquals("http://127.0.0.1:19110/plugin-static/file", properties.getResolvedFrontendBaseUrl());
        assertEquals("http://127.0.0.1:19110/health", properties.getResolvedHealthUrl());
    }

    @Test
    void shouldFallbackToServerAddressPortAndContextPath() {
        StandalonePluginProperties properties = new StandalonePluginProperties();
        properties.setPluginId("file");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.address", "10.0.0.8")
                .withProperty("server.port", "19110")
                .withProperty("server.servlet.context-path", "/plugin-app");
        properties.setEnvironment(environment);

        assertEquals("http://10.0.0.8:19110/plugin-app", properties.getResolvedBackendBaseUrl());
        assertEquals("http://10.0.0.8:19110/plugin-app/plugin-static/file", properties.getResolvedFrontendBaseUrl());
        assertEquals("http://10.0.0.8:19110/plugin-app/health", properties.getResolvedHealthUrl());
    }
}
