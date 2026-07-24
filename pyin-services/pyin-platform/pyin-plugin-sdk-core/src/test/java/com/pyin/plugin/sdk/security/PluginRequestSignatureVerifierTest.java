package com.pyin.plugin.sdk.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pyin.plugin.common.constant.PyinHeaders;
import com.pyin.plugin.spi.model.PluginAccessMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class PluginRequestSignatureVerifierTest {

    private final PluginRequestSignatureVerifier verifier = new PluginRequestSignatureVerifier();

    @Test
    void shouldVerifyMethodAndPathWhenProvided() throws Exception {
        HttpHeaders headers = headers("dict", "ADMIN_GATEWAY", "POST", "/dict/admin/items", "{\"ok\":true}");

        boolean verified = verifier.verify(
                "dict",
                PluginAccessMode.CENTER_ADMIN_ONLY,
                "POST",
                "/dict/admin/items",
                headers,
                "{\"ok\":true}"
        );

        assertThat(verified).isTrue();
    }

    @Test
    void shouldRejectWhenMethodOrPathDoesNotMatch() throws Exception {
        HttpHeaders headers = headers("dict", "ADMIN_GATEWAY", "POST", "/dict/admin/items", "");

        assertThat(verifier.verify("dict", PluginAccessMode.CENTER_ADMIN_ONLY, "GET", "/dict/admin/items", headers, ""))
                .isFalse();
        assertThat(verifier.verify("dict", PluginAccessMode.CENTER_ADMIN_ONLY, "POST", "/dict/admin/other", headers, ""))
                .isFalse();
    }

    private HttpHeaders headers(String pluginId, String source, String method, String path, String body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(PyinHeaders.PLUGIN_ID, pluginId);
        headers.set(PyinHeaders.REQUEST_SOURCE, source);
        headers.set(PyinHeaders.TIMESTAMP, String.valueOf(Instant.now().toEpochMilli()));
        headers.set(PyinHeaders.NONCE, "nonce");
        headers.set(PyinHeaders.SIGNATURE, "signature");
        headers.set(PyinHeaders.FORWARD_METHOD, method);
        headers.set(PyinHeaders.FORWARD_PATH, path);
        headers.set(PyinHeaders.BODY_SHA256, sha256(body));
        return headers;
    }

    private String sha256(String body) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
