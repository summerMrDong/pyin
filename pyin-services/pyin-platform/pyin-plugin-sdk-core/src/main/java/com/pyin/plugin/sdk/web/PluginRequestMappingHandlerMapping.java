package com.pyin.plugin.sdk.web;

import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

class PluginRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    private final PluginOwnershipResolver pluginOwnershipResolver;

    PluginRequestMappingHandlerMapping(PluginOwnershipResolver pluginOwnershipResolver) {
        this.pluginOwnershipResolver = pluginOwnershipResolver;
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo mapping = super.getMappingForMethod(method, handlerType);
        if (mapping == null) {
            return null;
        }
        String fixedSegment = fixedSegment(handlerType);
        if (fixedSegment == null) {
            return mapping;
        }
        String pluginId = pluginOwnershipResolver.resolvePluginId(handlerType);
        RequestMappingInfo prefix = RequestMappingInfo.paths("/" + pluginId + fixedSegment)
                .options(getBuilderConfiguration())
                .build();
        return prefix.combine(mapping);
    }

    private String fixedSegment(Class<?> handlerType) {
        if (handlerType.isAnnotationPresent(AdminMapping.class)) {
            return "/admin";
        }
        if (handlerType.isAnnotationPresent(OpenMapping.class)) {
            return "/open";
        }
        return null;
    }
}
