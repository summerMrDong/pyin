package com.pyin.plugin.sdk.manifest;

import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginAccessMode;
import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginPermission;
import com.pyin.plugin.spi.model.PluginPermissionResourceType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 扫描插件控制器注解并生成接口与权限定义。
 */
@Component
public class PluginApiScanner {

    public PluginScanResult scan(ApplicationContext applicationContext, PyinPlugin plugin) {
        String packagePrefix = ClassUtils.getPackageName(AopUtils.getTargetClass(plugin));
        String pluginId = plugin.pluginId();
        List<PluginApiDefinition> apis = new ArrayList<>();
        Map<String, PluginPermission> permissionMap = new LinkedHashMap<>();
        Map<String, String> apiKeys = new LinkedHashMap<>();

        scanControllers(
                applicationContext.getBeansWithAnnotation(AdminMapping.class).values(),
                packagePrefix,
                pluginId,
                "/admin",
                PluginAccessMode.CENTER_ADMIN_ONLY,
                apis,
                permissionMap,
                apiKeys
        );
        scanControllers(
                applicationContext.getBeansWithAnnotation(OpenMapping.class).values(),
                packagePrefix,
                pluginId,
                "/open",
                PluginAccessMode.CLIENT_SDK_GATEWAY,
                apis,
                permissionMap,
                apiKeys
        );

        return new PluginScanResult(apis, List.copyOf(permissionMap.values()));
    }

    private void scanControllers(
            Collection<Object> beans,
            String packagePrefix,
            String pluginId,
            String internalPrefix,
            PluginAccessMode accessMode,
            List<PluginApiDefinition> apis,
            Map<String, PluginPermission> permissionMap,
            Map<String, String> apiKeys
    ) {
        for (Object bean : beans) {
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            if (!beanClass.getPackageName().startsWith(packagePrefix)) {
                continue;
            }
            for (Method method : beanClass.getDeclaredMethods()) {
                if (!hasRouteMapping(method)) {
                    continue;
                }
                String relativePath = resolveSpringMappingPath(beanClass, method, "");
                String httpMethod = resolveHttpMethod(method);
                if (!StringUtils.hasText(httpMethod)) {
                    throw new IllegalStateException("插件接口必须声明唯一 HTTP 方法: " + method);
                }

                String publicPath = normalizePath("/" + pluginId + internalPrefix, relativePath);
                String internalPath = publicPath;
                String gatewayPath = normalizeGatewayApiPath(publicPath, pluginId);
                registerApi(apiKeys, accessMode, httpMethod, gatewayPath);

                Permission permission = AnnotatedElementUtils.findMergedAnnotation(method, Permission.class);
                String permissionCode = resolvePermissionCode(permission, publicPath, httpMethod);
                boolean auditEnabled = permission == null || permission.auditEnabled();

                apis.add(new PluginApiDefinition(
                        gatewayPath,
                        httpMethod,
                        internalPath,
                        accessMode,
                        permissionCode,
                        auditEnabled
                ));

                registerPermission(permissionMap, permissionCode, permission);
            }
        }
    }

    private void registerPermission(
            Map<String, PluginPermission> permissionMap,
            String permissionCode,
            Permission permission
    ) {
        String permissionName = permission == null ? "" : permission.name();
        PluginPermissionResourceType resourceType =
                permission == null ? PluginPermissionResourceType.API : permission.resourceType();
        PluginPermission pluginPermission = new PluginPermission(permissionCode, permissionName, resourceType);
        PluginPermission existing = permissionMap.putIfAbsent(permissionCode, pluginPermission);
        if (existing != null
                && (!existing.name().equals(pluginPermission.name())
                || existing.resourceType() != pluginPermission.resourceType())) {
            throw new IllegalStateException("Conflicting permission metadata for code: " + permissionCode);
        }
    }

    private void registerApi(Map<String, String> apiKeys, PluginAccessMode accessMode, String method, String path) {
        String apiKey = accessMode.name() + " " + method + " " + path;
        String existing = apiKeys.putIfAbsent(apiKey, apiKey);
        if (existing != null) {
            throw new IllegalStateException("Duplicate api mapping detected: " + apiKey);
        }
    }

    static String normalizeGatewayApiPath(String publicPath, String pluginId) {
        String normalized = normalizePath("", publicPath);
        String pluginPrefix = "/" + pluginId;
        if (!normalized.startsWith(pluginPrefix)) {
            return normalized;
        }
        String path = normalized.substring(pluginPrefix.length());
        if (path.startsWith("/admin")) {
            path = path.substring("/admin".length());
        } else if (path.startsWith("/open")) {
            path = path.substring("/open".length());
        }
        return normalizePath("", path);
    }

