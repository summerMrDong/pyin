package com.pyin.plugin.runtime.route;

import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 插件动态 API 路由解析服务。
 *
 * <p>该服务是插件运行时对网关暴露的查询入口。它统一判断插件是否存在、返回当前运行状态，
 * 并在插件已注册 API 中按通道、HTTP 方法和相对路径进行匹配。认证、授权、审计写入和请求转发
 * 均由调用方所在领域完成。</p>
 */
@Service
public class PluginApiRouteService {

    private final PluginRegistry pluginRegistry;
    private final CompiledPluginApiRegistry compiledPluginApiRegistry;

    public PluginApiRouteService(
            PluginRegistry pluginRegistry,
            CompiledPluginApiRegistry compiledPluginApiRegistry
    ) {
        this.pluginRegistry = pluginRegistry;
        this.compiledPluginApiRegistry = compiledPluginApiRegistry;
    }

    /**
     * 解析插件动态 API。
     *
     * @param pluginId 插件 ID。
     * @param channel 访问通道，决定允许匹配的 API 访问模式。
     * @param method HTTP 方法。
     * @param relativePath 插件网关相对路径，例如 {@code /items/1}。
     * @return 路由解析结果：插件不存在时 {@link PluginRouteResolution#plugin()} 为空；插件存在但
     * API 未命中时 {@link PluginRouteResolution#route()} 为空。
     */
    public PluginRouteResolution resolve(String pluginId, PluginApiChannel channel, String method, String relativePath) {
        RegisteredPlugin plugin = pluginRegistry.getRegistration(pluginId);
        if (plugin == null) {
            return new PluginRouteResolution(null, Optional.empty());
        }
        Optional<PluginApiRoute> route = compiledPluginApiRegistry
                .match(pluginId, method, relativePath, channel.accessMode())
                .map(rule -> new PluginApiRoute(
                        plugin.pluginId(),
                        plugin.status(),
                        plugin.sourceType(),
                        rule.accessMode(),
                        rule.internalPath(),
                        rule.permissionCode(),
                        rule.auditEnabled(),
                        plugin.backendBaseUrl(),
                        rule.rawPathPattern()
                ));
        return new PluginRouteResolution(plugin, route);
    }

    /**
     * 插件路由解析过程的完整结果。
     *
     * @param plugin 当前注册插件；插件不存在时为 {@code null}。
     * @param route 命中的 API 路由；插件存在但 API 未发布或访问模式不匹配时为空。
     */
    public record PluginRouteResolution(
            RegisteredPlugin plugin,
            Optional<PluginApiRoute> route
    ) {
    }
}
