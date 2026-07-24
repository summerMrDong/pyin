package com.pyin.plugin.config.model;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import java.util.Locale;

public enum ConfigDirectoryMode {
    KEY_PROJECTION,
    DIRECTORY_API;

    public static ConfigDirectoryMode from(String value) {
        if (value == null || value.isBlank()) {
            return KEY_PROJECTION;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "不支持的目录模式。");
        }
    }
}
