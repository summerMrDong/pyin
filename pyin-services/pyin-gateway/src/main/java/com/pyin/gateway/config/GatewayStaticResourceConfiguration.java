package com.pyin.gateway.config;

import com.pyin.gateway.staticresource.GatewayPluginStaticResourceResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GatewayStaticResourceConfiguration implements WebMvcConfigurer {

    private final GatewayPluginStaticResourceResolver resourceResolver;

    public GatewayStaticResourceConfiguration(GatewayPluginStaticResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/plugin-static/**")
                .addResourceLocations("classpath:/")
                .resourceChain(false)
                .addResolver(resourceResolver);
    }
}
