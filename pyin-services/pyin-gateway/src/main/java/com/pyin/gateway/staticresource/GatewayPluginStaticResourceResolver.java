package com.pyin.gateway.staticresource;

import com.pyin.plugin.runtime.loader.PluginRuntimeProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

@Component
public class GatewayPluginStaticResourceResolver extends AbstractResourceResolver {

    private final PluginRuntimeProperties properties;
    private final GatewayPluginStaticResourceLocator resourceLocator;

    public GatewayPluginStaticResourceResolver(
            PluginRuntimeProperties properties,
            GatewayPluginStaticResourceLocator resourceLocator
    ) {
        this.properties = properties;
        this.resourceLocator = resourceLocator;
    }

    @Override
    protected Resource resolveResourceInternal(
            HttpServletRequest request,
            String requestPath,
            List<? extends Resource> locations,
            ResourceResolverChain chain
    ) {
        String pluginId = extractPluginId(requestPath);
        String relativePath = extractRelativePath(requestPath);
        if (pluginId == null || relativePath == null) {
            return null;
        }
        try {
            return resourceLocator.resolve(
                    pluginId,
                    relativePath,
                    Path.of(properties.getSystemPluginsDir()),
                    Path.of(properties.getExternalPluginsDir())
            );
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    @Override
    protected String resolveUrlPathInternal(
            String resourcePath,
            List<? extends Resource> locations,
            ResourceResolverChain chain
    ) {
        return resolveResourceInternal(null, resourcePath, locations, chain) == null ? null : resourcePath;
    }

    private String extractPluginId(String requestPath) {
        int separator = requestPath.indexOf('/');
        if (separator <= 0) {
            return null;
        }
        return requestPath.substring(0, separator);
    }

    private String extractRelativePath(String requestPath) {
        int separator = requestPath.indexOf('/');
        if (separator <= 0 || separator + 1 >= requestPath.length()) {
            return null;
        }
        String relativePath = requestPath.substring(separator + 1);
        return relativePath.startsWith("assets/") ? relativePath : null;
    }
}
