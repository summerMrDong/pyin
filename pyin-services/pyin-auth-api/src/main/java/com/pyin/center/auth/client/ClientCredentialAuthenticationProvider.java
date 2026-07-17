package com.pyin.center.auth.client;

public interface ClientCredentialAuthenticationProvider {

    ClientCredentialIdentity findByAccessKey(String accessKey);

    ClientCredentialIdentity findById(Long id);

    String decryptSecret(ClientCredentialIdentity credential);
}
