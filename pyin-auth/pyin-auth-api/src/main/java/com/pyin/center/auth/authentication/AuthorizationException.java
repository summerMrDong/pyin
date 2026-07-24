package com.pyin.center.auth.authentication;

/**
 * 网关授权异常。
 *
 * <p>该异常用于 auth 模块向 gateway 表达“请求主体已识别，但缺少访问目标资源所需权限”的失败
 * 场景。调用方应将其转换为 403 响应，而不是登录态失效的 401 响应。</p>
 */
public class AuthorizationException extends AuthenticationException {

    /**
     * 创建网关授权异常。
     *
     * @param message 授权失败原因描述。
     */
    public AuthorizationException(String message) {
        super(message);
    }

    /**
     * 创建包含底层原因的网关授权异常。
     *
     * @param message 授权失败原因描述。
     * @param cause 触发授权失败的底层异常。
     */
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
