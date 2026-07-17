package com.pyin.plugin.client.config.starter.autoconfigure;

import com.pyin.plugin.client.core.context.DefaultPyinClientFeatureContext;
import com.pyin.plugin.client.core.registry.PyinClientFeatureRegistry;
import com.pyin.plugin.client.config.PyinConfigClient;
import com.pyin.plugin.client.config.support.ConfigClientFeature;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PyinConfigClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigClientFeature configClientFeature(
            DefaultPyinClientFeatureContext context,
            PyinClientFeatureRegistry registry
    ) {
        ConfigClientFeature feature = new ConfigClientFeature();
        feature.initialize(context);
        registry.register(feature);
        return feature;
    }

    @Bean
    @ConditionalOnMissingBean
    public PyinConfigClient pyinConfigClient(ConfigClientFeature feature) {
        return feature;
    }
}