    static String normalizePath(String classPath, String methodPath) {
        String left = classPath == null ? "" : classPath.trim();
        String right = methodPath == null ? "" : methodPath.trim();
        if (!left.startsWith("/") && !left.isEmpty()) {
            left = "/" + left;
        }
        if (!right.startsWith("/") && !right.isEmpty()) {
            right = "/" + right;
        }
        if (left.endsWith("/") && right.startsWith("/")) {
            return left.substring(0, left.length() - 1) + right;
        }
        if (left.isEmpty()) {
            return right.isEmpty() ? "/" : right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + right;
    }

    private String resolveSpringMappingPath(Class<?> beanClass, Method method, String fallbackPath) {
        String classPath = firstMappingPath(AnnotatedElementUtils.findMergedAnnotation(beanClass, RequestMapping.class));
        String methodPath = resolveMethodMappingPath(method);
        String combined = normalizePath(classPath, methodPath);
        return "/".equals(combined) ? normalizePath("", fallbackPath) : combined;
    }

    private boolean hasRouteMapping(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, GetMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, PostMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, PutMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, DeleteMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, PatchMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    private String resolveMethodMappingPath(Method method) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping != null) {
            String path = firstMappingPath(requestMapping);
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        GetMapping getMapping = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
        if (getMapping != null) {
            String path = firstMappingPath(getMapping.path(), getMapping.value());
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        PostMapping postMapping = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
        if (postMapping != null) {
            String path = firstMappingPath(postMapping.path(), postMapping.value());
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        PutMapping putMapping = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
        if (putMapping != null) {
            String path = firstMappingPath(putMapping.path(), putMapping.value());
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        DeleteMapping deleteMapping = AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class);
        if (deleteMapping != null) {
            String path = firstMappingPath(deleteMapping.path(), deleteMapping.value());
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        PatchMapping patchMapping = AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class);
        if (patchMapping != null) {
            String path = firstMappingPath(patchMapping.path(), patchMapping.value());
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        return "";
    }

    private String firstMappingPath(RequestMapping requestMapping) {
        if (requestMapping == null) {
            return "";
        }
        return firstMappingPath(requestMapping.path(), requestMapping.value());
    }

    private String firstMappingPath(String[] path, String[] value) {
        if (path.length > 0 && StringUtils.hasText(path[0])) {
            return path[0];
        }
        if (value.length > 0 && StringUtils.hasText(value[0])) {
            return value[0];
        }
        return "";
    }

    private String resolveHttpMethod(Method method) {
        if (AnnotatedElementUtils.hasAnnotation(method, GetMapping.class)) {
            return "GET";
        }
        if (AnnotatedElementUtils.hasAnnotation(method, PostMapping.class)) {
            return "POST";
        }
        if (AnnotatedElementUtils.hasAnnotation(method, PutMapping.class)) {
            return "PUT";
        }
        if (AnnotatedElementUtils.hasAnnotation(method, DeleteMapping.class)) {
            return "DELETE";
        }
        if (AnnotatedElementUtils.hasAnnotation(method, PatchMapping.class)) {
            return "PATCH";
        }
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping == null || requestMapping.method().length == 0) {
            return "";
        }
        if (requestMapping.method().length > 1) {
            throw new IllegalStateException("@RequestMapping 仅支持声明单个 HTTP 方法: " + method);
        }
        RequestMethod requestMethod = requestMapping.method()[0];
        return requestMethod.name();
    }

    private String resolvePermissionCode(Permission permission, String publicPath, String httpMethod) {
        if (permission != null) {
            if (StringUtils.hasText(permission.value())) {
                return permission.value();
            }
            if (StringUtils.hasText(permission.code())) {
                return permission.code();
            }
        }
        return generatePermissionCode(publicPath, httpMethod);
    }

    static String generatePermissionCode(String publicPath, String httpMethod) {
        String normalizedPath = normalizePath("", publicPath).toLowerCase(Locale.ROOT);
        String permissionPath = normalizedPath
                .replace("{", "")
                .replace("}", "")
                .replace('/', ':');
        while (permissionPath.startsWith(":")) {
            permissionPath = permissionPath.substring(1);
        }
        if (!StringUtils.hasText(permissionPath)) {
            permissionPath = "root";
        }
        return permissionPath + ":" + httpMethod.toLowerCase(Locale.ROOT);
    }
}
