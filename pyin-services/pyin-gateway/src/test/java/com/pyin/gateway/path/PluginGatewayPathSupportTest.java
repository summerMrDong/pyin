package com.pyin.gateway.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PluginGatewayPathSupportTest {

    @Test
    void shouldDetectPluginGatewayRequest() {
        assertTrue(PluginGatewayPathSupport.isPluginGatewayRequest("/dict/admin/list"));
        assertTrue(PluginGatewayPathSupport.isPluginGatewayRequest("/dict/open/query"));
        assertFalse(PluginGatewayPathSupport.isPluginGatewayRequest("/plugin-static/dict/remoteEntry.js"));
    }

    @Test
    void shouldExtractPluginIdAndAdminFlag() {
        assertEquals("dict", PluginGatewayPathSupport.extractPluginId("/dict/admin/list"));
        assertTrue(PluginGatewayPathSupport.isAdminRequest("/dict/admin/list"));
        assertFalse(PluginGatewayPathSupport.isAdminRequest("/dict/open/query"));
    }

    @Test
    void shouldExtractAdminRelativePath() {
        assertEquals("/list", PluginGatewayPathSupport.extractPluginRelativePath("/dict/admin/list", "dict"));
    }

    @Test
    void shouldExtractOpenRelativePath() {
        assertEquals("/query", PluginGatewayPathSupport.extractPluginRelativePath("/dict/open/query", "dict"));
    }

    @Test
    void shouldReturnRootForGatewayRoot() {
        assertEquals("/", PluginGatewayPathSupport.extractPluginRelativePath("/dict/open", "dict"));
    }
}
