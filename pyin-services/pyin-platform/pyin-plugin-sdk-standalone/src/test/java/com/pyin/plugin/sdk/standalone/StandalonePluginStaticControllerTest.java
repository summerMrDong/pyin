package com.pyin.plugin.sdk.standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pyin.plugin.sdk.resource.DefaultPluginResourceResolver;
import com.pyin.plugin.sdk.resource.PluginResourceProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;

class StandalonePluginStaticControllerTest {

    @Test
    void shouldServeRemoteEntryFromAssetsPath() throws Exception {
        StandalonePluginStaticController controller = new StandalonePluginStaticController(
                properties(),
                new DefaultPluginResourceResolver(resourceProperties()),
                resourceProperties()
        );

        ResponseEntity<Resource> response = controller.asset("file", "remoteEntry.js");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("remote-entry", readBody(response));
    }

    @Test
    void shouldServeAssetFromAssetsDirectory() throws Exception {
        StandalonePluginStaticController controller = new StandalonePluginStaticController(
                properties(),
                new DefaultPluginResourceResolver(resourceProperties()),
                resourceProperties()
        );

        ResponseEntity<Resource> response = controller.asset("file", "chunk.js");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("asset-chunk", readBody(response));
    }

    @Test
    void shouldRejectUnexpectedPluginId() throws Exception {
        StandalonePluginStaticController controller = new StandalonePluginStaticController(
                properties(),
                new DefaultPluginResourceResolver(resourceProperties()),
                resourceProperties()
        );

        ResponseEntity<Resource> response = controller.asset("other", "remoteEntry.js");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldPreferConfiguredFileLocation() throws Exception {
        Path root = Files.createTempDirectory("standalone-plugin-static");
        Path pluginDir = root.resolve("file");
        Path assetsDir = pluginDir.resolve("assets");
        Files.createDirectories(assetsDir);
        Files.writeString(assetsDir.resolve("remoteEntry.js"), "remote-entry-from-file");
        Files.writeString(assetsDir.resolve("chunk.js"), "asset-from-file");

        PluginResourceProperties resourceProperties = resourceProperties();
        resourceProperties.setLocations(java.util.List.of(
                "file:" + root + "/{pluginId}/",
                "classpath:/plugin-static/{pluginId}/"
        ));
        StandalonePluginStaticController controller = new StandalonePluginStaticController(
                properties(),
                new DefaultPluginResourceResolver(resourceProperties),
                resourceProperties
        );

        assertEquals("remote-entry-from-file", readBody(controller.asset("file", "remoteEntry.js")));
        assertEquals("asset-from-file", readBody(controller.asset("file", "chunk.js")));
    }

    private StandalonePluginProperties properties() {
        StandalonePluginProperties properties = new StandalonePluginProperties();
        properties.setPluginId("file");
        return properties;
    }

    private PluginResourceProperties resourceProperties() {
        return new PluginResourceProperties();
    }

    private String readBody(ResponseEntity<Resource> response) throws Exception {
        return StreamUtils.copyToString(response.getBody().getInputStream(), StandardCharsets.UTF_8).trim();
    }
}
