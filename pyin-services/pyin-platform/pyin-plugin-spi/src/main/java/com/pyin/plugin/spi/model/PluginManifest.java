package com.pyin.plugin.spi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件清单。
 *
 * <p>它是插件向中心声明自身能力的统一模型，用于描述插件的基础身份、运行模式、
 * 前端入口、后端接口、权限定义等元数据。</p>
 *
 * <p>在当前实现中，{@code PluginManifest} 只承担"插件作者声明入口"的角色：</p>
 *
 * <p>1. 对内嵌插件来说，中心启动时会先读取插件作者在 {@code manifest()} 中填写的基础信息，
 * 再结合后端注解扫描结果自动补齐接口、权限和资源信息，
 * 最终形成完整清单。</p>
 *
 * <p>2. 对独立插件来说，插件进程会先完成同样的本地装配，再把最终描述对象放入节点注册协议，
 * 供中心统一纳管与代理。</p>
 *
 * <p>3. 对插件开发者来说，它还是"手动覆盖入口"：
 * 当自动扫描能够满足需求时，只需要填写少量基础字段；
 * 当需要覆盖默认装配结果时，可以显式设置 {@code permissions}、
 * {@code apis} 等字段，运行时会以手动值为准。</p>
 *
 * <p>因此，{@code PluginManifest} 的意义不是要求插件作者手工维护一份完整的大清单，
 * 而是作为插件元数据的统一承载对象，让中心、SDK、前端构建和独立插件注册协议
 * 都围绕同一份声明协作。</p>
 */
public class PluginManifest {

    /** 插件唯一标识。 */
    private String pluginId;
    /** 插件显示名称。 */
    private String pluginName;
    /** 插件类型，例如系统插件或外部插件。 */
    private PluginType pluginType = PluginType.EXTERNAL;
    /** 插件运行模式，例如内嵌运行或独立进程运行。 */
    private PluginRuntimeMode runtimeMode = PluginRuntimeMode.EMBEDDED;
    /** 插件版本号。 */
    private String pluginVersion;
    /** 插件前端基础访问路径，例如 `/plugins/config`。 */
    private String basePath;
    /** 插件前端模块联邦入口地址。 */
    private String entryJs;
    /** 模块联邦远端名称。 */
    private String remoteName;
    /** 模块联邦暴露的应用模块名列表，支持多个模块。 */
    private List<String> exposedModules = new ArrayList<>();
    /** 独立插件后端服务基础地址。 */
    private String backendBaseUrl;
    /** 独立插件前端服务基础地址。 */
    private String frontendBaseUrl;
    /** 独立插件健康检查地址。 */
    private String healthUrl;
    /** 插件权限定义，默认由后端注解扫描自动装配，也可手动覆盖。 */
    private List<PluginPermission> permissions = new ArrayList<>();
    /** 插件接口定义，默认由后端注解扫描自动装配，也可手动覆盖。 */
    private List<PluginApiDefinition> apis = new ArrayList<>();
    /** 插件资源定义，默认由装配器自动生成，也可手动覆盖。 */
    private List<PluginResourceDefinition> resources = new ArrayList<>();

    public static Builder builder() {
        return new Builder();
    }

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

    public String getBackendBaseUrl() {
        return backendBaseUrl;
    }

    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getHealthUrl() {
        return healthUrl;
    }

    public void setHealthUrl(String healthUrl) {
        this.healthUrl = healthUrl;
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

    /**
     * 清单构建器。
     *
     * <p>用于让插件作者以链式写法声明基础信息或手动覆盖项。</p>
     */
    public static class Builder {
        private final PluginManifest manifest = new PluginManifest();

        public Builder pluginId(String pluginId) {
            manifest.setPluginId(pluginId);
            return this;
        }

        public Builder pluginName(String pluginName) {
            manifest.setPluginName(pluginName);
            return this;
        }

        public Builder pluginType(PluginType pluginType) {
            manifest.setPluginType(pluginType);
            return this;
        }

        public Builder runtimeMode(PluginRuntimeMode runtimeMode) {
            manifest.setRuntimeMode(runtimeMode);
            return this;
        }

        public Builder pluginVersion(String pluginVersion) {
            manifest.setPluginVersion(pluginVersion);
            return this;
        }

        public Builder basePath(String basePath) {
            manifest.setBasePath(basePath);
            return this;
        }

        public Builder entryJs(String entryJs) {
            manifest.setEntryJs(entryJs);
            return this;
        }

        public Builder remoteName(String remoteName) {
            manifest.setRemoteName(remoteName);
            return this;
        }

        public Builder exposedModule(String exposedModule) {
            manifest.getExposedModules().add(exposedModule);
            return this;
        }

        public Builder exposedModules(List<String> exposedModules) {
            manifest.setExposedModules(new ArrayList<>(exposedModules));
            return this;
        }

        public Builder backendBaseUrl(String backendBaseUrl) {
            manifest.setBackendBaseUrl(backendBaseUrl);
            return this;
        }

        public Builder frontendBaseUrl(String frontendBaseUrl) {
            manifest.setFrontendBaseUrl(frontendBaseUrl);
            return this;
        }

        public Builder healthUrl(String healthUrl) {
            manifest.setHealthUrl(healthUrl);
            return this;
        }

        public Builder permissions(List<PluginPermission> permissions) {
            manifest.setPermissions(new ArrayList<>(permissions));
            return this;
        }

        public Builder apis(List<PluginApiDefinition> apis) {
            manifest.setApis(new ArrayList<>(apis));
            return this;
        }

        public Builder resources(List<PluginResourceDefinition> resources) {
            manifest.setResources(new ArrayList<>(resources));
            return this;
        }

        public PluginManifest build() {
            return manifest;
        }
    }
}
