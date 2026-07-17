package com.pyin.gateway.signature;

import com.pyin.plugin.common.constant.PyinHeaders;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GatewaySignatureService {

    public Map<String, String> buildForwardHeaders(String pluginId, String requestSource, byte[] body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PyinHeaders.CENTER_ID, "pyin-center");
        headers.put(PyinHeaders.PLUGIN_ID, pluginId);
        headers.put(PyinHeaders.REQUEST_SOURCE, requestSource);
        headers.put(PyinHeaders.REQUEST_ID, UUID.randomUUID().toString());
        headers.put(PyinHeaders.TIMESTAMP, String.valueOf(Instant.now().toEpochMilli()));
        headers.put(PyinHeaders.NONCE, UUID.randomUUID().toString());
        headers.put(PyinHeaders.BODY_SHA256, sha256(body));
        headers.put(PyinHeaders.SIGNATURE, "pyin-gateway-signature");
        return headers;
    }

    private String sha256(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body == null ? new byte[0] : body);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            return Base64.getEncoder().encodeToString(new byte[0]);
        }
    }
}
