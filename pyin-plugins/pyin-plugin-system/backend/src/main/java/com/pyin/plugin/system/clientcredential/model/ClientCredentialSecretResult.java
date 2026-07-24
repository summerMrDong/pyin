package com.pyin.plugin.system.clientcredential.model;

public record ClientCredentialSecretResult(
        Long id,
        String credentialName,
        String accessKey,
        String accessSecret,
        String status
) {
}
