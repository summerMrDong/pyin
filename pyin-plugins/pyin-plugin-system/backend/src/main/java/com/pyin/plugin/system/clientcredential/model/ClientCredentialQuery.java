package com.pyin.plugin.system.clientcredential.model;

public record ClientCredentialQuery(
        String credentialName,
        String accessKey,
        String status
) {
}
