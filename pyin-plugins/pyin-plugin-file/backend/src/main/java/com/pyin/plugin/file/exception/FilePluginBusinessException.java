package com.pyin.plugin.file.exception;

import com.pyin.plugin.common.exception.BusinessException;

public class FilePluginBusinessException extends BusinessException {

    public FilePluginBusinessException(String code, String message) {
        super(code, message);
    }
}
