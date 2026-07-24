package com.pyin.center.auth.client.service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientCredentialNonceService {

    private static final long NONCE_TTL_MILLIS = 5 * 60 * 1000L;

    private final Map<String, Long> nonceStore = new ConcurrentHashMap<>();

    public boolean register(String accessKey, String nonce, long timestamp) {
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(nonce)) {
            return false;
        }
        evictExpired();
        String key = accessKey.trim() + ":" + nonce.trim();
        return nonceStore.putIfAbsent(key, timestamp) == null;
    }

    private void evictExpired() {
        long threshold = Instant.now().toEpochMilli() - NONCE_TTL_MILLIS;
        Iterator<Map.Entry<String, Long>> iterator = nonceStore.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() == null || entry.getValue() < threshold) {
                iterator.remove();
            }
        }
    }
}
