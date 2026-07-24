package com.pyin.center.auth.web;

import com.pyin.center.auth.authentication.ClientRequestAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class ClientAuthInterceptor implements HandlerInterceptor {

    private final ClientRequestAuthenticator clientRequestAuthenticator;

    public ClientAuthInterceptor(ClientRequestAuthenticator clientRequestAuthenticator) {
        this.clientRequestAuthenticator = clientRequestAuthenticator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        clientRequestAuthenticator.authenticate();
        return true;
    }
}
