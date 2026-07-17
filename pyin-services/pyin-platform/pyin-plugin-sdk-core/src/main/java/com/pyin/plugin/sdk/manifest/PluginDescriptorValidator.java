package com.pyin.plugin.sdk.manifest;

import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginPermission;
import com.pyin.plugin.spi.model.PluginResourceDefinition;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class PluginDescriptorValidator {

    private PluginDescriptorValidator() {
    }

    public static void validate(ResolvedPluginDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("Resolved plugin descriptor must not be null");
        }
        if (!StringUtils.hasText(descriptor.getPluginId())) {
            throw new IllegalArgumentException("Resolved plugin descriptor pluginId must not be blank");
        }
        if (!StringUtils.hasText(descriptor.getPluginName())) {
            throw new IllegalArgumentException("Resolved plugin descriptor pluginName must not be blank");
        }
        if (!StringUtils.hasText(descriptor.getPluginVersion())) {
            throw new IllegalArgumentException("Resolved plugin descriptor pluginVersion must not be blank");
        }
        ensureUniquePermissionCodes(descriptor);
        ensureUniqueResourceCodes(descriptor);
    }

    private static void ensureUniquePermissionCodes(ResolvedPluginDescriptor descriptor) {
        Set<String> seen = new HashSet<>();
        for (PluginPermission permission : descriptor.getPermissions()) {
            if (!seen.add(permission.code())) {
                throw new IllegalStateException("Duplicate permission code detected: " + permission.code());
            }
        }
    }

    private static void ensureUniqueResourceCodes(ResolvedPluginDescriptor descriptor) {
        Set<String> seen = new HashSet<>();
        for (PluginResourceDefinition resource : descriptor.getResources()) {
            if (!StringUtils.hasText(resource.resourceCode())) {
                throw new IllegalStateException("Plugin resource code must not be blank");
            }
            if (!seen.add(resource.resourceCode())) {
                throw new IllegalStateException("Duplicate resource code detected: " + resource.resourceCode());
            }
        }
        for (PluginApiDefinition api : descriptor.getApis()) {
            if (!StringUtils.hasText(api.internalPath())) {
                throw new IllegalStateException("Plugin api internalPath must not be blank: " + api.path());
            }
        }
    }
}
