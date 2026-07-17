package com.pyin.center.auth.client;

public interface ClientRequestAuditService {

    String STATUS_SUCCESS = "SUCCESS";
    String STATUS_FAILED = "FAILED";

    String TYPE_AUTH_TOKEN = "AUTH_TOKEN";
    String TYPE_AUTH_REFRESH = "AUTH_REFRESH";

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
}
