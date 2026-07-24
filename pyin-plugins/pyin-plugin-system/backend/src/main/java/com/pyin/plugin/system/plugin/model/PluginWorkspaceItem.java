package com.pyin.plugin.system.plugin.model;

/**
 * 主前端壳展示一个插件工作区所需的只读信息。
 *
 * <p>该模型不包含页面菜单或页面结构；页面路由完全由插件前端的 {@code ./routes} 模块定义。</p>
 *
 * @param pluginId 插件唯一标识，也是壳应用工作区入口 {@code /plugins/{pluginId}} 的路径参数
 * @param pluginName 插件显示名称，用于顶部工作区标签
 * @param status 插件当前运行状态；该接口仅返回已启动插件
 * @param sourceType 插件来源类型，用于平台诊断和展示
 * @param frontend 模块联邦远程加载信息
 */
public record PluginWorkspaceItem(
        String pluginId,
        String pluginName,
        String status,
        String sourceType,
        PluginWorkspaceFrontend frontend
) {
}
