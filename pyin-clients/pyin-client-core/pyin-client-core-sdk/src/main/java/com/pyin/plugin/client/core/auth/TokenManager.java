package com.pyin.plugin.client.core.auth;

public class TokenManager {

    private volatile String tokenValue = "";

    public String currentToken() {
        return tokenValue;
    }

    public void updateToken(String tokenValue) {
        this.tokenValue = tokenValue == null ? "" : tokenValue;
    }
}
