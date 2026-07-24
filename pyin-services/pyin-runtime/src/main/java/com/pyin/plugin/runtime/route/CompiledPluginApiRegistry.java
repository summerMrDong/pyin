package com.pyin.plugin.runtime.route;

import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginAccessMode;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 插件 API 规则编译注册表。
 *
 * <p>该组件属于插件运行时事实层：负责把扫描得到的 {@link PluginApiDefinition} 编译为
 * 可匹配的路径规则，并在插件元数据刷新时替换指定插件的规则快照。它不访问数据库，也不做鉴权。</p>
 */
@Component
public class CompiledPluginApiRegistry {

    private static final Pattern URI_VARIABLE_PATTERN = Pattern.compile("\\{[^/]+}");

    private final PathPatternParser pathPatternParser = new PathPatternParser();
    private final Map<String, Map<String, List<CompiledApiRule>>> registry = new ConcurrentHashMap<>();

    /**
     * 编译单个插件的 API 规则快照。
     *
     * @param descriptor 插件运行时描述对象；其 {@code apis} 字段为空时会得到空规则快照。
     * @return 可原子替换进注册表的规则快照。
     * @throws IllegalStateException 当同一插件、同一 HTTP 方法、同一访问模式下存在冲突路径时抛出。
     */
    public CompiledRegistrySnapshot compile(ResolvedPluginDescriptor descriptor) {
        Map<String, Map<String, CompiledApiRule>> uniquenessMap = new LinkedHashMap<>();
        Map<String, List<CompiledApiRule>> rulesByMethod = new LinkedHashMap<>();
        List<PluginApiDefinition> apis = descriptor.getApis() == null ? List.of() : descriptor.getApis();

        for (PluginApiDefinition apiDefinition : apis) {
            String httpMethod = normalizeMethod(apiDefinition.method());
            PluginAccessMode accessMode = apiDefinition.accessMode();
            String rawPathPattern = normalizePath(apiDefinition.path());
            String canonicalPathPattern = canonicalizePathPattern(rawPathPattern);

            String uniquenessKey = normalizeAccessMode(accessMode) + " " + canonicalPathPattern;
            uniquenessMap.computeIfAbsent(httpMethod, key -> new LinkedHashMap<>());
            if (uniquenessMap.get(httpMethod).containsKey(uniquenessKey)) {
                throw new IllegalStateException(
                        "Conflicting plugin api path pattern: "
                                + descriptor.getPluginId()
                                + " "
                                + normalizeAccessMode(accessMode)
                                + " "
                                + httpMethod
                                + " "
                                + rawPathPattern
                );
            }

            CompiledApiRule rule = new CompiledApiRule(
                    descriptor.getPluginId(),
                    httpMethod,
                    rawPathPattern,
                    canonicalPathPattern,
                    pathPatternParser.parse(rawPathPattern),
                    normalizePath(apiDefinition.internalPath()),
                    apiDefinition.permissionCode(),
                    accessMode,
                    apiDefinition.auditEnabled()
            );
            uniquenessMap.get(httpMethod).put(uniquenessKey, rule);
            rulesByMethod.computeIfAbsent(httpMethod, key -> new ArrayList<>()).add(rule);
        }

        rulesByMethod.replaceAll((httpMethod, rules) -> rules.stream()
                .sorted(Comparator.comparing(CompiledApiRule::compiledPattern, PathPattern.SPECIFICITY_COMPARATOR))
                .toList());

        return new CompiledRegistrySnapshot(descriptor.getPluginId(), rulesByMethod);
    }

    /**
     * 用新的规则快照替换指定插件的当前 API 规则。
     *
     * @param snapshot 由 {@link #compile(ResolvedPluginDescriptor)} 生成的插件规则快照。
     */
    public void replace(CompiledRegistrySnapshot snapshot) {
        registry.put(snapshot.pluginId(), snapshot.rulesByMethod());
    }

    /**
     * 移除指定插件的 API 规则。
     *
     * @param pluginId 插件 ID；为空时不做任何处理。
     */
    public void remove(String pluginId) {
        if (pluginId != null) {
            registry.remove(pluginId);
        }
    }

    /**
     * 匹配插件 API 规则。
     *
     * @param pluginId 插件 ID。
     * @param method HTTP 方法。
     * @param actualPath 插件网关相对路径。
     * @param accessMode 期望访问模式；为 {@code null} 时不按访问模式过滤。
     * @return 命中的 API 规则；未命中时返回 {@link Optional#empty()}。
     */
    public Optional<CompiledApiRule> match(String pluginId, String method, String actualPath, PluginAccessMode accessMode) {
        Map<String, List<CompiledApiRule>> methodMap = registry.get(pluginId);
        if (methodMap == null) {
            return Optional.empty();
        }
        List<CompiledApiRule> candidates = methodMap.get(normalizeMethod(method));
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        PathContainer pathContainer = PathContainer.parsePath(normalizePath(actualPath));
        return candidates.stream()
                .filter(rule -> accessMode == null || accessMode == rule.accessMode())
                .filter(rule -> rule.compiledPattern().matches(pathContainer))
                .findFirst();
    }

    private String normalizeMethod(String method) {
        return method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String normalizeAccessMode(PluginAccessMode accessMode) {
        return accessMode == null ? "" : accessMode.name();
    }

    private String canonicalizePathPattern(String rawPathPattern) {
        return URI_VARIABLE_PATTERN.matcher(normalizePath(rawPathPattern)).replaceAll("{}");
    }

    /**
     * 单个插件的已编译 API 规则快照。
     *
     * @param pluginId 插件 ID。
     * @param rulesByMethod 按 HTTP 方法分组的 API 规则。
     */
    public record CompiledRegistrySnapshot(
            String pluginId,
            Map<String, List<CompiledApiRule>> rulesByMethod
    ) {
    }
}
