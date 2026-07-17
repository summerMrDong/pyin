package com.pyin.gateway.staticresource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pyin.plugin.runtime.loader.PluginRuntimeProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceResolverChain;

class GatewayPluginStaticResourceResolverTest {

    private GatewayPluginStaticResourceResolver resolver;

    @BeforeEach
    void setUp() throws Exception {
        Path runtimeRoot = Files.createTempDirectory("gateway-static-resolver-test");
        Path systemPluginsDir = runtimeRoot.resolve("system");
        Path externalPluginsDir = runtimeRoot.resolve("external");
        Path assetPath = systemPluginsDir.resolve("config").resolve("web").resolve("assets").resolve("remoteEntry.js");
        Files.createDirectories(assetPath.getParent());
        Files.writeString(assetPath, "config remote entry");

        PluginRuntimeProperties properties = new PluginRuntimeProperties();
        properties.setSystemPluginsDir(systemPluginsDir.toString());
        properties.setExternalPluginsDir(externalPluginsDir.toString());
        resolver = new GatewayPluginStaticResourceResolver(properties, new GatewayPluginStaticResourceLocator());
    }

    @Test
    void shouldResolveEmbeddedPluginStaticAsset() throws Exception {
        Resource resource = resolver.resolveResource(
                null,
                "config/assets/remoteEntry.js",
                List.of(),
                Mockito.mock(ResourceResolverChain.class)
        );

        assertNotNull(resource);
        assertEquals("config remote entry", resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void shouldReturnNullForMissingStaticAsset() {
        Resource resource = resolver.resolveResource(
                null,
                "config/assets/not-found.js",
                List.of(),
                Mockito.mock(ResourceResolverChain.class)
        );

        assertNull(resource);
    }
}
