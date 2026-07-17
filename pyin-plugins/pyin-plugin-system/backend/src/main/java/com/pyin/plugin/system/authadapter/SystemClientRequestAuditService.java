package com.pyin.plugin.system.authadapter;

import com.pyin.center.auth.client.ClientRequestAuditService;
import com.pyin.plugin.system.clientcredential.ClientRequestLogService;
import org.springframework.stereotype.Component;

@Component
public class SystemClientRequestAuditService implements ClientRequestAuditService {

    private final ClientRequestLogService clientRequestLogService;

    public SystemClientRequestAuditService(ClientRequestLogService clientRequestLogService) {
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
