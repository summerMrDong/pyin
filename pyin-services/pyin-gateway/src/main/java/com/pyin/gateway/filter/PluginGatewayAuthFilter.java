package com.pyin.gateway.filter;

import com.pyin.center.auth.gateway.GatewayAuthenticationException;
import com.pyin.center.auth.gateway.GatewayAdminAuthService;
import com.pyin.gateway.exception.PluginGatewayException;
import com.pyin.gateway.exception.PluginGatewayExceptionFactory;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.path.PluginGatewayPathSupport;
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
 * 插件动态接口的鉴权过滤器。
 *
 * <p>它位于“插件可用性检查”之后，只负责访问身份校验：
 * 1. {@code /{pluginId}/admin/**} 统一复用后台 Sa-Token 登录态
 * 2. {@code /{pluginId}/open/**} 当前版本默认放行，预留未来接入密钥鉴权
 *
 * <p>这样可以把“插件是否存在”和“调用者是否有资格访问”拆成两层，
 * 让错误语义更清晰，也方便未来替换 open 链路的鉴权策略。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PluginGatewayAuthFilter extends OncePerRequestFilter {

    private final GatewayAdminAuthService gatewayAdminAuthService;
    private final PluginGatewayExceptionResolver pluginGatewayExceptionResolver;

    public PluginGatewayAuthFilter(
            GatewayAdminAuthService gatewayAdminAuthService,
            PluginGatewayExceptionResolver pluginGatewayExceptionResolver
    ) {
        this.gatewayAdminAuthService = gatewayAdminAuthService;
        this.pluginGatewayExceptionResolver = pluginGatewayExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只关心动态插件接口，静态资源与普通业务接口不走这里。
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
            if (PluginGatewayPathSupport.isAdminRequest(requestPath)) {
                // admin 语义统一纳入后台登录态保护。
                gatewayAdminAuthService.checkAdminRequest();
            }
            // open 目前不做拦截式鉴权，后续若升级密钥校验，可直接在这里扩展。
            filterChain.doFilter(request, response);
        } catch (GatewayAuthenticationException exception) {
            pluginGatewayExceptionResolver.resolveException(
                    request,
                    response,
                    null,
                    PluginGatewayExceptionFactory.unauthorized(exception.getMessage())
            );
        } catch (PluginGatewayException exception) {
            pluginGatewayExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
