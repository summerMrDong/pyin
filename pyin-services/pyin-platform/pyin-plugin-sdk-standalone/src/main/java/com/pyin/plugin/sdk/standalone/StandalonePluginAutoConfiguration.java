package com.pyin.plugin.sdk.standalone;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(StandalonePluginProperties.class)
public class StandalonePluginAutoConfiguration {

    @Bean
    public StandalonePluginRegistrar standalonePluginRegistrar(RestClient.Builder builder) {
        return new StandalonePluginRegistrar(builder);
    }
}
