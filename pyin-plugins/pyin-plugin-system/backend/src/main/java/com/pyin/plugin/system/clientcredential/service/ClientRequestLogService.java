package com.pyin.plugin.system.clientcredential.service;


import com.pyin.plugin.system.clientcredential.model.ClientRequestLogQuery;
import com.pyin.plugin.system.clientcredential.model.ClientRequestLogView;
import java.util.List;

public interface ClientRequestLogService {

    String STATUS_SUCCESS = "SUCCESS";
    String STATUS_FAILED = "FAILED";

    String TYPE_AUTH_TOKEN = "AUTH_TOKEN";
    String TYPE_PLUGIN_CLIENT_API = "PLUGIN_CLIENT_API";

    void log(
            Long credentialId,
            String accessKey,
            String requestType,
            String requestUri,
            String httpMethod,
            String clientIp,
            String requestStatus,
            String failureCode,
            String failureMessage
    );

    List<ClientRequestLogView> findByCredentialId(Long credentialId, ClientRequestLogQuery query);
}
