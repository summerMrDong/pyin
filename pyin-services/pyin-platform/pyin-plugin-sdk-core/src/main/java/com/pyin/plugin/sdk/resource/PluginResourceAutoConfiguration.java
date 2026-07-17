package com.pyin.plugin.sdk.resource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PluginResourceProperties.class)
public class PluginResourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PluginResourceResolver.class)
    public PluginResourceResolver pluginResourceResolver(PluginResourceProperties properties) {
        return new DefaultPluginResourceResolver(properties);
    }
}
