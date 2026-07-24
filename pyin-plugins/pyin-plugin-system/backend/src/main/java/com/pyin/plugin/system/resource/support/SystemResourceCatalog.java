package com.pyin.plugin.system.resource.support;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SystemResourceCatalog {

    public List<SystemResourceDefinition> definitions() {
        return List.of(
                page("dashboard", "控制台", "/", "LayoutDashboard", 0, "system:view"),
                page("users", "用户管理", "/users", "Users", 10, "user:view"),
                button("users:create", "新建用户", "users", 11, "user:create"),
                button("users:update", "编辑用户", "users", 12, "user:update"),
                button("users:reset-password", "重置密码", "users", 13, "user:reset-password"),
                button("users:delete", "删除用户", "users", 14, "user:delete"),
                page("roles", "角色管理", "/roles", "ShieldCheck", 20, "role:view"),
                button("roles:create", "新建角色", "roles", 21, "role:create"),
                button("roles:update", "编辑角色", "roles", 22, "role:update"),
                button("roles:grant-resource", "授权资源", "roles", 23, "role:update"),
                button("roles:grant-permission", "授权权限", "roles", 24, "role:update"),
                button("roles:grant-user", "授权用户", "roles", 25, "role:update"),
                button("roles:delete", "删除角色", "roles", 26, "role:delete"),
                page("permissions", "权限管理", "/permissions", "KeyRound", 30, "system:view"),
                page("plugins", "插件管理", "/plugins", "Blocks", 40, "system:view"),
                page("credentials", "接入凭证", "/credentials", "BadgeCheck", 50, "system:view"),
                page("settings", "系统设置", "/settings", "Settings2", 60, "system:view")
        );
    }

    private SystemResourceDefinition page(String code, String name, String path, String icon, int sort, String permissionCode) {
        return new SystemResourceDefinition(code, name, "PAGE", null, path, icon, sort, permissionCode, true);
    }

    private SystemResourceDefinition button(String code, String name, String parentCode, int sort, String permissionCode) {
        return new SystemResourceDefinition(code, name, "BUTTON", parentCode, null, null, sort, permissionCode, true);
    }
}
