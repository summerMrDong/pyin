package com.pyin.center.auth.web;

import com.pyin.center.auth.authentication.AdminRequestAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminRequestAuthenticator adminRequestAuthenticator;

    public AdminAuthInterceptor(AdminRequestAuthenticator adminRequestAuthenticator) {
        this.adminRequestAuthenticator = adminRequestAuthenticator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        adminRequestAuthenticator.checkLogin();
        return true;
    }
}
