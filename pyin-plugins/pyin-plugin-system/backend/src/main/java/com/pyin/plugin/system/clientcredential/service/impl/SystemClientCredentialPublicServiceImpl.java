package com.pyin.plugin.system.clientcredential.service.impl;

import com.pyin.plugin.system.api.model.SystemClientCredentialIdentity;
import com.pyin.plugin.system.api.service.SystemClientCredentialPublicService;
import com.pyin.plugin.system.clientcredential.entity.ClientCredentialEntity;
import com.pyin.plugin.system.clientcredential.service.ClientCredentialService;
import org.springframework.stereotype.Service;

@Service
public class SystemClientCredentialPublicServiceImpl implements SystemClientCredentialPublicService {

    private final ClientCredentialService clientCredentialService;

    public SystemClientCredentialPublicServiceImpl(ClientCredentialService clientCredentialService) {
        this.clientCredentialService = clientCredentialService;
    }

    @Override
    public SystemClientCredentialIdentity findByAccessKey(String accessKey) {
        return toIdentity(clientCredentialService.findByAccessKey(accessKey));
    }

    @Override
    public SystemClientCredentialIdentity findById(Long id) {
        return toIdentity(clientCredentialService.findById(id));
    }

    @Override
    public String decryptSecret(SystemClientCredentialIdentity credential) {
        if (credential == null) {
            return null;
        }
        ClientCredentialEntity entity = clientCredentialService.findById(credential.id());
        return entity == null ? null : clientCredentialService.decryptSecret(entity);
    }

    private SystemClientCredentialIdentity toIdentity(ClientCredentialEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SystemClientCredentialIdentity(
                entity.getId(),
                entity.getAccessKey(),
                entity.getAccessSecretHash(),
                ClientCredentialService.STATUS_ENABLED.equals(entity.getStatus())
        );
    }
}
