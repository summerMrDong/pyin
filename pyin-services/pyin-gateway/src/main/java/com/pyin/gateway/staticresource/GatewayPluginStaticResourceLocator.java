package com.pyin.gateway.staticresource;

import com.pyin.plugin.sdk.resource.DefaultPluginResourceResolver;
import com.pyin.plugin.sdk.resource.PluginResourceProperties;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class GatewayPluginStaticResourceLocator {

    private final DefaultPluginResourceResolver pluginResourceResolver;

    public GatewayPluginStaticResourceLocator() {
        this.pluginResourceResolver = new DefaultPluginResourceResolver(
                new PluginResourceProperties(),
                new DefaultResourceLoader()
        );
    }

    public Resource resolve(
            String pluginId,
            String relativePath,
            Path systemPluginsDir,
            Path externalPluginsDir
    ) {
        List<String> locations = new ArrayList<>();
        locations.add("classpath:/plugin-static/{pluginId}/");
        locations.add("file:" + systemPluginsDir.resolve("{pluginId}").resolve("web") + "/");
        locations.add("file:" + systemPluginsDir.resolve("{pluginId}-plugin").resolve("web") + "/");
        locations.add("file:" + externalPluginsDir.resolve("{pluginId}").resolve("web") + "/");
        locations.add("file:" + externalPluginsDir.resolve("{pluginId}-plugin").resolve("web") + "/");
        return pluginResourceResolver.resolve(pluginId, relativePath, locations).resource();
    }
}
