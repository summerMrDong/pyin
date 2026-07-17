package com.pyin.center.auth.gateway;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Service;

@Service
public class SaTokenGatewayAdminAuthService implements GatewayAdminAuthService {

    @Override
    public void checkAdminRequest() {
        try {
            StpUtil.checkLogin();
        } catch (NotLoginException exception) {
            throw new GatewayAuthenticationException("未登录或登录已过期", exception);
        }
    }
}
