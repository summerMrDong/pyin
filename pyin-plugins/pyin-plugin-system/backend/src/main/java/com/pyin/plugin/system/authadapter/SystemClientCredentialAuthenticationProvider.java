package com.pyin.plugin.system.authadapter;

import com.pyin.center.auth.client.ClientCredentialAuthenticationProvider;
import com.pyin.center.auth.client.ClientCredentialIdentity;
import com.pyin.plugin.system.clientcredential.ClientCredentialEntity;
import com.pyin.plugin.system.clientcredential.ClientCredentialService;
import org.springframework.stereotype.Component;

@Component
public class SystemClientCredentialAuthenticationProvider implements ClientCredentialAuthenticationProvider {

    private final ClientCredentialService clientCredentialService;

    public SystemClientCredentialAuthenticationProvider(ClientCredentialService clientCredentialService) {
        this.clientCredentialService = clientCredentialService;
    }

    @Override
    public ClientCredentialIdentity findByAccessKey(String accessKey) {
        return toIdentity(clientCredentialService.findByAccessKey(accessKey));
    }

    @Override
    public ClientCredentialIdentity findById(Long id) {
        return toIdentity(clientCredentialService.findById(id));
    }

    @Override
    public String decryptSecret(ClientCredentialIdentity credential) {
        if (credential == null) {
            return null;
        }
        ClientCredentialEntity entity = clientCredentialService.findById(credential.id());
        return entity == null ? null : clientCredentialService.decryptSecret(entity);
    }

    private ClientCredentialIdentity toIdentity(ClientCredentialEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ClientCredentialIdentity(
                entity.getId(),
                entity.getAccessKey(),
                entity.getAccessSecretHash(),
                ClientCredentialService.STATUS_ENABLED.equals(entity.getStatus())
        );
    }
}
