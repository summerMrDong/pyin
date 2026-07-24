package com.pyin.plugin.system.clientcredential.model;

public record ClientRequestLogQuery(
        String requestStatus,
        String requestType,
        String keyword
) {
}
