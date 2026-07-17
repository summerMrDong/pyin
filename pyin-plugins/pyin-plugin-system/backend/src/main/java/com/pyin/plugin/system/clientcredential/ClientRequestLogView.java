package com.pyin.plugin.system.clientcredential;

import java.time.LocalDateTime;

public record ClientRequestLogView(
        Long id,
        Long credentialId,
        String credentialName,
        String accessKey,
        String requestType,
        String requestUri,
        String httpMethod,
        String clientIp,
        String requestStatus,
        String failureCode,
        String failureMessage,
        LocalDateTime createdAt
) {
}
