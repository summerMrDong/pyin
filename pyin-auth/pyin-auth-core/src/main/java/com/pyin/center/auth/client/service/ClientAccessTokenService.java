package com.pyin.center.auth.client.service;

import com.pyin.center.auth.authentication.AuthenticationException;
import com.pyin.plugin.system.api.model.SystemClientCredentialIdentity;
import com.pyin.plugin.system.api.service.SystemClientCredentialPublicService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * C端无状态访问票据服务。
 *
 * <p>票据自身包含凭证 ID、Access Key、签发时间和过期时间，并使用当前接入凭证 Secret
 * 签名。中心侧不保存 token 会话状态，因此服务重启不会导致已签发且未过期的 C端 token
 * 失效。</p>
 */
@Service
public class ClientAccessTokenService {

    private static final String TOKEN_PREFIX = "pyin_client_v1";
    private static final String PAYLOAD_VERSION = "v1";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final SystemClientCredentialPublicService systemClientCredentialPublicService;
    private final long tokenTimeoutSeconds;

    public ClientAccessTokenService(
            SystemClientCredentialPublicService systemClientCredentialPublicService,
            @Value("${pyin.center.client-auth.token-timeout:${sa-token.timeout:2592000}}") long tokenTimeoutSeconds
    ) {
        this.systemClientCredentialPublicService = systemClientCredentialPublicService;
        this.tokenTimeoutSeconds = tokenTimeoutSeconds;
    }

    public String issueToken(SystemClientCredentialIdentity credential) {
        if (credential == null || credential.id() == null || !StringUtils.hasText(credential.accessKey())) {
            throw new AuthenticationException("C端接入凭证无效");
        }
        String rawSecret = systemClientCredentialPublicService.decryptSecret(credential);
        if (!StringUtils.hasText(rawSecret)) {
            throw new AuthenticationException("C端接入凭证 Secret 不可用");
        }

        long issuedAt = Instant.now().toEpochMilli();
        long expiresAt = expiresAt(issuedAt);
        String payload = String.join("\n",
                PAYLOAD_VERSION,
                String.valueOf(credential.id()),
                credential.accessKey(),
                String.valueOf(issuedAt),
                String.valueOf(expiresAt),
                UUID.randomUUID().toString()
        );
        return TOKEN_PREFIX + "." + encode(payload) + "." + sign(payload, rawSecret);
    }

    public ClientAccessToken authenticate(String authorization) {
        String token = normalizeAuthorization(authorization);
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationException("C端请求未认证或 token 为空");
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !TOKEN_PREFIX.equals(parts[0])) {
            throw new AuthenticationException("C端 token 格式无效");
        }

        String payload = decode(parts[1]);
        TokenPayload tokenPayload = parsePayload(payload);
        if (tokenPayload.expiresAt() < Instant.now().toEpochMilli()) {
            throw new AuthenticationException("C端 token 已过期");
        }

        SystemClientCredentialIdentity credential = systemClientCredentialPublicService.findById(tokenPayload.credentialId());
        if (credential == null || !credential.enabled()) {
            throw new ClientCredentialUnavailableException("C端接入凭证不存在或已停用", credential);
        }
        if (!tokenPayload.accessKey().equals(credential.accessKey())) {
            throw new AuthenticationException("C端 token 与接入凭证不匹配");
        }

        String rawSecret = systemClientCredentialPublicService.decryptSecret(credential);
        if (!StringUtils.hasText(rawSecret) || !constantTimeEquals(parts[2], sign(payload, rawSecret))) {
            throw new AuthenticationException("C端 token 签名无效");
        }
        return new ClientAccessToken(credential, tokenPayload.issuedAt(), tokenPayload.expiresAt());
    }

    private long expiresAt(long issuedAt) {
        if (tokenTimeoutSeconds < 0) {
            return Long.MAX_VALUE;
        }
        long timeoutMillis = tokenTimeoutSeconds * 1000L;
        if (Long.MAX_VALUE - issuedAt < timeoutMillis) {
            return Long.MAX_VALUE;
        }
        return issuedAt + timeoutMillis;
    }

    private String normalizeAuthorization(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return "";
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return value.substring("Bearer ".length()).trim();
        }
        return value;
    }

    private TokenPayload parsePayload(String payload) {
        String[] fields = payload.split("\n", -1);
        if (fields.length != 6 || !PAYLOAD_VERSION.equals(fields[0])) {
            throw new AuthenticationException("C端 token 载荷无效");
        }
        try {
            return new TokenPayload(
                    Long.parseLong(fields[1]),
                    fields[2],
                    Long.parseLong(fields[3]),
                    Long.parseLong(fields[4])
            );
        } catch (NumberFormatException exception) {
            throw new AuthenticationException("C端 token 时间或凭证 ID 无效", exception);
        }
    }

    private String encode(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        try {
            return new String(URL_DECODER.decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationException("C端 token 编码无效", exception);
        }
    }

    private String sign(String payload, String rawSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(rawSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign C端 token", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8),
                right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record ClientAccessToken(
            SystemClientCredentialIdentity credential,
            long issuedAt,
            long expiresAt
    ) {
    }

    private record TokenPayload(
            Long credentialId,
            String accessKey,
            long issuedAt,
            long expiresAt
    ) {
    }

    public static class ClientCredentialUnavailableException extends AuthenticationException {

        private final SystemClientCredentialIdentity credential;

        public ClientCredentialUnavailableException(String message, SystemClientCredentialIdentity credential) {
            super(message);
            this.credential = credential;
        }

        public SystemClientCredentialIdentity credential() {
            return credential;
        }
    }
}
