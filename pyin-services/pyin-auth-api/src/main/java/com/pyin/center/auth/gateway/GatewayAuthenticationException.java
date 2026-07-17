package com.pyin.center.auth.gateway;

public class GatewayAuthenticationException extends RuntimeException {

    public GatewayAuthenticationException(String message) {
        super(message);
    }

    public GatewayAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
