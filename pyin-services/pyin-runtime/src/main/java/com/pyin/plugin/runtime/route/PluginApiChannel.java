package com.pyin.plugin.runtime.route;

import com.pyin.plugin.spi.model.PluginAccessMode;

/**
 * 插件网关访问通道。
 *
 * <p>该枚举用于运行时路由解析阶段把外部入口路径归一为明确的访问语义。运行时只根据通道
 * 选择可匹配的 {@link PluginAccessMode}，不负责用户认证、权限授权或请求转发。</p>
 */
public enum PluginApiChannel {

    /** 后台管理端插件接口通道。 */
    ADMIN(PluginAccessMode.CENTER_ADMIN_ONLY),

    /** C 端 SDK 插件接口通道。 */
    CLIENT(PluginAccessMode.CLIENT_SDK_GATEWAY);

    private final PluginAccessMode accessMode;

    PluginApiChannel(PluginAccessMode accessMode) {
        this.accessMode = accessMode;
    }

    /**
     * 返回该通道允许匹配的插件 API 访问模式。
     *
     * @return 插件 API 访问模式，不会返回 {@code null}。
     */
    public PluginAccessMode accessMode() {
        return accessMode;
    }
}
