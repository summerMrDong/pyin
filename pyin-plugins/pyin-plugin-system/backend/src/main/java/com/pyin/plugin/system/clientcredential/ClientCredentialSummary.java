package com.pyin.plugin.system.clientcredential;

import java.time.LocalDateTime;

public record ClientCredentialSummary(
        Long id,
        String credentialName,
        String accessKey,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
