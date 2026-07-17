package com.pyin.plugin.system.clientcredential;

import java.util.List;

public interface ClientCredentialService {

    String STATUS_ENABLED = "ENABLED";
    String STATUS_DISABLED = "DISABLED";

    List<ClientCredentialSummary> findAll(ClientCredentialQuery query);

    ClientCredentialSecretResult create(CreateClientCredentialRequest request);

    ClientCredentialSummary enable(Long id);

    ClientCredentialSummary disable(Long id);

    ClientCredentialSecretResult rotateSecret(Long id);

    ClientCredentialEntity findByAccessKey(String accessKey);

    ClientCredentialEntity findById(Long id);

    String decryptSecret(ClientCredentialEntity entity);
}
