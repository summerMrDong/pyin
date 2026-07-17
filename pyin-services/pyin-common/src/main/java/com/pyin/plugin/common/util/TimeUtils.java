package com.pyin.plugin.common.util;

import java.time.Instant;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static long currentEpochMillis() {
        return Instant.now().toEpochMilli();
    }
}
