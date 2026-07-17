package com.pyin.plugin.sdk.manifest;

import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 组装最终插件描述，自动配置优先生成，手动清单负责覆盖。
 */
@Component
public class PluginDescriptorAssembler {

    private final PluginApiScanner pluginApiScanner;
    private final PluginResourceAssembler pluginResourceAssembler;

    public PluginDescriptorAssembler(
            PluginApiScanner pluginApiScanner,
            PluginResourceAssembler pluginResourceAssembler
    ) {
        this.pluginApiScanner = pluginApiScanner;
        this.pluginResourceAssembler = pluginResourceAssembler;
    }

    public ResolvedPluginDescriptor assemble(ApplicationContext applicationContext, PyinPlugin plugin) {
        PluginManifest manifest = plugin.manifest();
        PluginManifestValidator.validate(manifest);
        PluginScanResult scanResult = pluginApiScanner.scan(applicationContext, plugin);
        applyDefaults(manifest, plugin.pluginId());
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        copyScalarFields(manifest, descriptor);
        descriptor.setMenus(List.copyOf(plugin.menus()));
        descriptor.setApis(scanResult.apis());
        descriptor.setPermissions(scanResult.permissions());
        descriptor.setResources(pluginResourceAssembler.assemble(manifest, scanResult));
        applyOverrides(manifest, descriptor);
        PluginDescriptorValidator.validate(descriptor);
        return descriptor;
    }

    public ResolvedPluginDescriptor assembleFromManifest(PluginManifest manifest) {
        PluginManifestValidator.validate(manifest);
        applyDefaults(manifest, manifest.getPluginId());
        ResolvedPluginDescriptor descriptor = new ResolvedPluginDescriptor();
        copyScalarFields(manifest, descriptor);
        applyOverrides(manifest, descriptor);
        PluginDescriptorValidator.validate(descriptor);
        return descriptor;
    }

    private void applyDefaults(PluginManifest manifest, String pluginId) {
        if (!StringUtils.hasText(manifest.getEntryJs())) {
            manifest.setEntryJs("/plugin-static/" + pluginId + "/assets/remoteEntry.js");
        }
        if (!StringUtils.hasText(manifest.getRemoteName())) {
            manifest.setRemoteName(pluginId);
        }
        if (manifest.getExposedModules().isEmpty()) {
            manifest.setExposedModules(List.of("./" + toCamelCase(pluginId) + "RemoteApp"));
        } else {
            manifest.setExposedModules(manifest.getExposedModules().stream()
                    .map(m -> m.startsWith("./") ? m : "./" + m)
                    .toList());
        }
    }

    private void copyScalarFields(PluginManifest source, ResolvedPluginDescriptor target) {
        target.setPluginId(source.getPluginId());
        target.setPluginName(source.getPluginName());
        target.setPluginType(source.getPluginType());
        target.setRuntimeMode(source.getRuntimeMode());
        target.setPluginVersion(source.getPluginVersion());
        target.setBasePath(source.getBasePath());
        target.setEntryJs(source.getEntryJs());
        target.setRemoteName(source.getRemoteName());
        target.setExposedModules(List.copyOf(source.getExposedModules()));
    }

    private void applyOverrides(PluginManifest overrideManifest, ResolvedPluginDescriptor target) {
        if (hasManualCollection(overrideManifest.getPermissions())) {
            target.setPermissions(List.copyOf(overrideManifest.getPermissions()));
        }
        if (hasManualCollection(overrideManifest.getApis())) {
            target.setApis(List.copyOf(overrideManifest.getApis()));
        }
        if (hasManualCollection(overrideManifest.getResources())) {
            target.setResources(List.copyOf(overrideManifest.getResources()));
        }
        if (StringUtils.hasText(overrideManifest.getEntryJs())) {
            target.setEntryJs(overrideManifest.getEntryJs());
        }
        if (StringUtils.hasText(overrideManifest.getRemoteName())) {
            target.setRemoteName(overrideManifest.getRemoteName());
        }
        if (hasManualCollection(overrideManifest.getExposedModules())) {
            target.setExposedModules(List.copyOf(overrideManifest.getExposedModules()));
        }
        if (StringUtils.hasText(overrideManifest.getBasePath())) {
            target.setBasePath(overrideManifest.getBasePath());
        }
    }

    private boolean hasManualCollection(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private String toCamelCase(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '-' || c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }
}
