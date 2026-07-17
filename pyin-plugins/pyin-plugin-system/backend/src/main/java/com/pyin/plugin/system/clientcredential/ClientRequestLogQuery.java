package com.pyin.plugin.system.clientcredential;

public record ClientRequestLogQuery(
        String requestStatus,
        String requestType,
        String keyword
) {
}
