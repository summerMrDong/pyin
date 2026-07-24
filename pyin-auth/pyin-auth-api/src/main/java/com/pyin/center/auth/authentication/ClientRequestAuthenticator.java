package com.pyin.center.auth.authentication;

/**
 * 网关侧 C 端请求认证契约。
 *
 * <p>该接口供插件网关在处理 C 端 SDK 插件接口前调用。P1 阶段该服务只确认当前 C 端 token
 * 对应的接入凭证存在且启用，并返回可传递给审计和插件的主体上下文；它不做 plugin、scope、
 * namespace 等细粒度授权，也不负责插件路由匹配或请求转发。</p>
 */
public interface ClientRequestAuthenticator {

    /**
     * 校验当前线程绑定的 C 端 SDK 请求是否已通过接入凭证认证。
     *
     * @return 已认证的 C 端接入凭证主体。
     * @throws AuthenticationException 当前请求未登录、token 无效、凭证不存在或凭证被禁用时抛出。
     */
    AuthenticatedPrincipal authenticate();
}
