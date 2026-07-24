package com.pyin.gateway.signature;

import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.gateway.path.PluginGatewayPathSupport.PluginGatewayPath;
import com.pyin.plugin.common.constant.PyinHeaders;
import com.pyin.plugin.runtime.route.PluginApiRoute;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GatewaySignatureService {

    public Map<String, String> buildForwardHeaders(
            PluginApiRoute route,
            PluginGatewayPath path,
            AuthenticatedPrincipal principal,
            String forwardMethod,
            String forwardPath,
            byte[] body
    ) {
        String requestSource = path.channel() == com.pyin.plugin.runtime.route.PluginApiChannel.ADMIN
                ? "ADMIN_GATEWAY"
                : "CLIENT_SDK_GATEWAY";
        return buildForwardHeaders(route.pluginId(), requestSource, route.permissionCode(), principal, forwardMethod, forwardPath, body);
    }

    private Map<String, String> buildForwardHeaders(
            String pluginId,
            String requestSource,
            String permissionCode,
            AuthenticatedPrincipal principal,
            String forwardMethod,
            String forwardPath,
            byte[] body
    ) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PyinHeaders.CENTER_ID, "pyin-center");
        headers.put(PyinHeaders.PLUGIN_ID, pluginId);
        headers.put(PyinHeaders.REQUEST_SOURCE, requestSource);
        headers.put(PyinHeaders.REQUEST_ID, UUID.randomUUID().toString());
        headers.put(PyinHeaders.TIMESTAMP, String.valueOf(Instant.now().toEpochMilli()));
        headers.put(PyinHeaders.NONCE, UUID.randomUUID().toString());
        headers.put(PyinHeaders.BODY_SHA256, sha256(body));
        headers.put(PyinHeaders.SIGNATURE, "pyin-gateway-signature");
        if (forwardMethod != null) {
            headers.put(PyinHeaders.FORWARD_METHOD, forwardMethod);
        }
        if (forwardPath != null) {
            headers.put(PyinHeaders.FORWARD_PATH, forwardPath);
        }
        if (principal != null) {
            headers.put(PyinHeaders.PRINCIPAL_TYPE, principal.principalType());
            headers.put(PyinHeaders.PRINCIPAL_ID, principal.principalId());
            headers.put(PyinHeaders.PRINCIPAL_NAME, principal.displayName() == null ? "" : principal.displayName());
        }
        headers.put(PyinHeaders.PERMISSION_CODE, permissionCode == null ? "" : permissionCode);
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
