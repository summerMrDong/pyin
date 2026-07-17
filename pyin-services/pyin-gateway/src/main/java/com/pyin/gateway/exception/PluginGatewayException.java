package com.pyin.gateway.exception;

public class PluginGatewayException extends RuntimeException {

    private final int statusCode;
    private final String code;

    public PluginGatewayException(int statusCode, String code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }
}
