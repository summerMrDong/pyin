package com.pyin.gateway.support;

import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.plugin.runtime.route.PluginApiRoute;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 插件网关内部请求上下文工具。
 *
 * <p>该类集中定义 gateway 与内嵌插件 Controller 之间通过 request attribute 传递的上下文键。
 * 外部请求不能伪造这些 attribute；只有统一插件网关完成路由和鉴权后才会写入。</p>
 */
public final class PluginGatewayInternalSupport {

    /** 标记当前请求已经由中心插件网关完成路由和鉴权，可进入内嵌插件 Controller。 */
    public static final String INTERNAL_REQUEST_ATTRIBUTE =
            PluginGatewayInternalSupport.class.getName() + ".INTERNAL_REQUEST";

    /** 当前请求命中的插件 API 路由。 */
    public static final String ROUTE_ATTRIBUTE =
            PluginGatewayInternalSupport.class.getName() + ".ROUTE";

    /** 当前请求已认证主体。 */
    public static final String PRINCIPAL_ATTRIBUTE =
            PluginGatewayInternalSupport.class.getName() + ".PRINCIPAL";

    private PluginGatewayInternalSupport() {
    }

    /**
     * 写入网关已放行标记和上下文。
     *
     * @param request 当前 HTTP 请求。
     * @param route 命中的插件 API 路由。
     * @param principal 已认证且已授权的请求主体。
     */
    public static void markAllowed(HttpServletRequest request, PluginApiRoute route, AuthenticatedPrincipal principal) {
        request.setAttribute(INTERNAL_REQUEST_ATTRIBUTE, Boolean.TRUE);
        request.setAttribute(ROUTE_ATTRIBUTE, route);
        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
    }

    /**
     * 判断当前请求是否已经由插件网关放行。
     *
     * @param request 当前 HTTP 请求。
     * @return 已放行时返回 {@code true}。
     */
    public static boolean isAllowed(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(INTERNAL_REQUEST_ATTRIBUTE));
    }
}
