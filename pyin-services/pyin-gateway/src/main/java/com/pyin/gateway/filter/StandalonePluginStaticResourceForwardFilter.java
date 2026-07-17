package com.pyin.gateway.filter;

import com.pyin.gateway.exception.PluginGatewayException;
import com.pyin.gateway.exception.PluginGatewayExceptionFactory;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.forward.StandalonePluginStaticResourceForwardService;
import com.pyin.gateway.path.PluginGatewayPathSupport;
import com.pyin.gateway.path.PluginStaticResourcePathSupport;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 插件静态资源的运行模式分流过滤器。
 *
 * <p>它只处理统一静态资源入口 {@code /plugin-static/{pluginId}/assets/**}：
 * 1. 解析 {@code pluginId}
 * 2. 检查插件是否存在且状态可用
 * 3. 若为独立插件，则直接转发到插件自己的前端静态资源地址
 * 4. 若为内嵌插件，则放行给 Spring 的 ResourceHandler 做本地文件解析
 *
 * <p>这样静态资源链和动态接口链保持一致：
 * 都先按插件注册信息做判断，再决定是“本地处理”还是“远端代理”。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class StandalonePluginStaticResourceForwardFilter extends OncePerRequestFilter {

    private final PluginRegistry pluginRegistry;
    private final StandalonePluginStaticResourceForwardService forwardService;
    private final PluginGatewayExceptionResolver pluginGatewayExceptionResolver;

    public StandalonePluginStaticResourceForwardFilter(
            PluginRegistry pluginRegistry,
            StandalonePluginStaticResourceForwardService forwardService,
            PluginGatewayExceptionResolver pluginGatewayExceptionResolver
    ) {
        this.pluginRegistry = pluginRegistry;
        this.forwardService = forwardService;
        this.pluginGatewayExceptionResolver = pluginGatewayExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只拦截统一的插件静态资源路径，不干预普通静态资源或业务接口。
        return !PluginStaticResourcePathSupport.isPluginStaticAssetRequest(PluginGatewayPathSupport.requestPath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String requestPath = PluginGatewayPathSupport.requestPath(request);
            String pluginId = PluginStaticResourcePathSupport.extractPluginId(requestPath);
            RegisteredPlugin plugin = pluginRegistry.getRegistration(pluginId);
            if (plugin == null) {
                throw PluginGatewayExceptionFactory.pluginNotFound(pluginId);
            }
            if (plugin.status() != PluginRuntimeStatus.STARTED) {
                throw PluginGatewayExceptionFactory.pluginUnavailable(pluginId, plugin.status());
            }
            if (plugin.sourceType() != PluginSourceType.STANDALONE_NODE) {
                // 内嵌插件资源交给后面的 ResourceHandler，从本地 plugin-static 目录读取。
                filterChain.doFilter(request, response);
                return;
            }
            // 独立插件资源由中心网关代理到插件自身前端服务。
            forwardService.forward(plugin, request, response);
        } catch (PluginGatewayException exception) {
            pluginGatewayExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
