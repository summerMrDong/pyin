package com.pyin.plugin.sdk.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@AutoConfiguration
public class PluginWebMvcAutoConfiguration {

    @Bean
    WebMvcRegistrations pluginWebMvcRegistrations(PluginOwnershipResolver pluginOwnershipResolver) {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new PluginRequestMappingHandlerMapping(pluginOwnershipResolver);
            }
        };
    }
}
