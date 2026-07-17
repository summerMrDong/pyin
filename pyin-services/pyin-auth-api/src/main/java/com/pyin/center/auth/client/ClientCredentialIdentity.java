package com.pyin.center.auth.client;

public record ClientCredentialIdentity(
        Long id,
        String accessKey,
        String accessSecretHash,
        boolean enabled
) {
}
