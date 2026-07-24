package com.pyin.gateway.filter;

import com.pyin.gateway.exception.PluginGatewayException;
import com.pyin.gateway.exception.PluginGatewayExceptionFactory;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.path.PluginGatewayPathSupport;
import com.pyin.gateway.support.PluginGatewayInternalSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 插件 Controller 访问保护过滤器。
 *
 * <p>插件 Controller 直接注册在规范网关路径 {@code /plugins/{pluginId}/admin|open/**} 上。
 * 请求必须先经过统一插件网关过滤器，并带有放行标记，才能继续进入插件 Controller；这样可以
 * 避免过滤器顺序或测试装配变化时绕过网关的路由与鉴权决策。</p>
 */
@Component
// 必须在统一网关完成鉴权并写入放行标记之后执行。
@Order(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 1)
public class PluginGatewayInternalGuardFilter extends OncePerRequestFilter {

    private final PluginGatewayExceptionResolver pluginGatewayExceptionResolver;

    public PluginGatewayInternalGuardFilter(PluginGatewayExceptionResolver pluginGatewayExceptionResolver) {
        this.pluginGatewayExceptionResolver = pluginGatewayExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PluginGatewayPathSupport.isPluginGatewayRequest(PluginGatewayPathSupport.requestPath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            if (!PluginGatewayInternalSupport.isAllowed(request)) {
                throw PluginGatewayExceptionFactory.forbidden("插件接口必须通过中心插件网关完成路由与鉴权");
            }
            filterChain.doFilter(request, response);
        } catch (PluginGatewayException exception) {
            pluginGatewayExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
