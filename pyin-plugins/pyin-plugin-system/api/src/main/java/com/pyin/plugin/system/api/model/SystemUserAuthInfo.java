package com.pyin.plugin.system.api.model;

/**
 * 后台用户认证信息视图。
 *
 * <p>该模型用于跨模块认证校验，包含密码哈希等敏感字段，只应在服务端内存中使用，不应
 * 直接返回给前端或写入普通业务日志。</p>
 *
 * @param id 用户主键 ID。
 * @param username 用户登录账号，通常全局唯一。
 * @param displayName 用户展示名称，可为空。
 * @param passwordHash 用户密码哈希，供认证模块校验密码使用。
 * @param enabled 用户是否启用；为 {@code false} 时调用方应拒绝登录或会话继续使用。
 */
public record SystemUserAuthInfo(
        Long id,
        String username,
        String displayName,
        String passwordHash,
        boolean enabled
) {
}
