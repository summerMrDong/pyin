package com.pyin.gateway.filter;

import com.pyin.gateway.exception.PluginGatewayException;
import com.pyin.gateway.exception.PluginGatewayExceptionFactory;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.path.PluginGatewayPathSupport;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 插件动态接口的第一道前置过滤器。
 *
 * <p>职责只聚焦在“插件是否可以被访问”：
 * 1. 仅拦截 {@code /{pluginId}/admin/**} 和 {@code /{pluginId}/open/**}
 * 2. 解析路径中的 {@code pluginId}
 * 3. 查询插件注册表，确认插件已注册且状态为 {@code STARTED}
 *
 * <p>这里故意不做鉴权，也不做独立插件转发，
 * 这样后续过滤器可以建立在“插件一定存在且可用”的前提上继续处理。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class PluginAvailabilityFilter extends OncePerRequestFilter {

    private final PluginRegistry pluginRegistry;
    private final PluginGatewayExceptionResolver pluginGatewayExceptionResolver;

    public PluginAvailabilityFilter(
            PluginRegistry pluginRegistry,
            PluginGatewayExceptionResolver pluginGatewayExceptionResolver
    ) {
        this.pluginRegistry = pluginRegistry;
        this.pluginGatewayExceptionResolver = pluginGatewayExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只处理动态插件接口，不干预 /plugin-static/** 或系统其他请求。
        return !PluginGatewayPathSupport.isPluginGatewayRequest(PluginGatewayPathSupport.requestPath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 到这里说明已经命中了动态插件网关路径，可以安全按插件语义解析 pluginId。
            String pluginId = PluginGatewayPathSupport.extractPluginId(PluginGatewayPathSupport.requestPath(request));
            RegisteredPlugin plugin = pluginRegistry.getRegistration(pluginId);
            if (plugin == null) {
                throw PluginGatewayExceptionFactory.pluginNotFound(pluginId);
            }
            if (plugin.status() != PluginRuntimeStatus.STARTED) {
                throw PluginGatewayExceptionFactory.pluginUnavailable(pluginId, plugin.status());
            }
            // 插件存在且可用，后续链路再继续做鉴权和运行模式分流。
            filterChain.doFilter(request, response);
        } catch (PluginGatewayException exception) {
            pluginGatewayExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
