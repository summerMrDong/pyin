package com.pyin.gateway.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PluginGatewayPathSupportTest {

    @Test
    void shouldDetectPluginGatewayRequest() {
        assertTrue(PluginGatewayPathSupport.isPluginGatewayRequest("/plugins/dict/admin/list"));
        assertTrue(PluginGatewayPathSupport.isPluginGatewayRequest("/plugins/dict/open/query"));
        assertFalse(PluginGatewayPathSupport.isPluginGatewayRequest("/dict/admin/list"));
        assertFalse(PluginGatewayPathSupport.isPluginGatewayRequest("/api/plugins/dict/admin/list"));
        assertFalse(PluginGatewayPathSupport.isPluginGatewayRequest("/legacy/dict/client/query"));
        assertFalse(PluginGatewayPathSupport.isPluginGatewayRequest("/plugin-static/dict/remoteEntry.js"));
    }

    @Test
    void shouldExtractPluginIdAndAdminFlag() {
        assertEquals("dict", PluginGatewayPathSupport.extractPluginId("/plugins/dict/admin/list"));
        assertTrue(PluginGatewayPathSupport.isAdminRequest("/plugins/dict/admin/list"));
        assertFalse(PluginGatewayPathSupport.isAdminRequest("/plugins/dict/open/query"));
    }

    @Test
    void shouldExtractAdminRelativePath() {
        assertEquals("/list", PluginGatewayPathSupport.extractPluginRelativePath("/plugins/dict/admin/list", "dict"));
    }

    @Test
    void shouldExtractOpenRelativePath() {
        assertEquals("/query", PluginGatewayPathSupport.extractPluginRelativePath("/plugins/dict/open/query", "dict"));
    }

    @Test
    void shouldReturnRootForGatewayRoot() {
        assertEquals("/", PluginGatewayPathSupport.extractPluginRelativePath("/plugins/dict/open", "dict"));
    }

    @Test
    void shouldBuildPluginControllerPath() {
        assertEquals(
                "/plugins/dict/admin/list",
                PluginGatewayPathSupport.toPluginControllerPath(PluginGatewayPathSupport.parse("/plugins/dict/admin/list"))
        );
    }
}
