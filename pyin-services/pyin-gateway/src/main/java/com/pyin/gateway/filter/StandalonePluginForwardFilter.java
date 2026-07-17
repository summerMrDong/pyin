package com.pyin.gateway.filter;

import com.pyin.gateway.exception.PluginGatewayException;
import com.pyin.gateway.exception.PluginGatewayExceptionFactory;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.forward.StandalonePluginForwardService;
import com.pyin.gateway.path.PluginGatewayPathSupport;
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
 * 动态插件接口的运行模式分流过滤器。
 *
 * <p>它位于“可用性检查”和“鉴权”之后，职责是根据插件来源类型决定请求下一步去向：
 * 1. {@code EMBEDDED_SYSTEM} 直接放行，由本地真实 controller 处理
 * 2. {@code STANDALONE_NODE} 直接转发到独立插件后端服务
 *
 * <p>虽然前置过滤器已经做过一次注册表检查，这里仍然保留防御式校验，
 * 这样该过滤器单独复用或调整顺序时依然具备完整语义，不依赖外部隐式前提。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class StandalonePluginForwardFilter extends OncePerRequestFilter {

    private final PluginRegistry pluginRegistry;
    private final StandalonePluginForwardService standalonePluginForwardService;
    private final PluginGatewayExceptionResolver pluginGatewayExceptionResolver;

    public StandalonePluginForwardFilter(
            PluginRegistry pluginRegistry,
            StandalonePluginForwardService standalonePluginForwardService,
            PluginGatewayExceptionResolver pluginGatewayExceptionResolver
    ) {
        this.pluginRegistry = pluginRegistry;
        this.standalonePluginForwardService = standalonePluginForwardService;
        this.pluginGatewayExceptionResolver = pluginGatewayExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 仅处理动态插件接口，请求不命中 admin/open 动态路径时直接跳过。
        return !PluginGatewayPathSupport.isPluginGatewayRequest(PluginGatewayPathSupport.requestPath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String requestPath = PluginGatewayPathSupport.requestPath(request);
            String pluginId = PluginGatewayPathSupport.extractPluginId(requestPath);
            RegisteredPlugin plugin = pluginRegistry.getRegistration(pluginId);
            if (plugin == null) {
                throw PluginGatewayExceptionFactory.pluginNotFound(pluginId);
            }
            if (plugin.status() != PluginRuntimeStatus.STARTED) {
                throw PluginGatewayExceptionFactory.pluginUnavailable(pluginId, plugin.status());
            }
            if (plugin.sourceType() != PluginSourceType.STANDALONE_NODE) {
                // 内嵌式插件已经在当前 Spring 容器里注册了真实路由，直接继续后续处理。
                filterChain.doFilter(request, response);
                return;
            }
            // 独立式插件不命中本地 controller，而是由网关负责网络转发。
            standalonePluginForwardService.forward(plugin, request, response);
        } catch (PluginGatewayException exception) {
            pluginGatewayExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
