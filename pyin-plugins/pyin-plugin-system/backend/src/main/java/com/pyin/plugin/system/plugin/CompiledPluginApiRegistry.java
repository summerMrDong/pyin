package com.pyin.plugin.system.plugin;

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

@Component
public class CompiledPluginApiRegistry {

    private static final Pattern URI_VARIABLE_PATTERN = Pattern.compile("\\{[^/]+}");

    private final PathPatternParser pathPatternParser = new PathPatternParser();
    private final Map<String, Map<String, List<CompiledApiRule>>> registry = new ConcurrentHashMap<>();

    public CompiledRegistrySnapshot compile(ResolvedPluginDescriptor descriptor) {
        Map<String, Map<String, CompiledApiRule>> uniquenessMap = new LinkedHashMap<>();
        Map<String, List<CompiledApiRule>> rulesByMethod = new LinkedHashMap<>();
        List<PluginApiDefinition> apis = descriptor.getApis() == null ? List.of() : descriptor.getApis();

        for (PluginApiDefinition apiDefinition : apis) {
            String httpMethod = normalizeMethod(apiDefinition.method());
            String accessMode = normalizeAccessMode(apiDefinition.accessMode());
            String rawPathPattern = normalizePath(apiDefinition.path());
            String canonicalPathPattern = canonicalizePathPattern(rawPathPattern);

            String uniquenessKey = accessMode + " " + canonicalPathPattern;
            uniquenessMap.computeIfAbsent(httpMethod, key -> new LinkedHashMap<>());
            if (uniquenessMap.get(httpMethod).containsKey(uniquenessKey)) {
                throw new IllegalStateException(
                        "Conflicting plugin api path pattern: "
                                + descriptor.getPluginId()
                                + " "
                                + accessMode
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
                    apiDefinition.internalPath(),
                    apiDefinition.permissionCode(),
                    apiDefinition.accessMode().name(),
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

    public void replace(CompiledRegistrySnapshot snapshot) {
        registry.put(snapshot.pluginId(), snapshot.rulesByMethod());
    }

    public Optional<CompiledApiRule> match(String pluginId, String method, String actualPath) {
        return match(pluginId, method, actualPath, null);
    }

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
                .filter(rule -> accessMode == null || accessMode.name().equals(rule.accessMode()))
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

    public record CompiledRegistrySnapshot(
            String pluginId,
            Map<String, List<CompiledApiRule>> rulesByMethod
    ) {
    }
}
