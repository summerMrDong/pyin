package com.pyin.gateway.response;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.runtime.state.PluginRuntimeStatus;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class
PluginGatewayErrorResponseSupport {

    private PluginGatewayErrorResponseSupport() {
    }

    public static ResponseEntity<byte[]> pluginNotFound(String pluginId) {
        return resultResponse(404, Result.fail("PYIN-PLUGIN-404", "插件不存在：" + pluginId));
    }

    public static ResponseEntity<byte[]> pluginUnavailable(String pluginId, PluginRuntimeStatus status) {
        return resultResponse(503, Result.fail("PYIN-PLUGIN-503", unavailableMessage(pluginId, status)));
    }

    public static ResponseEntity<byte[]> resultResponse(int statusCode, Result<?> result) {
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(result).getBytes(StandardCharsets.UTF_8));
    }

    public static void writeResponse(HttpServletResponse response, ResponseEntity<byte[]> entity) throws IOException {
        response.setStatus(entity.getStatusCode().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (entity.getHeaders().getContentType() != null) {
            response.setContentType(entity.getHeaders().getContentType().toString());
        }
        entity.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        if (entity.getBody() != null) {
            response.getOutputStream().write(entity.getBody());
        }
    }

    private static String unavailableMessage(String pluginId, PluginRuntimeStatus status) {
        if (status == PluginRuntimeStatus.STOPPED) {
            return "插件已停用，暂时无法访问：" + pluginId;
        }
        return "插件当前不可用，请稍后重试：" + pluginId;
    }

    private static String toJson(Result<?> result) {
        return "{\"success\":" + result.isSuccess()
                + ",\"code\":\"" + result.getCode()
                + "\",\"message\":\"" + result.getMessage()
                + "\"}";
    }
}
