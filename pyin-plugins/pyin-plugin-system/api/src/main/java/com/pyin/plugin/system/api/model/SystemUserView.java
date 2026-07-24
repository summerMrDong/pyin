package com.pyin.plugin.system.api.model;

/**
 * 后台用户基础展示视图。
 *
 * <p>该模型用于当前用户信息、审计上下文、权限上下文等非密码校验场景，不包含密码哈希等
 * 认证敏感字段。</p>
 *
 * @param id 用户主键 ID。
 * @param username 用户登录账号，通常全局唯一。
 * @param displayName 用户展示名称，可为空。
 * @param enabled 用户是否启用；为 {@code false} 时调用方应视为不可继续访问后台能力。
 */
public record SystemUserView(
        Long id,
        String username,
        String displayName,
        boolean enabled
) {
}
