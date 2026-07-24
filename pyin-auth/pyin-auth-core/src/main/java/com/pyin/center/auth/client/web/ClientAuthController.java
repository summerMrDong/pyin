package com.pyin.center.auth.client.web;

import com.pyin.center.auth.client.service.ClientAccessTokenService;
import com.pyin.center.auth.client.service.ClientCredentialNonceService;
import com.pyin.center.auth.client.service.ClientCredentialSignatureService;
import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.constant.PyinHeaders;
import com.pyin.plugin.system.api.model.SystemClientCredentialIdentity;
import com.pyin.plugin.system.api.service.SystemClientCredentialPublicService;
import com.pyin.plugin.system.api.service.SystemClientRequestAuditPublicService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open/auth")
public class ClientAuthController {

    private static final long TIMESTAMP_TOLERANCE_MILLIS = 5 * 60 * 1000L;

    private final SystemClientCredentialPublicService systemClientCredentialPublicService;
    private final ClientCredentialSignatureService clientCredentialSignatureService;
    private final ClientCredentialNonceService clientCredentialNonceService;
    private final SystemClientRequestAuditPublicService systemClientRequestAuditPublicService;
    private final PasswordEncoder passwordEncoder;
    private final ClientAccessTokenService clientAccessTokenService;

    public ClientAuthController(
            SystemClientCredentialPublicService systemClientCredentialPublicService,
            ClientCredentialSignatureService clientCredentialSignatureService,
            ClientCredentialNonceService clientCredentialNonceService,
            SystemClientRequestAuditPublicService systemClientRequestAuditPublicService,
            PasswordEncoder passwordEncoder,
            ClientAccessTokenService clientAccessTokenService
    ) {
        this.systemClientCredentialPublicService = systemClientCredentialPublicService;
        this.clientCredentialSignatureService = clientCredentialSignatureService;
        this.clientCredentialNonceService = clientCredentialNonceService;
        this.systemClientRequestAuditPublicService = systemClientRequestAuditPublicService;
        this.passwordEncoder = passwordEncoder;
        this.clientAccessTokenService = clientAccessTokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<Result<Map<String, Object>>> token(HttpServletRequest request) {
        String accessKey = request.getHeader(PyinHeaders.ACCESS_KEY);
        SystemClientCredentialIdentity credential = systemClientCredentialPublicService.findByAccessKey(accessKey);
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

        String rawSecret = systemClientCredentialPublicService.decryptSecret(credential);
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
                || !passwordEncoder.matches(rawSecret, credential.accessSecretHash())) {
            logFailed(credential.id(), accessKey, request, "PYIN-CLIENT-AUTH-401", "签名无效");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.fail("PYIN-CLIENT-AUTH-401", "签名无效"));
        }

        String token = clientAccessTokenService.issueToken(credential);
        systemClientRequestAuditPublicService.log(
                credential.id(),
                credential.accessKey(),
                SystemClientRequestAuditPublicService.TYPE_AUTH_TOKEN,
                request.getRequestURI(),
                request.getMethod(),
                clientIp(request),
                SystemClientRequestAuditPublicService.STATUS_SUCCESS,
                null,
                null
        );
        return ResponseEntity.ok(Result.ok(Map.of(
                "token", token,
                "tokenType", "Bearer",
                "credentialId", credential.id()
        )));
    }

    private void logFailed(Long credentialId, String accessKey, HttpServletRequest request, String code, String message) {
        systemClientRequestAuditPublicService.log(
                credentialId,
                accessKey,
                SystemClientRequestAuditPublicService.TYPE_AUTH_TOKEN,
                request.getRequestURI(),
                request.getMethod(),
                clientIp(request),
                SystemClientRequestAuditPublicService.STATUS_FAILED,
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

}
