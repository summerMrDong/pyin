package com.pyin.plugin.runtime.route;

import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import com.pyin.plugin.runtime.state.PluginSourceType;
import com.pyin.plugin.spi.model.PluginAccessMode;

/**
 * 插件 API 路由解析结果。
 *
 * <p>该模型是 gateway 调 runtime 后获得的统一请求事实，包含插件状态、来源类型和命中的 API
 * 元数据。它不代表已经完成鉴权，也不表示请求已经转发或放行。</p>
 *
 * @param pluginId 插件 ID。
 * @param status 插件运行状态。
 * @param sourceType 当前注册来源类型。
 * @param accessMode 命中 API 的访问模式。
 * @param internalPath 内嵌插件真实 Controller 路径。
 * @param permissionCode 后台管理端权限编码；为空时只要求登录态。
 * @param auditEnabled 是否启用审计。
 * @param backendBaseUrl 独立插件后端基础地址；内嵌插件通常为空。
 * @param routePattern 命中的 API 路径模式。
 */
public record PluginApiRoute(
        String pluginId,
        PluginRuntimeStatus status,
        PluginSourceType sourceType,
        PluginAccessMode accessMode,
        String internalPath,
        String permissionCode,
        boolean auditEnabled,
        String backendBaseUrl,
        String routePattern
) {
}
