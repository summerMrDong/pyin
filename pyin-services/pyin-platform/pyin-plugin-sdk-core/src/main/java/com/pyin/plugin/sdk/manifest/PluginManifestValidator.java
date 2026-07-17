package com.pyin.plugin.sdk.manifest;

import com.pyin.plugin.spi.model.PluginManifest;
import org.springframework.util.StringUtils;

public final class PluginManifestValidator {

    private PluginManifestValidator() {
    }

    public static void validate(PluginManifest manifest) {
        if (manifest == null) {
            throw new IllegalArgumentException("Plugin manifest must not be null");
        }
        if (!StringUtils.hasText(manifest.getPluginId())) {
            throw new IllegalArgumentException("Plugin manifest pluginId must not be blank");
        }
        if (!StringUtils.hasText(manifest.getPluginName())) {
            throw new IllegalArgumentException("Plugin manifest pluginName must not be blank");
        }
        if (!StringUtils.hasText(manifest.getPluginVersion())) {
            throw new IllegalArgumentException("Plugin manifest pluginVersion must not be blank");
        }
    }
}
