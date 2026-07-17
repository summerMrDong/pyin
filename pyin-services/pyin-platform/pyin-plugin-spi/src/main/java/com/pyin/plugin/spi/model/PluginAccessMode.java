package com.pyin.plugin.spi.model;

/**
 * 插件接口访问模式。
 */
public enum PluginAccessMode {
    /** 仅允许中心管理端访问。 */
    CENTER_ADMIN_ONLY,
    /** 允许通过 C 端 SDK 网关访问。 */
    CLIENT_SDK_GATEWAY,
    /** 仅允许插件内部调用。 */
    INTERNAL_ONLY,
    /** 预留的客户端直连模式。 */
    CLIENT_SDK_DIRECT
}
