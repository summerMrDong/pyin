package com.pyin.plugin.runtime.loader;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyin.center.plugin-runtime")
public class PluginRuntimeProperties {

    private String sourcePluginsDir = "pyin-plugins";
    private String runtimeRoot = "pyin-distribution-parent/runtime/pyin-config-center-runtime";
    private String bundledPluginsDir = "pyin-distribution-parent/bundled-plugins";
    private String systemPluginsDir = "pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/system";
    private String externalPluginsDir = "pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/external";

    public String getSourcePluginsDir() {
        return sourcePluginsDir;
    }

    public void setSourcePluginsDir(String sourcePluginsDir) {
        this.sourcePluginsDir = sourcePluginsDir;
    }

    public String getRuntimeRoot() {
        return runtimeRoot;
    }

    public void setRuntimeRoot(String runtimeRoot) {
        this.runtimeRoot = runtimeRoot;
    }

    public String getBundledPluginsDir() {
        return bundledPluginsDir;
    }

    public void setBundledPluginsDir(String bundledPluginsDir) {
        this.bundledPluginsDir = bundledPluginsDir;
    }

    public String getSystemPluginsDir() {
        return systemPluginsDir;
    }

    public void setSystemPluginsDir(String systemPluginsDir) {
        this.systemPluginsDir = systemPluginsDir;
    }

    public String getExternalPluginsDir() {
        return externalPluginsDir;
    }

    public void setExternalPluginsDir(String externalPluginsDir) {
        this.externalPluginsDir = externalPluginsDir;
    }
}
