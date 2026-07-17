package com.pyin.center.auth.client;

import cn.dev33.satoken.stp.StpUtil;
import com.pyin.center.auth.crypto.PasswordHasher;
import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.constant.PyinHeaders;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/capi/auth")
public class ClientAuthController {

    private static final long TIMESTAMP_TOLERANCE_MILLIS = 5 * 60 * 1000L;

    private final ClientCredentialAuthenticationProvider clientCredentialAuthenticationProvider;
    private final ClientCredentialSignatureService clientCredentialSignatureService;
    private final ClientCredentialNonceService clientCredentialNonceService;
    private final ClientRequestAuditService clientRequestAuditService;
    private final PasswordHasher passwordHasher;

    public ClientAuthController(
            ClientCredentialAuthenticationProvider clientCredentialAuthenticationProvider,
            ClientCredentialSignatureService clientCredentialSignatureService,
            ClientCredentialNonceService clientCredentialNonceService,
            ClientRequestAuditService clientRequestAuditService,
            PasswordHasher passwordHasher
    ) {
        this.clientCredentialAuthenticationProvider = clientCredentialAuthenticationProvider;
        this.clientCredentialSignatureService = clientCredentialSignatureService;
        this.clientCredentialNonceService = clientCredentialNonceService;
        this.clientRequestAuditService = clientRequestAuditService;
        this.passwordHasher = passwordHasher;
    }

    @PostMapping("/token")
    public ResponseEntity<Result<Map<String, Object>>> token(HttpServletRequest request) {
        String accessKey = request.getHeader(PyinHeaders.ACCESS_KEY);
        ClientCredentialIdentity credential = clientCredentialAuthenticationProvider.findByAccessKey(accessKey);
        if (credential == null) {
            logFailed(null, accessKey, request, "PYIN-CLIENT-AUTH-401", "接入凭证不存在");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.fail("PYIN-CLIENT-AUTH-401", "接入凭证不存在"));
        }
        if (!credential.enabled()) {
            logFailed(credential.id(), accessKey, request, "PYIN-CLIENT-AUTH-403", "接入凭证已停用");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Result.fail("PYIN-CLIENT-AUTH-403", "接入凭证已停用"));
        }

        String timestamp = request.getHeader(PyinHeaders.TIMESTAMP);
        long parsedTimestamp;
        try {
            parsedTimestamp = Long.parseLong(timestamp);
        } catch (Exception exception) {
            logFailed(credential.id(), accessKey, request, "PYIN-CLIENT-AUTH-400", "时间戳无效");
            return ResponseEntity.badRequest()
                    .body(Result.fail("PYIN-CLIENT-AUTH-400", "时间戳无效"));
        }
        long now = Instant.now().toEpochMilli();
        if (Math.abs(now - parsedTimestamp) > TIMESTAMP_TOLERANCE_MILLIS) {
            logFailed(credential.id(), accessKey, request, "PYIN-CLIENT-AUTH-408", "时间戳超出允许窗口");
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(Result.fail("PYIN-CLIENT-AUTH-408", "时间戳超出允许窗口"));
        }

        String nonce = request.getHeader(PyinHeaders.NONCE);
        if (!clientCredentialNonceService.register(accessKey, nonce, parsedTimestamp)) {
            logFailed(credential.id(), accessKey, request, "PYIN-CLIENT-AUTH-409", "Nonce 重复或无效");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.fail("PYIN-CLIENT-AUTH-409", "Nonce 重复或无效"));
        }

        byte[] body = clientCredentialSignatureService.readRequestBody(request);
        String expectedBodySha = clientCredentialSignatureService.bodySha256(body);
        String bodySha = request.getHeader(PyinHeaders.BODY_SHA256);
        if (!expectedBodySha.equals(bodySha)) {
            logFailed(credential.id(), accessKey, request, "PYIN-CLIENT-AUTH-400", "请求体摘要不匹配");
            return ResponseEntity.badRequest()
                    .body(Result.fail("PYIN-CLIENT-AUTH-400", "请求体摘要不匹配"));
        }

        String rawSecret = clientCredentialAuthenticationProvider.decryptSecret(credential);
        String signature = request.getHeader(PyinHeaders.SIGNATURE);
        String expectedSignature = clientCredentialSignatureService.buildSignature(
                request.getMethod(),
                request.getRequestURI(),
                accessKey,
                timestamp,
                nonce,
                bodySha,
                rawSecret
        );
        if (!expectedSignature.equals(signature)
                || !passwordHasher.matches(rawSecret, credential.accessSecretHash())) {
            logFailed(credential.id(), accessKey, request, "PYIN-CLIENT-AUTH-401", "签名无效");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.fail("PYIN-CLIENT-AUTH-401", "签名无效"));
        }

        StpUtil.login("client:" + credential.id());
        clientRequestAuditService.log(
                credential.id(),
                credential.accessKey(),
                ClientRequestAuditService.TYPE_AUTH_TOKEN,
                request.getRequestURI(),
                request.getMethod(),
                clientIp(request),
                ClientRequestAuditService.STATUS_SUCCESS,
                null,
                null
        );
        return ResponseEntity.ok(Result.ok(Map.of(
                "token", StpUtil.getTokenValue(),
                "tokenType", "Bearer",
                "credentialId", credential.id()
        )));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Result<Map<String, Object>>> refresh(HttpServletRequest request) {
        try {
            StpUtil.checkLogin();
        } catch (Exception exception) {
            clientRequestAuditService.log(
                    null,
                    null,
                    ClientRequestAuditService.TYPE_AUTH_REFRESH,
                    request.getRequestURI(),
                    request.getMethod(),
                    clientIp(request),
                    ClientRequestAuditService.STATUS_FAILED,
                    "PYIN-CLIENT-AUTH-401",
                    "未登录"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.fail("PYIN-CLIENT-AUTH-401", "未登录"));
        }

        Long credentialId = parseClientCredentialId(StpUtil.getLoginId());
        ClientCredentialIdentity credential = clientCredentialAuthenticationProvider.findById(credentialId);
        if (credential == null || !credential.enabled()) {
            clientRequestAuditService.log(
                    credentialId,
                    credential == null ? null : credential.accessKey(),
                    ClientRequestAuditService.TYPE_AUTH_REFRESH,
                    request.getRequestURI(),
                    request.getMethod(),
                    clientIp(request),
                    ClientRequestAuditService.STATUS_FAILED,
                    "PYIN-CLIENT-AUTH-403",
                    "接入凭证不可用"
            );
            StpUtil.logout();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Result.fail("PYIN-CLIENT-AUTH-403", "接入凭证不可用"));
        }
        clientRequestAuditService.log(
                credential.id(),
                credential.accessKey(),
                ClientRequestAuditService.TYPE_AUTH_REFRESH,
                request.getRequestURI(),
                request.getMethod(),
                clientIp(request),
                ClientRequestAuditService.STATUS_SUCCESS,
                null,
                null
        );
        return ResponseEntity.ok(Result.ok(Map.of("token", StpUtil.getTokenValue(), "refreshed", true)));
    }

    private void logFailed(Long credentialId, String accessKey, HttpServletRequest request, String code, String message) {
        clientRequestAuditService.log(
                credentialId,
                accessKey,
                ClientRequestAuditService.TYPE_AUTH_TOKEN,
                request.getRequestURI(),
                request.getMethod(),
                clientIp(request),
                ClientRequestAuditService.STATUS_FAILED,
                code,
                message
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Long parseClientCredentialId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        String value = String.valueOf(loginId);
        if (!value.startsWith("client:")) {
            return null;
        }
        try {
            return Long.parseLong(value.substring("client:".length()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
