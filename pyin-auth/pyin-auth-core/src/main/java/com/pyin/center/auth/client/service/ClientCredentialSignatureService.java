package com.pyin.center.auth.client.service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class ClientCredentialSignatureService {

    public String buildSignature(
            String httpMethod,
            String requestUri,
            String accessKey,
            String timestamp,
            String nonce,
            String bodySha256,
            String accessSecret
    ) {
        String canonical = String.join("\n",
                defaultValue(httpMethod).toUpperCase(),
                defaultValue(requestUri),
                defaultValue(accessKey),
                defaultValue(timestamp),
                defaultValue(nonce),
                defaultValue(bodySha256));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(accessSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign client credential request", exception);
        }
    }

    public String bodySha256(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body == null ? new byte[0] : body);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash request body", exception);
        }
    }

    public byte[] readRequestBody(HttpServletRequest request) {
        try (InputStream inputStream = request.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read request body", exception);
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
