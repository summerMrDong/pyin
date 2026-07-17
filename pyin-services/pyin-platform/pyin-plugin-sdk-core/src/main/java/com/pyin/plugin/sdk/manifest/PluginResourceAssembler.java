package com.pyin.plugin.sdk.manifest;

import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.PluginResourceDefinition;
import com.pyin.plugin.spi.model.PluginResourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PluginResourceAssembler {

    public List<PluginResourceDefinition> assemble(PluginManifest manifest, PluginScanResult scanResult) {
        Map<String, PluginResourceDefinition> resources = new LinkedHashMap<>();
        String pluginId = manifest.getPluginId();

        for (PluginApiDefinition api : scanResult.apis()) {
            String code = pluginId + ":api:" + normalizeResourceCode(api.path()) + ":" + api.method().toUpperCase();
            resources.put(code, new PluginResourceDefinition(
                    code,
                    api.method().toUpperCase() + " " + api.path(),
                    PluginResourceType.API,
                    null,
                    api.path(),
                    null,
                    null,
                    api.permissionCode(),
                    false,
                    Map.of("internalPath", api.internalPath(), "auditEnabled", api.auditEnabled())
            ));
        }

        if (StringUtils.hasText(manifest.getEntryJs())) {
            String code = pluginId + ":static:entry";
            resources.put(code, new PluginResourceDefinition(
                    code,
                    manifest.getPluginName() + " remoteEntry",
                    PluginResourceType.STATIC_ENTRY,
                    null,
                    manifest.getEntryJs(),
                    null,
                    null,
                    null,
                    false,
                    Map.of("remoteName", manifest.getRemoteName(), "exposedModules", manifest.getExposedModules())
            ));
        }

        if (manifest.getResources() != null && !manifest.getResources().isEmpty()) {
            resources.clear();
            for (PluginResourceDefinition resource : manifest.getResources()) {
                resources.put(resource.resourceCode(), resource);
            }
        }
        return new ArrayList<>(resources.values());
    }

    private String normalizeResourceCode(String value) {
        if (!StringUtils.hasText(value)) {
            return "root";
        }
        return value.replace('/', '.').replace('{', '_').replace('}', '_').replace(':', '.');
    }
}
