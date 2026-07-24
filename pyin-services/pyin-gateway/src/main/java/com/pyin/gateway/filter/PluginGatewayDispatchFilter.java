package com.pyin.gateway.filter;

import com.pyin.center.auth.authentication.AdminRequestAuthenticator;
import com.pyin.center.auth.authentication.AuthenticationException;
import com.pyin.center.auth.authentication.AuthorizationException;
import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.center.auth.authentication.ClientRequestAuthenticator;
import com.pyin.gateway.exception.PluginGatewayException;
import com.pyin.gateway.exception.PluginGatewayExceptionFactory;
import com.pyin.gateway.exception.PluginGatewayExceptionResolver;
import com.pyin.gateway.forward.StandalonePluginForwardService;
import com.pyin.gateway.path.PluginGatewayPathSupport;
import com.pyin.gateway.path.PluginGatewayPathSupport.PluginGatewayPath;
import com.pyin.gateway.support.PluginGatewayInternalSupport;
import com.pyin.plugin.runtime.route.PluginApiChannel;
import com.pyin.plugin.runtime.route.PluginApiRoute;
import com.pyin.plugin.runtime.route.PluginApiRouteService;
import com.pyin.plugin.runtime.route.PluginApiRouteService.PluginRouteResolution;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
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
 * 插件动态 API 的统一网关调度过滤器。
 *
 * <p>该过滤器是插件动态接口的中心侧唯一编排入口，统一处理内嵌插件和独立插件：
 * 先解析插件路径，再调用 runtime 匹配插件/API 元数据，随后调用 auth 完成认证授权，最后根据
 * 插件来源决定进入本地 Controller 或签名转发到独立插件。</p>
 */
@Component
// Sa-Token 通过 RequestContextHolder 获取当前请求；必须晚于 OrderedRequestContextFilter 执行。
@Order(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER)
public class PluginGatewayDispatchFilter extends OncePerRequestFilter {

    private final PluginApiRouteService pluginApiRouteService;
    private final AdminRequestAuthenticator adminRequestAuthenticator;
    private final ClientRequestAuthenticator clientRequestAuthenticator;
    private final StandalonePluginForwardService standalonePluginForwardService;
    private final PluginGatewayExceptionResolver pluginGatewayExceptionResolver;

    public PluginGatewayDispatchFilter(
            PluginApiRouteService pluginApiRouteService,
            AdminRequestAuthenticator adminRequestAuthenticator,
            ClientRequestAuthenticator clientRequestAuthenticator,
            StandalonePluginForwardService standalonePluginForwardService,
            PluginGatewayExceptionResolver pluginGatewayExceptionResolver
    ) {
        this.pluginApiRouteService = pluginApiRouteService;
        this.adminRequestAuthenticator = adminRequestAuthenticator;
        this.clientRequestAuthenticator = clientRequestAuthenticator;
        this.standalonePluginForwardService = standalonePluginForwardService;
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
            PluginGatewayPath path = PluginGatewayPathSupport.parse(PluginGatewayPathSupport.requestPath(request));
            if (path == null) {
                filterChain.doFilter(request, response);
                return;
            }

            PluginRouteResolution resolution = pluginApiRouteService.resolve(
                    path.pluginId(),
                    path.channel(),
                    request.getMethod(),
                    path.relativePath()
            );
            if (resolution.plugin() == null) {
                throw PluginGatewayExceptionFactory.pluginNotFound(path.pluginId());
            }
            if (resolution.plugin().status() != PluginRuntimeStatus.STARTED) {
                throw PluginGatewayExceptionFactory.pluginUnavailable(path.pluginId(), resolution.plugin().status());
            }
            PluginApiRoute route = resolution.route()
                    .orElseThrow(() -> PluginGatewayExceptionFactory.apiNotPublished(
                            path.pluginId(),
                            request.getMethod(),
                            path.relativePath()
                    ));

            AuthenticatedPrincipal principal = authenticate(path.channel(), route.permissionCode());
            PluginGatewayInternalSupport.markAllowed(request, route, principal);

            if (route.sourceType() == PluginSourceType.STANDALONE_NODE) {
                standalonePluginForwardService.forward(route, path, principal, request, response);
                return;
            }

            filterChain.doFilter(request, response);
        } catch (AuthorizationException exception) {
            pluginGatewayExceptionResolver.resolveException(
                    request,
                    response,
                    null,
                    PluginGatewayExceptionFactory.forbidden(exception.getMessage())
            );
        } catch (AuthenticationException exception) {
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

    private AuthenticatedPrincipal authenticate(PluginApiChannel channel, String permissionCode) {
        if (channel == PluginApiChannel.ADMIN) {
            return adminRequestAuthenticator.authenticate(permissionCode);
        }
        return clientRequestAuthenticator.authenticate();
    }

}
