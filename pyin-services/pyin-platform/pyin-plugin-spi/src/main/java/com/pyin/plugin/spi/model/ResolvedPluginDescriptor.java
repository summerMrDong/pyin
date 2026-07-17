package com.pyin.plugin.spi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件运行时装配后的最终描述对象。
 */
public class ResolvedPluginDescriptor {

    private String pluginId;
    private String pluginName;
    private PluginType pluginType = PluginType.EXTERNAL;
    private PluginRuntimeMode runtimeMode = PluginRuntimeMode.EMBEDDED;
    private String pluginVersion;
    private String basePath;
    private String entryJs;
    private String remoteName;
    private List<String> exposedModules = new ArrayList<>();
    private List<PluginMenu> menus = new ArrayList<>();
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

    public PluginType getPluginType() {
        return pluginType;
    }

    public void setPluginType(PluginType pluginType) {
        this.pluginType = pluginType;
    }

    public PluginRuntimeMode getRuntimeMode() {
        return runtimeMode;
    }

    public void setRuntimeMode(PluginRuntimeMode runtimeMode) {
        this.runtimeMode = runtimeMode;
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

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public List<String> getExposedModules() {
        return exposedModules;
    }

    public void setExposedModules(List<String> exposedModules) {
        this.exposedModules = exposedModules;
    }

    public List<PluginMenu> getMenus() {
        return menus;
    }

    public void setMenus(List<PluginMenu> menus) {
        this.menus = menus;
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
