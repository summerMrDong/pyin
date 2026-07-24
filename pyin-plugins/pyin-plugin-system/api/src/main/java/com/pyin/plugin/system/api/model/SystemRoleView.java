package com.pyin.plugin.system.api.model;

/**
 * 后台角色基础视图。
 *
 * <p>该模型用于跨模块展示和鉴权上下文，只包含角色的稳定标识和展示信息，不暴露角色内部
 * 权限绑定明细。</p>
 *
 * @param id 角色主键 ID。
 * @param code 角色编码，通常用于程序侧识别和排序。
 * @param name 角色名称，通常用于前端展示。
 */
public record SystemRoleView(
        Long id,
        String code,
        String name
) {
}
