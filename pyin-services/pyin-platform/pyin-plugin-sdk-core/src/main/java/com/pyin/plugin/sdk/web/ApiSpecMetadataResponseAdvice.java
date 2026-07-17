package com.pyin.plugin.sdk.web;

import com.pyin.plugin.sdk.annotation.ApiSpecMeta;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.FunctionalPoint;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = Controller.class)
public class ApiSpecMetadataResponseAdvice implements ResponseBodyAdvice<Object> {

    public static final String HEADER_API_ID = "X-Pyin-Api-Id";
    public static final String HEADER_MODULE_ID = "X-Pyin-Module-Id";
    public static final String HEADER_FUNCTION_POINT_ID = "X-Pyin-Function-Point-Id";
    public static final String HEADER_ROLE_ID = "X-Pyin-Role-Id";
    public static final String HEADER_PLUGIN_ID = "X-Pyin-Plugin-Id";

    private final PluginOwnershipResolver pluginOwnershipResolver;

    public ApiSpecMetadataResponseAdvice(PluginOwnershipResolver pluginOwnershipResolver) {
        this.pluginOwnershipResolver = pluginOwnershipResolver;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(ApiSpecMeta.class)
                || returnType.hasMethodAnnotation(FunctionalPoint.class);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        ApiSpecMeta apiSpec = returnType.getMethodAnnotation(ApiSpecMeta.class);
        if (apiSpec != null) {
            response.getHeaders().add(HEADER_API_ID, apiSpec.apiId());
            response.getHeaders().add(HEADER_MODULE_ID, apiSpec.moduleId());
            response.getHeaders().add(HEADER_FUNCTION_POINT_ID, apiSpec.functionPointId());
            response.getHeaders().add(HEADER_ROLE_ID, apiSpec.roleId());
            response.getHeaders().add(HEADER_PLUGIN_ID, apiSpec.pluginId());
            return body;
        }

        FunctionalPoint fp = returnType.getMethodAnnotation(FunctionalPoint.class);
        if (fp != null) {
            response.getHeaders().add(HEADER_API_ID, fp.id().replace("FP-", "API-"));
            response.getHeaders().add(HEADER_MODULE_ID, fp.module());
            response.getHeaders().add(HEADER_FUNCTION_POINT_ID, fp.id());

            Class<?> controllerClass = returnType.getContainingClass();
            if (controllerClass.isAnnotationPresent(OpenMapping.class)) {
                response.getHeaders().add(HEADER_ROLE_ID, "CITIZEN");
            } else if (controllerClass.isAnnotationPresent(AdminMapping.class)) {
                response.getHeaders().add(HEADER_ROLE_ID, "ADMIN");
            }

            response.getHeaders().add(HEADER_PLUGIN_ID, pluginOwnershipResolver.resolvePluginId(controllerClass));
        }
        return body;
    }
}
