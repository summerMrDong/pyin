package com.pyin.plugin.sdk.web;

import com.pyin.plugin.spi.PyinPlugin;
import java.util.Comparator;
import java.util.List;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class PluginOwnershipResolver {

    private final List<PyinPlugin> plugins;

    public PluginOwnershipResolver(List<PyinPlugin> plugins) {
        this.plugins = plugins;
    }

    public String resolvePluginId(Class<?> controllerClass) {
        return resolveOwner(controllerClass).pluginId();
    }

    public boolean belongsToPlugin(Class<?> controllerClass, PyinPlugin plugin) {
        return resolvePluginId(controllerClass).equals(plugin.manifest().getPluginId());
    }

    private PluginOwner resolveOwner(Class<?> controllerClass) {
        String controllerPackage = ClassUtils.getPackageName(controllerClass);
        List<PluginOwner> matches = plugins.stream()
                .map(plugin -> new PluginOwner(
                        plugin.manifest().getPluginId(),
                        ClassUtils.getPackageName(AopUtils.getTargetClass(plugin))
                ))
                .filter(owner -> controllerPackage.startsWith(owner.packagePrefix()))
                .sorted(Comparator.comparingInt((PluginOwner owner) -> owner.packagePrefix().length()).reversed())
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalStateException("No plugin ownership found for controller: " + controllerClass.getName());
        }
        if (matches.size() > 1
                && matches.get(0).packagePrefix().length() == matches.get(1).packagePrefix().length()
                && !matches.get(0).pluginId().equals(matches.get(1).pluginId())) {
            throw new IllegalStateException("Ambiguous plugin ownership for controller: " + controllerClass.getName());
        }
        return matches.get(0);
    }

    private record PluginOwner(String pluginId, String packagePrefix) {
    }
}
