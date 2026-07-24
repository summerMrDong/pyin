package com.pyin.plugin.system.clientcredential.service.impl;

import com.pyin.plugin.system.api.service.SystemClientRequestAuditPublicService;
import com.pyin.plugin.system.clientcredential.service.ClientRequestLogService;
import org.springframework.stereotype.Service;

@Service
public class SystemClientRequestAuditPublicServiceImpl implements SystemClientRequestAuditPublicService {

    private final ClientRequestLogService clientRequestLogService;

    public SystemClientRequestAuditPublicServiceImpl(ClientRequestLogService clientRequestLogService) {
        this.clientRequestLogService = clientRequestLogService;
    }

    @Override
    public void log(
            Long credentialId,
            String accessKey,
            String requestType,
            String requestUri,
            String httpMethod,
            String clientIp,
            String requestStatus,
            String failureCode,
            String failureMessage
    ) {
        clientRequestLogService.log(
                credentialId,
                accessKey,
                requestType,
                requestUri,
                httpMethod,
                clientIp,
                requestStatus,
                failureCode,
                failureMessage
        );
    }
}
