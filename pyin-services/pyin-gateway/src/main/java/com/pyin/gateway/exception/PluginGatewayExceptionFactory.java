package com.pyin.gateway.exception;

import com.pyin.plugin.runtime.state.PluginRuntimeStatus;

public final class PluginGatewayExceptionFactory {

    private PluginGatewayExceptionFactory() {
    }

    public static PluginGatewayException pluginNotFound(String pluginId) {
        return new PluginGatewayException(404, "PYIN-PLUGIN-404", "插件不存在：" + pluginId);
    }

    public static PluginGatewayException pluginUnavailable(String pluginId, PluginRuntimeStatus status) {
        return new PluginGatewayException(503, "PYIN-PLUGIN-503", unavailableMessage(pluginId, status));
    }

    public static PluginGatewayException unauthorized(String message) {
        return new PluginGatewayException(401, "PYIN-AUTH-401", message);
    }

    private static String unavailableMessage(String pluginId, PluginRuntimeStatus status) {
        if (status == PluginRuntimeStatus.STOPPED) {
            return "插件已停用，暂时无法访问：" + pluginId;
        }
        return "插件当前不可用，请稍后重试：" + pluginId;
    }
}
