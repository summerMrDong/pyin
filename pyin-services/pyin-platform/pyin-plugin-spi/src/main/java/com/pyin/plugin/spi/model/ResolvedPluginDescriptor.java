package com.pyin.plugin.spi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件运行时装配后的最终描述对象。
 */
public class ResolvedPluginDescriptor {

    private String pluginId;
    private String pluginName;
    private String pluginVersion;
    private String basePath;
    private String entryJs;
    private List<PluginPermission> permissions = new ArrayList<>();
    private List<PluginApiDefinition> apis = new ArrayList<>();
    private List<PluginResourceDefinition> resources = new ArrayList<>();

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getEntryJs() {
        return entryJs;
    }

    public void setEntryJs(String entryJs) {
        this.entryJs = entryJs;
    }

    public List<PluginPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PluginPermission> permissions) {
        this.permissions = permissions;
    }

    public List<PluginApiDefinition> getApis() {
        return apis;
    }

    public void setApis(List<PluginApiDefinition> apis) {
        this.apis = apis;
    }

    public List<PluginResourceDefinition> getResources() {
        return resources;
    }

    public void setResources(List<PluginResourceDefinition> resources) {
        this.resources = resources;
    }
}
