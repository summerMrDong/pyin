package com.pyin.center.auth.authentication;

/**
 * 网关侧后台管理请求鉴权契约。
 *
 * <p>插件网关或其他入口在处理后台管理请求前调用该接口，用于确认当前请求已经完成后台
 * 用户登录校验，并按插件 API 发布的权限编码完成授权。该契约不负责插件存在性判断、插件 API
 * 匹配或请求转发。</p>
 */
public interface AdminRequestAuthenticator {

    /**
     * 校验当前线程绑定的后台管理请求是否已登录。
     *
     * <p>校验失败时实现方应抛出 {@link AuthenticationException} 或其他运行时认证异常，
     * 由调用入口统一转换为 401/403 响应。校验成功时方法正常返回。</p>
     */
    void checkLogin();

    /**
     * 校验当前后台管理请求的登录态和权限。
     *
     * <p>当 {@code permissionCode} 为空时，实现方只需要校验登录态；当权限编码非空时，实现方
     * 必须同时完成授权校验。校验成功后返回后台用户主体，供网关写入审计上下文和独立插件转发头。</p>
     *
     * @param permissionCode 插件 API 发布的权限编码；为空表示只要求后台登录态。
     * @return 已认证且已授权的后台用户主体。
     * @throws AuthenticationException 未登录或用户不可用时抛出；权限不足时抛出
     * {@link AuthorizationException}。
     */
    default AuthenticatedPrincipal authenticate(String permissionCode) {
        checkLogin();
        return AuthenticatedPrincipal.adminUser(null, null);
    }
}
