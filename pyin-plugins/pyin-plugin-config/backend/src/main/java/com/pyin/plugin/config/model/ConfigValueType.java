package com.pyin.plugin.config.model;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import java.util.Locale;

public enum ConfigValueType {
    STRING,
    INTEGER,
    BOOLEAN,
    JSON;

    public static ConfigValueType from(String value) {
        if (value == null || value.isBlank()) {
            return STRING;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "不支持的配置值类型。");
        }
    }
}
