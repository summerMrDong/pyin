package com.pyin.plugin.system.api.model;

/**
 * 后台资源授权视图。
 *
 * <p>该模型描述用户可访问的系统资源或插件资源，适合构造菜单、按钮、页面等资源级访问
 * 上下文。资源的完整树结构仍由 system 插件内部维护。</p>
 *
 * @param resourceKey 资源唯一键，例如 {@code SYSTEM:users} 或 {@code PLUGIN:config/items}。
 * @param scope 资源归属范围，例如 {@code SYSTEM} 或 {@code PLUGIN}。
 * @param pluginId 插件资源所属插件 ID；系统资源时为 {@code null}。
 * @param resourceCode 资源在所属范围内的编码。
 * @param permissionCode 访问该资源关联的权限编码；未绑定权限时可为空。
 */
public record SystemResourceView(
        String resourceKey,
        String scope,
        String pluginId,
        String resourceCode,
        String permissionCode
) {
}
