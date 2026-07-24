package com.pyin.plugin.runtime.loader;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyin.center.plugin-runtime")
@Getter
@Setter
public class PluginRuntimeProperties {

    private String sourcePluginsDir = "pyin-plugins";
    private String runtimeRoot = "pyin-distribution-parent/runtime/pyin-config-center-runtime";
    private String bundledPluginsDir = "pyin-distribution-parent/bundled-plugins";
    private String systemPluginsDir = "pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/system";
    private String externalPluginsDir = "pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/external";
    /**
     * 允许被中心 JVM 注册为内嵌插件的受控插件 ID 列表。
     *
     * <p>该配置属于平台部署策略，不是插件 Manifest 的可声明能力。未列入此列表的插件即使出现在
     * Spring 容器中，也不会被注册为内嵌插件。</p>
     */
    private List<String> embeddedPluginIds = List.of("system", "config", "dict");

}
