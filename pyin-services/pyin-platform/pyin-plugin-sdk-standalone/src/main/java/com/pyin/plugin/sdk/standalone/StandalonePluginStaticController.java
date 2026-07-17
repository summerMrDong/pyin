package com.pyin.plugin.sdk.standalone;

import com.pyin.plugin.sdk.resource.PluginResourceProperties;
import com.pyin.plugin.sdk.resource.PluginResourceResolver;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plugin-static/{pluginId}")
@ConditionalOnBean(StandalonePluginProperties.class)
@ConditionalOnProperty(prefix = "pyin.plugin", name = "plugin-id")
public class StandalonePluginStaticController {

    private final StandalonePluginProperties standalonePluginProperties;
    private final PluginResourceResolver pluginResourceResolver;
    private final PluginResourceProperties pluginResourceProperties;

    public StandalonePluginStaticController(
            StandalonePluginProperties standalonePluginProperties,
            PluginResourceResolver pluginResourceResolver,
            PluginResourceProperties pluginResourceProperties
    ) {
        this.standalonePluginProperties = standalonePluginProperties;
        this.pluginResourceResolver = pluginResourceResolver;
        this.pluginResourceProperties = pluginResourceProperties;
    }

    @GetMapping("/assets/{assetName:.+}")
    public ResponseEntity<Resource> asset(@PathVariable String pluginId, @PathVariable String assetName) throws IOException {
        if (!isExpectedPlugin(pluginId)) {
            return ResponseEntity.notFound().build();
        }
        return serve(pluginId, normalizeAssetPath(pluginId, assetName));
    }

    private ResponseEntity<Resource> serve(String pluginId, String relativePath) throws IOException {
        try {
            Resource resource = pluginResourceResolver.resolve(pluginId, relativePath).resource();
            return withMediaType(resource);
        } catch (IllegalStateException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private String normalizeAssetPath(String pluginId, String relativePath) {
        String assetsDir = pluginResourceProperties.getEffectiveAssetsDir(pluginId);
        String normalizedAssetsDir = assetsDir.endsWith("/") ? assetsDir : assetsDir + "/";
        if (relativePath.startsWith(normalizedAssetsDir)) {
            return relativePath;
        }
        return normalizedAssetsDir + relativePath;
    }

    private ResponseEntity<Resource> withMediaType(Resource resource) throws IOException {
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    private boolean isExpectedPlugin(String pluginId) {
        return StringUtils.hasText(pluginId)
                && pluginId.equals(standalonePluginProperties.getPluginId());
    }
}
