package com.pyin.plugin.runtime.state;

/**
 * 插件运行状态。
 */
public enum PluginRuntimeStatus {
    /** 插件当前可用并已启动。 */
    STARTED,
    /** 插件已停止，不对外提供服务。 */
    STOPPED,
    /** 插件当前不可用，例如心跳超时或主动下线。 */
    UNAVAILABLE
}
