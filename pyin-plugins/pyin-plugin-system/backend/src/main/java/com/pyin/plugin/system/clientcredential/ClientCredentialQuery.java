package com.pyin.plugin.system.clientcredential;

public record ClientCredentialQuery(
        String credentialName,
        String accessKey,
        String status
) {
}
