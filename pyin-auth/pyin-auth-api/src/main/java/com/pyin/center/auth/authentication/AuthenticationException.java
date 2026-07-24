package com.pyin.center.auth.authentication;

/**
 * 网关认证异常。
 *
 * <p>该异常用于 auth 模块向网关调用方表达认证态无效、登录已过期或当前请求无法被识别等
 * 认证失败场景。调用方应将该异常转换为统一的认证失败响应。</p>
 */
public class AuthenticationException extends RuntimeException {

    /**
     * 创建一个只有错误消息的网关认证异常。
     *
     * @param message 认证失败原因描述。
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * 创建一个包含底层原因的网关认证异常。
     *
     * @param message 认证失败原因描述。
     * @param cause 触发认证失败的底层异常。
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
