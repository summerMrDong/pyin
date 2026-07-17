package com.pyin.gateway.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PluginStaticResourcePathSupportTest {

    @Test
    void shouldDetectPluginStaticAssetRequest() {
        assertTrue(PluginStaticResourcePathSupport.isPluginStaticAssetRequest("/plugin-static/dict/assets/remoteEntry.js"));
        assertFalse(PluginStaticResourcePathSupport.isPluginStaticAssetRequest("/plugin-static/dict/remoteEntry.js"));
        assertFalse(PluginStaticResourcePathSupport.isPluginStaticAssetRequest("/dict/open/query"));
    }

    @Test
    void shouldExtractPluginIdAndRelativeAssetPath() {
        String requestPath = "/plugin-static/dict/assets/remoteEntry.js";
        assertEquals("dict", PluginStaticResourcePathSupport.extractPluginId(requestPath));
        assertEquals("assets/remoteEntry.js", PluginStaticResourcePathSupport.extractRelativeAssetPath(requestPath));
    }
}
