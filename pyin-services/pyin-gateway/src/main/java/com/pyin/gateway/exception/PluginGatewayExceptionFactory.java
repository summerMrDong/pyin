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

    public static PluginGatewayException forbidden(String message) {
        return new PluginGatewayException(403, "PYIN-AUTH-403", message);
    }

    public static PluginGatewayException apiNotPublished(String pluginId, String method, String path) {
        return new PluginGatewayException(
                404,
                "PYIN-PLUGIN-API-404",
                "插件接口未发布：" + pluginId + " " + method + " " + path
        );
    }

    public static PluginGatewayException badGateway(String pluginId, String message) {
        return new PluginGatewayException(502, "PYIN-PLUGIN-GATEWAY-502", "插件转发失败：" + pluginId + "，" + message);
    }

    public static PluginGatewayException gatewayTimeout(String pluginId, String message) {
        return new PluginGatewayException(504, "PYIN-PLUGIN-GATEWAY-504", "插件转发超时：" + pluginId + "，" + message);
    }

    private static String unavailableMessage(String pluginId, PluginRuntimeStatus status) {
        if (status == PluginRuntimeStatus.STOPPED) {
            return "插件已停用，暂时无法访问：" + pluginId;
        }
        return "插件当前不可用，请稍后重试：" + pluginId;
    }
}
