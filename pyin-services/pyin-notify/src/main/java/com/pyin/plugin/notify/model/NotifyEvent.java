package com.pyin.plugin.notify.model;

import java.util.Map;

public record NotifyEvent(
        String eventId,
        String featureCode,
        String eventType,
        String namespace,
        String env,
        Long version,
        Long timestamp,
        Map<String, Object> payload
) {
}
