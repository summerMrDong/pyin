package com.pyin.plugin.spi.model;

/**
 * 独立部署插件向中心注册的节点协议。
 */
public class PluginNodeRegistration {

    private String pluginId;
    private String nodeId;
    private String backendBaseUrl;
    private String frontendBaseUrl;
    private String healthUrl;
    private ResolvedPluginDescriptor descriptor;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
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

    public ResolvedPluginDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(ResolvedPluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }
}
