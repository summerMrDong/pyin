package com.pyin.plugin.sdk.standalone;

import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "pyin.plugin")
@Getter
@Setter
public class StandalonePluginProperties implements EnvironmentAware {

    private String centerUrl;
    private String nodeId;
    private String advertiseBaseUrl;
    private Long heartbeatIntervalMs = 15000L;

    /**
     * @deprecated use advertiseBaseUrl instead.
     */
    @Deprecated
    private String backendBaseUrl;

    /**
     * @deprecated use advertiseBaseUrl instead.
     */
    @Deprecated
    private String frontendBaseUrl;

    /**
     * @deprecated use advertiseBaseUrl instead.
     */
    @Deprecated
    private String healthUrl;

    private Environment environment;

    public String getResolvedBackendBaseUrl() {
        if (StringUtils.hasText(backendBaseUrl)) {
            return backendBaseUrl;
        }
        return normalizeBaseUrl(resolveAdvertiseBaseUrl());
    }

    public String getResolvedFrontendBaseUrl(String pluginId) {
        if (StringUtils.hasText(frontendBaseUrl)) {
            return normalizeBaseUrl(frontendBaseUrl);
        }
        return getResolvedBackendBaseUrl() + "/plugin-static/" + pluginId;
    }

    public String getResolvedHealthUrl() {
        if (StringUtils.hasText(healthUrl)) {
            return normalizeBaseUrl(healthUrl);
        }
        return getResolvedBackendBaseUrl() + "/health";
    }

    public String getResolvedNodeId(String pluginId) {
        if (StringUtils.hasText(nodeId)) {
            return nodeId;
        }
        return pluginId + "@" + resolveAdvertiseBaseUrl();
    }

    private String resolveAdvertiseBaseUrl() {
        if (StringUtils.hasText(advertiseBaseUrl)) {
            return advertiseBaseUrl;
        }
        String address = environment == null ? null : environment.getProperty("server.address");
        String host = StringUtils.hasText(address) ? address : resolveLocalHost();
        String port = environment == null ? "8080" : environment.getProperty("server.port", "8080");
        String contextPath = environment == null ? "" : environment.getProperty("server.servlet.context-path", "");
        String normalizedContextPath = StringUtils.hasText(contextPath)
                ? (contextPath.startsWith("/") ? contextPath : "/" + contextPath)
                : "";
        return "http://" + host + ":" + port + normalizedContextPath;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String resolveLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            return "127.0.0.1";
        }
    }
}
