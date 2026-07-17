package com.pyin.plugin.client.dict.starter.autoconfigure;

import com.pyin.plugin.client.core.context.DefaultPyinClientFeatureContext;
import com.pyin.plugin.client.core.registry.PyinClientFeatureRegistry;
import com.pyin.plugin.client.dict.PyinDictClient;
import com.pyin.plugin.client.dict.support.DictClientFeature;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PyinDictClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DictClientFeature dictClientFeature(
            DefaultPyinClientFeatureContext context,
            PyinClientFeatureRegistry registry
    ) {
        DictClientFeature feature = new DictClientFeature();
        feature.initialize(context);
        registry.register(feature);
        return feature;
    }

    @Bean
    @ConditionalOnMissingBean
    public PyinDictClient pyinDictClient(DictClientFeature feature) {
        return feature;
    }
}
