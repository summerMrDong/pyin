package com.pyin.plugin.sdk.security;

import com.pyin.plugin.common.constant.PyinHeaders;
import com.pyin.plugin.spi.model.PluginAccessMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

public class PluginRequestSignatureVerifier {

    private final Duration allowedClockSkew = Duration.ofMinutes(5);

    public boolean verify(String pluginId, PluginAccessMode accessMode, HttpHeaders headers, String body) {
        return verify(pluginId, accessMode, null, null, headers, body);
    }

    public boolean verify(
            String pluginId,
            PluginAccessMode accessMode,
            String method,
            String path,
            HttpHeaders headers,
            String body
    ) {
        if (!pluginId.equals(headers.getFirst(PyinHeaders.PLUGIN_ID))) {
            return false;
        }
        if (!isForwardTargetValid(method, headers.getFirst(PyinHeaders.FORWARD_METHOD))) {
            return false;
        }
        if (!isForwardTargetValid(path, headers.getFirst(PyinHeaders.FORWARD_PATH))) {
            return false;
        }
        if (!isTimestampValid(headers.getFirst(PyinHeaders.TIMESTAMP))) {
            return false;
        }
        if (!isBodyHashValid(headers.getFirst(PyinHeaders.BODY_SHA256), body)) {
            return false;
        }
        return isAccessModeAllowed(accessMode, headers.getFirst(PyinHeaders.REQUEST_SOURCE))
                && StringUtils.hasText(headers.getFirst(PyinHeaders.NONCE))
                && StringUtils.hasText(headers.getFirst(PyinHeaders.SIGNATURE));
    }

    private boolean isForwardTargetValid(String expected, String actual) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return expected.equals(actual);
    }

    private boolean isTimestampValid(String timestampValue) {
        if (!StringUtils.hasText(timestampValue)) {
            return false;
        }
        long timestamp = Long.parseLong(timestampValue);
        long delta = Math.abs(Instant.now().toEpochMilli() - timestamp);
        return delta <= allowedClockSkew.toMillis();
    }

    private boolean isBodyHashValid(String providedHash, String body) {
        if (!StringUtils.hasText(providedHash)) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
            return providedHash.equals(Base64.getEncoder().encodeToString(hash));
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isAccessModeAllowed(PluginAccessMode accessMode, String requestSource) {
        return switch (accessMode) {
            case CENTER_ADMIN_ONLY -> Set.of("ADMIN_GATEWAY").contains(requestSource);
            case CLIENT_SDK_GATEWAY -> Set.of("CLIENT_SDK_GATEWAY").contains(requestSource);
            case INTERNAL_ONLY -> Set.of("INTERNAL").contains(requestSource);
            case CLIENT_SDK_DIRECT -> false;
        };
    }
}
