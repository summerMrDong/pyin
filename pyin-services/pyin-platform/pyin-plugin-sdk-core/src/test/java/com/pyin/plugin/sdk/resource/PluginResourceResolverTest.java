package com.pyin.plugin.sdk.resource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PluginResourceResolverTest {

    @Test
    void shouldResolveRemoteEntryFromClasspathByDefault() {
        PluginResourceProperties properties = new PluginResourceProperties();
        DefaultPluginResourceResolver resolver = new DefaultPluginResourceResolver(properties);

        assertDoesNotThrow(() -> resolver.resolveRemoteEntry("demo"));
    }

    @Test
    void shouldPreferConfiguredFileLocation() throws Exception {
        Path root = Files.createTempDirectory("plugin-resource-resolver");
        Path pluginDir = root.resolve("demo");
        Files.createDirectories(pluginDir);
        Files.writeString(pluginDir.resolve("remoteEntry.js"), "export default {};");

        PluginResourceProperties properties = new PluginResourceProperties();
        properties.setLocations(java.util.List.of(
                "file:" + root + "/{pluginId}/",
                "classpath:/plugin-static/{pluginId}/"
        ));
        DefaultPluginResourceResolver resolver = new DefaultPluginResourceResolver(properties);

        assertDoesNotThrow(() -> resolver.resolveRemoteEntry("demo"));
    }

    @Test
    void shouldFailWhenResourceMissingEverywhere() {
        PluginResourceProperties properties = new PluginResourceProperties();
        DefaultPluginResourceResolver resolver = new DefaultPluginResourceResolver(properties);

        assertThrows(IllegalStateException.class, () -> resolver.resolveRemoteEntry("missing"));
    }

    @Test
    void shouldUsePluginSpecificOverrideLocations() throws Exception {
        Path root = Files.createTempDirectory("plugin-resource-override");
        Path pluginDir = root.resolve("dict");
        Files.createDirectories(pluginDir);
        Files.writeString(pluginDir.resolve("plugin-entry.js"), "export default {};");

        PluginResourceProperties properties = new PluginResourceProperties();
        PluginResourceProperties.PluginResourceOverride override = new PluginResourceProperties.PluginResourceOverride();
        override.setLocations(java.util.List.of("file:" + root + "/{pluginId}/"));
        override.setRemoteEntryFile("plugin-entry.js");
        properties.getPlugins().put("dict", override);
        DefaultPluginResourceResolver resolver = new DefaultPluginResourceResolver(properties);

        assertDoesNotThrow(() -> resolver.resolve("dict", properties.getEffectiveRemoteEntryFile("dict")));
    }
}
